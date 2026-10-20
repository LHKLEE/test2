package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 一个用户，对应 t_user 表。
 *
 * ⚠️ 安全要点：salt（盐）和 passwordHash（密码散列）上加了 {@code @JsonIgnore}。
 * 这两个字段只应该待在数据库里，绝不能随 JSON 返回给浏览器 ——
 * 否则等于把「撞库材料」直接送给客户端。加注解比在每个 Controller 里手动清理可靠。
 */
public class User {

    /** 用户编号，数据库自增 */
    private long id;

    /** 登录名，唯一 */
    private String username;

    /** 显示昵称 */
    private String nickname;

    /**
     * 头像的访问路径，如 /uploads/avatar/1_a1b2c3.png。
     *
     * 注意存的是「路径」不是图片本身：图片落在磁盘的 uploads 目录，
     * 数据库只记它在哪。这样查用户时不会拖着一坨二进制，备份策略也自由。
     * 为 null 表示没传过头像，前端会退回到用昵称首字生成的默认头像。
     */
    private String avatar;

    /** 邮箱，可为空 */
    private String email;

    /** 手机号，可为空 */
    private String phone;

    /** 个性签名，可为空 */
    private String bio;

    /** 该用户独立的随机盐 */
    private String salt;

    /** sha256(salt + ":" + 明文密码) 的十六进制 */
    private String passwordHash;

    /** 无参构造：JdbcTemplate 的行映射和 Jackson 都需要 */
    public User() {
    }

    public User(long id, String username, String nickname, String avatar,
                String email, String phone, String bio, String salt, String passwordHash) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
        this.phone = phone;
        this.bio = bio;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    /** 不序列化：只给服务层内部校验用 */
    @JsonIgnore
    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    /** 不序列化：同上 */
    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** 方便测试/日志里看清对象，同样不含敏感信息 */
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', nickname='" + nickname + "'}";
    }
}
