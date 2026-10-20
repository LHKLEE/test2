package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 用户服务 —— 注册、登录、查询。
 *
 * 数据存在 MySQL 的 t_user 表，用 JdbcTemplate 直接写 SQL。
 * 为什么不用 JPA？因为这里只有一张表、几条语句，JdbcTemplate 更直观，
 * 不用去猜 ORM 在背后生成了什么。（要用 ORM 时把这个类换成 Repository 即可，Controller 不用改。）
 *
 * 密码永远不存明文：库里是 salt + password_hash 两列，算法见 {@link Passwords}。
 */
@Service
public class UserService {

    /** 登录名：3~20 位，允许字母、数字、下划线、中文 */
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_\\u4e00-\\u9fa5]{3,20}$");

    /** 密码最短长度 */
    private static final int PASSWORD_MIN_LENGTH = 6;

    /** 昵称最长长度，和 t_user.nickname 的 VARCHAR(32) 对齐 */
    private static final int NICKNAME_MAX_LENGTH = 32;

    /**
     * 邮箱格式。刻意写得宽松（不追求 RFC 完备）：
     * 邮箱校验的正解是「发一封信看用户能不能收到」，正则只要拦住明显的手误就够了。
     * 写得太严反而会把合法邮箱判成非法，这是很多系统的老毛病。
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 手机号：11 位数字，1 开头。国内号码够用 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    /** 查表用的列，抽出来避免各处写不一致 */
    private static final String COLUMNS =
            "id, username, nickname, avatar, email, phone, bio, salt, password_hash";

    private final JdbcTemplate jdbc;

    /** 把一个结果行变成 User 对象 */
    private final RowMapper<User> rowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("nickname"),
            rs.getString("avatar"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("bio"),
            rs.getString("salt"),
            rs.getString("password_hash"));

    public UserService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 注册。
     *
     * 校验不通过时抛 IllegalArgumentException，由 ApiExceptionHandler 统一转成 400，
     * 所以这里不用手写 try-catch（和 TodoService 的风格保持一致）。
     *
     * @param username 登录名
     * @param password 明文密码（只在内存里存在，落库前就被散列了）
     * @param nickname 昵称，为空则默认用登录名
     * @return 新用户（含 id）
     */
    public User register(String username, String password, String nickname) {
        String name = Texts.strip(username);
        if (!USERNAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("用户名需为 3~20 位字母、数字、下划线或中文");
        }
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("密码至少 " + PASSWORD_MIN_LENGTH + " 位");
        }
        if (password.length() > 64) {
            throw new IllegalArgumentException("密码最多 64 位");
        }
        if (findByUsername(name).isPresent()) {
            throw new IllegalArgumentException("用户名「" + name + "」已被注册");
        }

        String salt = Passwords.newSalt();
        String hash = Passwords.hash(password, salt);

        String nick = Texts.strip(nickname);
        if (nick.isEmpty()) {
            nick = name;
        }
        if (nick.length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("昵称最多 " + NICKNAME_MAX_LENGTH + " 个字");
        }

        try {
            jdbc.update("INSERT INTO t_user (username, nickname, salt, password_hash) VALUES (?, ?, ?, ?)",
                    name, nick, salt, hash);
        } catch (DuplicateKeyException e) {
            // 上面查过一次，但两个人同时注册同一个名字时仍可能撞唯一索引，这里兜住
            throw new IllegalArgumentException("用户名「" + name + "」已被注册");
        }

        return findByUsername(name)
                .orElseThrow(() -> new IllegalStateException("注册后查不到用户：" + name));
    }

    /**
     * 登录校验。
     *
     * @return 校验通过返回用户；用户名不存在或密码错误都返回空 Optional
     *         （刻意不区分这两种情况，避免帮攻击者枚举出哪些用户名真实存在）
     */
    public Optional<User> login(String username, String password) {
        if (password == null) {
            return Optional.empty();
        }
        Optional<User> found = findByUsername(Texts.strip(username));
        if (!found.isPresent()) {
            return Optional.empty();
        }
        User user = found.get();
        return Passwords.matches(password, user.getSalt(), user.getPasswordHash())
                ? Optional.of(user)
                : Optional.empty();
    }

    /** 按登录名查。登录名上有唯一索引，所以最多一条 */
    public Optional<User> findByUsername(String username) {
        if (Texts.isBlank(username)) {
            return Optional.empty();
        }
        List<User> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM t_user WHERE username = ?", rowMapper, Texts.strip(username));
        return rows.isEmpty() ? Optional.<User>empty() : Optional.of(rows.get(0));
    }

    /** 按编号查 —— 会话里存的就是编号 */
    public Optional<User> findById(long id) {
        List<User> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM t_user WHERE id = ?", rowMapper, id);
        return rows.isEmpty() ? Optional.<User>empty() : Optional.of(rows.get(0));
    }

    /** 用户总数，给页面/演示用 */
    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM t_user", Integer.class);
        return n == null ? 0 : n;
    }

    // ==================== 个人资料 ====================

    /**
     * 更新个人资料（昵称 / 邮箱 / 手机号 / 个性签名）。
     *
     * 刻意【不允许改 username】：它是登录名，也是唯一键，
     * 改它等于换了账号身份，还会让已登录用户的会话语义变模糊。
     * 头像也不在这里改，它走 {@link #updateAvatar}，因为多了一步文件校验。
     *
     * 所有字段都允许「不填」，所以空白一律存成 null，而不是空字符串 ——
     * 这样前端判断「有没有填过」时只需要看是否为 null，不用同时处理 '' 和 null 两种情况。
     */
    public User updateProfile(long userId, String nickname, String email, String phone, String bio) {
        User current = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));

        String nick = Texts.strip(nickname);
        if (nick.isEmpty()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (nick.length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("昵称最多 " + NICKNAME_MAX_LENGTH + " 个字");
        }

        String mail = Texts.strip(email);
        if (!mail.isEmpty() && !EMAIL_PATTERN.matcher(mail).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        if (mail.length() > 64) {
            throw new IllegalArgumentException("邮箱最多 64 个字符");
        }

        String tel = Texts.strip(phone);
        if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
            throw new IllegalArgumentException("手机号格式不正确（11 位数字）");
        }

        String sign = Texts.strip(bio);
        if (sign.length() > 200) {
            throw new IllegalArgumentException("个性签名最多 200 个字");
        }

        jdbc.update("UPDATE t_user SET nickname = ?, email = ?, phone = ?, bio = ? WHERE id = ?",
                nick,
                mail.isEmpty() ? null : mail,
                tel.isEmpty() ? null : tel,
                sign.isEmpty() ? null : sign,
                current.getId());

        return findById(userId)
                .orElseThrow(() -> new IllegalStateException("更新后查不到用户：" + userId));
    }

    /** 更新头像路径。文件本身由 ProfileController 负责保存 */
    public User updateAvatar(long userId, String avatarPath) {
        findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        jdbc.update("UPDATE t_user SET avatar = ? WHERE id = ?", avatarPath, userId);
        return findById(userId)
                .orElseThrow(() -> new IllegalStateException("更新后查不到用户：" + userId));
    }

    /**
     * 修改密码。
     *
     * 必须校验旧密码 —— 否则只要会话被劫持（或者用户忘了锁屏），
     * 对方就能直接把密码改掉、把账号彻底夺走。这是个人中心最容易被忽略的一条安全规则。
     *
     * 改完换一个新的盐重新散列：这样即使旧散列曾经泄露过，也拿不到新密码。
     */
    public void changePassword(long userId, String oldPassword, String newPassword) {
        User current = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));

        if (oldPassword == null || !Passwords.matches(oldPassword, current.getSalt(), current.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (newPassword == null || newPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("新密码至少 " + PASSWORD_MIN_LENGTH + " 位");
        }
        if (newPassword.length() > 64) {
            throw new IllegalArgumentException("新密码最多 64 位");
        }
        if (newPassword.equals(oldPassword)) {
            throw new IllegalArgumentException("新密码不能和当前密码相同");
        }

        String salt = Passwords.newSalt();
        jdbc.update("UPDATE t_user SET salt = ?, password_hash = ? WHERE id = ?",
                salt, Passwords.hash(newPassword, salt), userId);
    }
}
