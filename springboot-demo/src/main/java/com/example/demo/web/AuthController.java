package com.example.demo.web;

import com.example.demo.model.User;
import com.example.demo.service.CaptchaService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * 登录 / 注册 / 退出 —— 商城的账号入口。
 *
 * 接口清单：
 *   GET  /api/auth/captcha   取一个图形验证码（返回 captchaId + base64 图片）
 *   POST /api/auth/register  注册（body: {"username":"x","password":"y","nickname":"z"}）
 *                            成功 201 并自动登录；校验失败 400
 *   POST /api/auth/login     登录（body: {"username":"x","password":"y","captchaId":"…","captchaCode":"…"}）
 *                            成功 200；验证码错 400；账号或密码错 401
 *   POST /api/auth/logout    退出，204
 *   GET  /api/auth/me        查询当前登录用户，已登录 200，未登录 401
 *
 * 「登录态」用什么存？
 *   HttpSession —— 登录成功后把用户编号放进会话，容器发一个 Cookie 给浏览器，
 *   之后浏览器的每个请求都自动带上它。这是最传统的做法，也最容易看懂。
 *   前端不需要自己保存 token。
 *
 * 「验证码」用什么存？
 *   Redis（见 CaptchaService）—— 因为它自带过期时间，5 分钟后自动消失，
 *   不用我们写任何清理逻辑；将来多实例部署也能共用。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;

    public AuthController(UserService userService, CaptchaService captchaService) {
        this.userService = userService;
        this.captchaService = captchaService;
    }

    /**
     * 取一个验证码。
     *
     * 返回的图片是 base64 的 PNG，前端直接放进 img 的 src。
     * 注意：字符本身不会出现在响应里 —— 只在服务端和 Redis 里，
     * 否则抓个包就能看到答案，验证码就等于没做。
     */
    @GetMapping("/captcha")
    public CaptchaService.Captcha captcha() {
        return captchaService.create();
    }

    /** 注册。校验规则在 UserService 里，失败会抛 IllegalArgumentException -> 400 */
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Credentials body, HttpSession session) {
        if (body == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }
        User user = userService.register(body.getUsername(), body.getPassword(), body.getNickname());
        // 注册成功直接登录，省掉「注册完再登录一次」的步骤
        SessionUser.login(session, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * 登录。先验验证码，再验账号密码。
     *
     * 顺序很重要：验证码必须在查数据库之前校验 ——
     * 否则脚本仍然可以毫秒级地试用户名密码，验证码就白加了。
     */
    @PostMapping("/login")
    public User login(@RequestBody Credentials body, HttpSession session) {
        if (body == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }

        if (!captchaService.verify(body.getCaptchaId(), body.getCaptchaCode())) {
            // 用 400 而不是 401：这是「你填的验证码不对」，不是「账号密码不对」，
            // 两者分开也有助于前端决定该刷新验证码还是该提示密码错误
            throw new IllegalArgumentException("验证码错误或已过期，请重新输入");
        }

        Optional<User> found = userService.login(body.getUsername(), body.getPassword());
        if (!found.isPresent()) {
            throw new UnauthorizedException("用户名或密码不正确");
        }
        SessionUser.login(session, found.get().getId());
        return found.get();
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        SessionUser.logout(session);
        return ResponseEntity.noContent().build();
    }

    /** 当前登录用户。页面加载时调用它，用来恢复登录态（刷新页面不会被登出） */
    @GetMapping("/me")
    public User me(HttpSession session) {
        long userId = SessionUser.require(session);
        return userService.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("登录状态已失效，请重新登录"));
    }

    /**
     * 登录/注册的请求体，对应 JSON：
     *   {"username":"demo","password":"123456","nickname":"体验用户",
     *    "captchaId":"a1b2…","captchaCode":"A7K9"}
     *
     * nickname 只有注册时用得上；captchaId / captchaCode 只有登录时用得上，
     * 传了多余字段也不会报错（Jackson 默认忽略不认识的字段）。
     * 用独立的小类而不是 Map，好处是字段一目了然，也能被 IDE 提示。
     */
    public static class Credentials {

        private String username;
        private String password;
        private String nickname;

        /** 验证码编号，由 GET /api/auth/captcha 下发 */
        private String captchaId;

        /** 用户看图输入的那 4 个字符 */
        private String captchaCode;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getCaptchaCode() {
            return captchaCode;
        }

        public void setCaptchaCode(String captchaCode) {
            this.captchaCode = captchaCode;
        }
    }
}
