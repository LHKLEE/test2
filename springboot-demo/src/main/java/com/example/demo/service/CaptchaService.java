package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 登录验证码服务 —— 随机生成 + 存 Redis + 校验。
 *
 * 为什么把验证码放 Redis，而不是放 session 或者内存 Map？
 *   1. 天然支持过期：SET 时带上 TTL，5 分钟后 Redis 自己删，不用写清理任务
 *   2. 多实例友好：将来起了两台服务器，验证码也能共用（session 就不行）
 *   3. 只存 4 个字符 + 一个随机 id，数据量极小，正是 Redis 擅长的场景
 *
 * 关键设计：**图片由服务端生成，字符本身绝不下发**。
 *   如果接口直接返回 "A7K9" 让前端自己画，那任何人都能从网络响应里读到答案，
 *   验证码就完全失去了意义。所以这里返回的是 PNG 的 base64。
 *
 * 另一个关键点：**一次性**。校验时不管对错都立刻删除 key，
 *   否则攻击者拿同一个 captchaId 可以无限次试密码 —— 那等于没有验证码。
 */
@Service
public class CaptchaService {

    /** Redis key 前缀。加上项目名做命名空间，避免和别的项目共用 Redis 时冲突 */
    private static final String KEY_PREFIX = "mall:captcha:";

    /**
     * 可选字符集：去掉了容易看错的 0/O、1/I/L、2/Z。
     * 验证码不是密码，让人认错而反复重试是纯损失。
     */
    private static final char[] SYMBOLS = "3456789ABCDEFGHJKMNPQRSTUVWXY".toCharArray();

    /** 验证码长度 */
    private static final int LENGTH = 4;

    /** 图片尺寸 */
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();
    private final boolean enabled;
    private final long ttlSeconds;

    public CaptchaService(StringRedisTemplate redis,
                          @Value("${app.captcha.enabled:true}") boolean enabled,
                          @Value("${app.captcha.ttl-seconds:300}") long ttlSeconds) {
        this.redis = redis;
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * 生成一个验证码。
     *
     * @return captchaId（前端回传用）+ 图片（data URL，直接塞进 img src）
     */
    public Captcha create() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 只存 id -> 字符。图片不存，随用随生成，省内存
        redis.opsForValue().set(KEY_PREFIX + captchaId, code, Duration.ofSeconds(ttlSeconds));

        return new Captcha(captchaId, toDataUrl(code), ttlSeconds);
    }

    /**
     * 校验验证码。
     *
     * @return true 表示通过
     */
    public boolean verify(String captchaId, String input) {
        if (!enabled) {
            return true;        // 开关关掉时直接放行（本地调试用）
        }
        if (Texts.isBlank(captchaId) || Texts.isBlank(input)) {
            return false;
        }

        String key = KEY_PREFIX + captchaId.trim();
        String expected = redis.opsForValue().get(key);

        // 无论对错都删掉：一个验证码只能用一次，防止拿它反复试密码
        redis.delete(key);

        return expected != null && expected.equalsIgnoreCase(input.trim());
    }

    /** 开关状态，前端可以用来决定是否显示验证码输入框 */
    public boolean isEnabled() {
        return enabled;
    }

    /** 生成 LENGTH 个随机字符 */
    private String randomCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(SYMBOLS[random.nextInt(SYMBOLS.length)]);
        }
        return sb.toString();
    }

    /** PNG -> base64 data URL */
    private String toDataUrl(String code) {
        try {
            BufferedImage image = render(code);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("生成验证码图片失败", e);
        }
    }

    /**
     * 把字符画成图片。
     *
     * 除了字符本身，还加了干扰线、噪点和旋转 —— 这些不是为了"好看"，
     * 而是让 OCR 没那么容易识破。当然，做得再花也只是抬高成本，
     * 真正的防线是「一次性 + 有效期 + 失败即作废」。
     */
    private BufferedImage render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景
            g.setColor(new Color(246, 247, 251));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            for (int i = 0; i < 5; i++) {
                g.setColor(randomColor(160, 220));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                           random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            // 噪点
            for (int i = 0; i < 40; i++) {
                g.setColor(randomColor(150, 230));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }

            // 字符：每个都轻微旋转并上下抖动，防止对齐切割
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 26);
            for (int i = 0; i < code.length(); i++) {
                AffineTransform saved = g.getTransform();

                int x = 14 + i * 24;
                int y = 30 + random.nextInt(6) - 3;
                // 以字符中心为轴旋转 ±0.25 弧度
                g.rotate((random.nextDouble() - 0.5) * 0.5, x + 8, y - 10);

                g.setFont(font);
                g.setColor(randomColor(20, 120));
                g.drawString(String.valueOf(code.charAt(i)), x, y);

                g.setTransform(saved);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /** 生成一个不太刺眼的随机颜色（三个通道都落在 [min,max]） */
    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(
                min + random.nextInt(range),
                min + random.nextInt(range),
                min + random.nextInt(range));
    }

    /** 返回给前端的验证码信息 */
    public static class Captcha {

        private final String captchaId;
        private final String image;
        private final long expiresIn;

        Captcha(String captchaId, String image, long expiresIn) {
            this.captchaId = captchaId;
            this.image = image;
            this.expiresIn = expiresIn;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        /** data:image/png;base64,... 可以直接放进 <img src> */
        public String getImage() {
            return image;
        }

        /** 有效期（秒），前端可以显示倒计时 */
        public long getExpiresIn() {
            return expiresIn;
        }
    }
}
