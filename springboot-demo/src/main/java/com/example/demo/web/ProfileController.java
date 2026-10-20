package com.example.demo.web;

import com.example.demo.model.User;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

/**
 * 个人资料接口 —— 全部要求登录（未登录 401）。
 *
 * 接口清单：
 *   GET  /api/profile            查自己的资料
 *   PUT  /api/profile            改昵称 / 邮箱 / 手机号 / 个性签名
 *   POST /api/profile/avatar     上传头像（multipart/form-data，字段名 file）
 *   PUT  /api/profile/password   改密码（body: {"oldPassword","newPassword"}）
 *
 * 和 CartController 同一条纪律：**用户身份只能从会话里取，绝不接受前端传 userId**。
 * 否则改一下参数就能改别人的资料、换别人的头像。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ProfileController(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    /** 查询当前登录用户的资料 */
    @GetMapping
    public User me(HttpSession session) {
        return requireUser(session);
    }

    /** 更新资料。昵称必填，其余可留空 */
    @PutMapping
    public User update(@RequestBody ProfileRequest body, HttpSession session) {
        long userId = SessionUser.require(session);
        if (body == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }
        return userService.updateProfile(userId, body.getNickname(), body.getEmail(),
                body.getPhone(), body.getBio());
    }

    /**
     * 上传头像。
     *
     * 用 multipart/form-data 而不是把图片转成 base64 塞进 JSON：
     * base64 会让体积膨胀约 33%，而且大图会把请求体撑得很大，不如二进制直传。
     *
     * 上传成功后顺手把旧头像文件删掉，不然用户每换一次头像，磁盘上就多留一份。
     */
    @PostMapping("/avatar")
    public User uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        long userId = SessionUser.require(session);

        User before = userService.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("登录状态已失效，请重新登录"));

        String avatarPath = fileStorageService.saveAvatar(userId, file);
        User updated = userService.updateAvatar(userId, avatarPath);

        // 先存新的、再删旧的：万一删除失败也不影响用户使用新头像
        fileStorageService.deleteAvatar(before.getAvatar());
        return updated;
    }

    /** 改密码。旧密码必须正确 */
    @PutMapping("/password")
    public void changePassword(@RequestBody PasswordRequest body, HttpSession session) {
        long userId = SessionUser.require(session);
        if (body == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }
        userService.changePassword(userId, body.getOldPassword(), body.getNewPassword());
    }

    /** 统一取当前用户，顺带处理「会话还在但用户已被删」的边界情况 */
    private User requireUser(HttpSession session) {
        long userId = SessionUser.require(session);
        return userService.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("登录状态已失效，请重新登录"));
    }

    /** 更新资料的请求体 */
    public static class ProfileRequest {

        private String nickname;
        private String email;
        private String phone;
        private String bio;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }
    }

    /** 改密码的请求体 */
    public static class PasswordRequest {

        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
