package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件存储服务 —— 目前只用来存头像。
 *
 * 为什么把图片存到磁盘、数据库只存路径？
 *   1. 查用户时不用拖着一坨二进制
 *   2. 图片可以直接由 Web 容器发出去（甚至将来换成 Nginx/OSS），不必经过 Java
 *   3. 备份策略可以分开：数据库天天备，图片另算
 *
 * 存储位置：项目运行目录下的 uploads/avatar/，由 application.properties 的
 * app.upload-dir 配置。测试里可以把它指到临时目录，互不干扰。
 *
 * ⚠️ 上传功能是 Web 应用最危险的入口之一，这里做了四道防线：
 *   1. 大小限制（配置文件里也有 spring.servlet.multipart.max-file-size 兜底）
 *   2. 内容类型白名单：只认 image/png、image/jpeg、image/gif、image/webp
 *   3. 文件头校验（magic number）：把 .exe 改名成 .png 也骗不过去
 *   4. 文件名完全由服务端生成：用户名不可信，绝不能拿它拼路径
 *      —— 否则用户传个 ../../ 就能覆盖服务器上任意文件（路径穿越漏洞）
 */
@Service
public class FileStorageService {

    /** 允许的图片类型 -> 落盘时用的扩展名 */
    private static final List<String> ALLOWED_TYPES =
            Arrays.asList("image/png", "image/jpeg", "image/gif", "image/webp");

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    /** 头像子目录名 */
    private static final String AVATAR_DIR = "avatar";

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        // toAbsolutePath + normalize：把相对路径钉死成绝对路径，
        // 后面判断「文件是否真在 uploads 里」时才有可靠的基准
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 上传根目录，供静态资源映射使用 */
    public Path getUploadRoot() {
        return uploadRoot;
    }

    /**
     * 保存头像。
     *
     * @param userId 用户编号，只用来生成文件名前缀（便于人工排查），不参与拼路径
     * @param file   上传的文件
     * @return 可以直接给前端用的访问路径，如 /uploads/avatar/1_3f2a9c.png
     */
    public String saveAvatar(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("头像不能超过 " + (MAX_AVATAR_BYTES / 1024 / 1024) + " MB");
        }

        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("只支持 PNG / JPG / GIF / WebP 格式的图片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }

        // 光看 Content-Type 不够：这个头是浏览器说的，改一下就能伪造。
        // 校验真实文件头，才能挡住「把 exe 改名成 png」这类伪装。
        if (!looksLikeImage(bytes)) {
            throw new IllegalArgumentException("这个文件的内容不像图片，已拒绝");
        }

        String extension = extensionOf(contentType);

        // 文件名完全由服务端生成：用户编号 + 随机串 + 扩展名。
        // 用 UUID 而不是原名，是为了避免同名覆盖，也避免中文名/特殊字符带来的编码问题。
        String fileName = userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "." + extension;

        Path targetDir = uploadRoot.resolve(AVATAR_DIR).normalize();
        Path target = targetDir.resolve(fileName).normalize();

        // 双保险：确认最终路径确实落在允许的目录里。
        // 就算上面哪一步被绕过，这一步也能拦住路径穿越。
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalStateException("非法的存储路径");
        }

        try {
            Files.createDirectories(targetDir);
            Files.copy(new java.io.ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("保存头像失败：" + e.getMessage(), e);
        }

        // 返回的是 URL 路径，不是磁盘路径 —— 前端要的是能放进 <img src> 的东西
        return "/uploads/" + AVATAR_DIR + "/" + fileName;
    }

    /**
     * 删除旧头像，避免磁盘上堆垃圾。
     *
     * 只删上传目录里的文件：先算出绝对路径再确认它在 uploadRoot 之内，
     * 否则一个构造出来的路径 "/uploads/../../application.properties" 就能删掉配置文件。
     */
    public void deleteAvatar(String avatarPath) {
        if (Texts.isBlank(avatarPath)) {
            return;
        }
        String relative = avatarPath.startsWith("/uploads/")
                ? avatarPath.substring("/uploads/".length())
                : avatarPath;

        Path target = uploadRoot.resolve(relative).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;     // 不在上传目录里，一律不碰
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // 删不掉不算致命错误：新头像已经存好了，旧文件留着只是占点空间
            System.err.println("删除旧头像失败（忽略）：" + target + " -> " + e.getMessage());
        }
    }

    /** 通过「魔数」判断是不是真图片：PNG / JPEG / GIF / WebP 各自的开头几个字节 */
    private boolean looksLikeImage(byte[] b) {
        if (b == null || b.length < 12) {
            return false;
        }
        // PNG: 89 50 4E 47
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return true;
        }
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return true;
        }
        // GIF: "GIF8"
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return true;
        }
        // WebP: "RIFF" .... "WEBP"
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return true;
        }
        return false;
    }

    private String extensionOf(String contentType) {
        switch (contentType) {
            case "image/png":
                return "png";
            case "image/jpeg":
                return "jpg";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            default:
                return "png";
        }
    }
}
