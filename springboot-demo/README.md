# springboot-demo

一个**商城主页**示例（商品列表 / 商品详情 / 登录注册 / 购物车），数据存在 MySQL 里；
同时保留了**用于学习编写测试用例**的四层测试范本（77 个用例）。

> 📖 **测试教学报告：[`docs/测试指南.md`](docs/测试指南.md)**
> 包含从零写测试的完整思考过程、断言与 Mockito 速查表、覆盖率报告解读、
> 真实缺陷的复盘、9 个常见陷阱和 9 道练习。

页面能做什么：

| 功能 | 说明 |
|---|---|
| 商品列表 | 分类筛选 + 关键词搜索，数据来自 `t_product` 表 |
| **打开商品** | 点任意卡片弹出详情（大图、评分、销量、折扣、卖点、数量选择、加入购物车 / 立即购买） |
| **登录 / 注册** | 真实后端登录：`t_user` 表 + 加盐 SHA-256 + HttpSession 会话；注册成功自动登录 |
| **图形验证码** | 登录必须带验证码：服务端随机生成 + 画成 PNG，存 **Redis** 并 5 分钟自动过期，**一次性**用完即废 |
| **购物车** | 登录后存数据库 `t_cart_item`（换设备还在）；没登录时暂存浏览器 localStorage，**登录瞬间自动合并**上去 |
| **个人资料** | 点头像进「个人资料」：改昵称/邮箱/手机号/签名、**上传头像**、改密码 |
| 结算 | 演示下单：校验非空 → 算钱 → 清空购物车 |

## 技术栈

| 项 | 版本 |
|---|---|
| Java | 8（本机唯一可用的 JDK） |
| Spring Boot | 2.7.18（支持 Java 8 的最后一个版本） |
| 构建工具 | Maven 3.9.12 |
| 内置容器 | Tomcat **9.0.83**（Spring Boot 2.7.18 官方锁定；**不能用 Tomcat 10+**，那个换成了 jakarta 包名） |
| 数据库 | MySQL 5.7 / 8.0（本机实测 5.7；脚本对两者都兼容） |
| 数据访问 | `spring-boot-starter-jdbc`（JdbcTemplate + HikariCP 连接池，不用 ORM） |
| 缓存 | **Redis**（`spring-boot-starter-data-redis` + Lettuce），只用来存登录验证码 |
| 会话 | HttpSession（`MALLSESSION` Cookie），没引 Spring Security |
| 前端 | 单个 `static/index.html`，原生 JS + fetch，零构建步骤 |
| 测试 | JUnit 5 + AssertJ + Mockito + MockMvc |
| 覆盖率 | JaCoCo 0.8.8 |

## 运行

### 第 1 步：导入数据库（只需一次）

**最省事**：双击 `sql\import.cmd`，或在 PowerShell 里执行：

```powershell
cd D:\Git-space\springboot-demo
cmd /c "sql\import.cmd"
```

想手动敲命令也行，但**注意 PowerShell 不支持 `<` 重定向**
（会报 `The '<' operator is reserved for future use.`），所以要把整条命令交给 cmd 执行：

```powershell
cmd /c '"D:\MySQL Server 5.7\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p < sql\init.sql'
```

或者干脆切到「命令提示符」(cmd.exe) 里执行：

```bat
"D:\MySQL Server 5.7\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p < sql\init.sql
```

> ⚠️ `--default-character-set=utf8mb4` 不能省，否则商品的 emoji 会变成 `?`。
>
> 脚本会建库 `mall_db`、建专用账号 **`mall` / `mall123`**（应用用它连库，不用 root）、
> 建三张表并灌入 18 条商品 + 2 个演示账号。
> 如果你的账号没有 `CREATE USER` 权限，把脚本第 2 段注释掉，改成用你自己的账号连库，
> 并同步修改 `application.properties`。
>
> 重复导入 = 清空重来（脚本开头有 `DROP TABLE`）。

### 第 2 步：确认 Redis 在跑（登录验证码要用）

本机已装 Redis（`D:\Redis`），注册成了 Windows 服务，名为 **`Redis`**：

```powershell
Get-Service Redis            # 看状态，应为 Running
& 'D:\Redis\redis-cli.exe' ping   # 应返回 PONG
```

如果没启动：`Start-Service Redis`（需要管理员权限），或者直接双击 `D:\Redis\redis-server.exe`。

> **Redis 没开会发生什么？** 商品、购物车、个人资料都正常，
> **只有登录会返回 503** 并提示「验证码服务暂不可用（Redis 未连接）」。
> 急着测其它功能可以临时加 `--app.captcha.enabled=false` 跳过验证码。

### 第 3 步：启动

**最省事**：双击项目根目录的 **`start.cmd`**（或用命令行 `start.cmd`）。
它会先检查 JDK / Maven / MySQL / Redis / 端口占用，然后前台启动，日志就在窗口里。

要停止：在那个窗口按 **Ctrl+C**，或者在**另一个窗口**运行 **`stop.cmd`**。

> **`stop.cmd` 只杀本项目的进程**，靠命令行里是否含 `springboot-demo` 来识别，
> **不会**动占用同一端口的其他项目。这一点很重要 —— 8080 上常有别的项目在跑。
> 如果你的机器上还有别的服务占着 8080，用 `start.cmd 8081` 换个端口启动。

手动启动也行：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_161'
& 'C:\Tools\apache-maven-3.9.12\bin\mvn.cmd' spring-boot:run
```

或者打包后运行 jar：

```powershell
& 'C:\Tools\apache-maven-3.9.12\bin\mvn.cmd' clean package
& 'C:\Program Files\Java\jdk1.8.0_161\bin\java.exe' -jar target\springboot-demo-1.0.0.jar
```

打开 http://localhost:8080/

**演示账号**：`demo` / `123456`，管理员 `admin` / `admin123`（也可以直接在页面注册新账号）

> 如果提示 `Port 8080 was already in use`，加参数换端口：
> `java -jar target\springboot-demo-1.0.0.jar --server.port=8081`
>
> 换数据库账号/密码不用改代码，设环境变量即可：
> `$env:DB_PASSWORD='你的密码'`（配置见 `application.properties` 的 `${DB_PASSWORD:mall123}`）

## 跑测试

```powershell
# 全部测试（同时生成覆盖率报告）
& 'C:\Tools\apache-maven-3.9.12\bin\mvn.cmd' test

# 只跑某一个测试类
& 'C:\Tools\apache-maven-3.9.12\bin\mvn.cmd' test -Dtest=CalculatorServiceTest

# 只跑某一个方法
& 'C:\Tools\apache-maven-3.9.12\bin\mvn.cmd' test -Dtest=CalculatorServiceTest#add_shouldReturnSum

# 跑多个类
& 'C:\Tools\apache-maven-3.9.12\bin\mvn.cmd' test -Dtest=TodoServiceTest,TodoControllerTest
```

- 覆盖率报告：`target/site/jacoco/index.html`（命令行版 `target/site/jacoco/jacoco.csv`）
- 测试报告：`target/surefire-reports/`

### 当前结果

```
Tests run: 77, Failures: 0, Errors: 0, Skipped: 0
行覆盖 25.0% (128/511)   分支覆盖 25.8% (48/186)   指令覆盖 22.2% (487/2195)
```

> ⚠️ **覆盖率从 97.2% 掉到 25%，不是代码写坏了，而是分母变了。**
> 引入 MySQL 后新增了 8 个类（`Product`、`User`、`CartItem`、`CartSummary`、
> `ProductService`、`UserService`、`CartService`、`Passwords`），本轮**故意没给它们写用例**，
> 所以它们全是 0%。原有那批类的覆盖率**一点没降**：

| 类 | 行覆盖 | 分支覆盖 |
|---|---|---|
| `CalculatorService` | 100% (15/15) | 100% (12/12) |
| `TodoService` | 100% (41/41) | 90% (27/30) |
| `TodoController` | 100% (17/17) | 90% (9/10) |
| `HelloController` | 100% (5/5) | — |
| `ApiExceptionHandler` | 85.7% (6/7) | — |
| `Todo` | 94.1% (16/17) | — |

> 这正好演示了覆盖率最容易被误用的一点：**它是一个比值，把没测的代码加进来就能"稀释"它**。
> 想把这些数字拉回去，就该给 `UserService` / `CartService` 补测试（见文末练习 6~8）。

### 数据库没启动会影响测试吗？

不影响。`DemoApplicationTests` 是 `@SpringBootTest`，会创建 `DataSource` Bean，
但 **HikariCP 是懒连接**——不调用 `getConnection()` 就不连库，
所以 MySQL 没开、`mall_db` 还没导入时，`mvn test` 依然是全绿的（已实测）。
真正需要数据库的是**页面**：导入之前打开首页会显示"商品加载失败"。

## 接口一览

| 方法 | 路径 | 说明 | 成功 | 失败 |
|---|---|---|---|---|
| GET | `/` | 商城主页 | 200 | — |
| GET | `/hello?name=x` | 动态页面 | 200 | — |
| GET | `/api/products` | 商品列表（`?category=` / `?keyword=`，可叠加） | 200 | — |
| GET | `/api/products/categories` | 分类列表（第一个固定是「全部」） | 200 | — |
| GET | `/api/products/{id}` | **商品详情** | 200 | 404 |
| GET | `/api/auth/captcha` | **取图形验证码**，返回 `{"captchaId","image":"data:image/png;base64,…","expiresIn":300}` | 200 | 503 |
| POST | `/api/auth/register` | **注册**，body `{"username","password","nickname"}`，成功即登录 | 201 | 400 |
| POST | `/api/auth/login` | **登录**，body `{"username","password","captchaId","captchaCode"}` | 200 | 400（验证码错）/ 401（账号密码错） |
| POST | `/api/auth/logout` | **退出登录** | 204 | — |
| GET | `/api/auth/me` | **当前登录用户**（页面刷新时恢复登录态） | 200 | 401 |
| GET | `/api/cart` | **查看购物车** | 200 | 401 |
| POST | `/api/cart/items` | **加入购物车**，body `{"productId","quantity"}`，已有则累加 | 200 | 400 / 401 |
| PUT | `/api/cart/items/{productId}` | **改数量**，body `{"quantity"}`，0 等于删除 | 200 | 400 / 401 |
| DELETE | `/api/cart/items/{productId}` | **删除一行** | 200 | 401 |
| DELETE | `/api/cart` | **清空购物车** | 200 | 401 |
| POST | `/api/cart/merge` | **合并游客购物车**，body `{"items":[{"productId","quantity"}]}` | 200 | 401 |
| POST | `/api/cart/checkout` | **结算**（清空并返回订单摘要） | 200 | 400 / 401 |
| GET | `/api/profile` | **查看个人资料** | 200 | 401 |
| PUT | `/api/profile` | **修改资料**，body `{"nickname","email","phone","bio"}` | 200 | 400 / 401 |
| POST | `/api/profile/avatar` | **上传头像**（multipart，字段名 `file`，≤2MB） | 200 | 400 / 401 |
| PUT | `/api/profile/password` | **修改密码**，body `{"oldPassword","newPassword"}` | 200 | 400 / 401 |
| GET | `/api/todos` | 查询全部（`?onlyUnfinished=true` 只看未完成） | 200 | — |
| GET | `/api/todos/{id}` | 查询单条 | 200 | 404 |
| POST | `/api/todos` | 新增，body `{"title":"..."}` | 201 | 400（标题为空） |
| POST | `/api/todos/{id}/toggle` | 切换完成状态 | 200 | 404 |
| DELETE | `/api/todos/{id}` | 删除 | 204 | 404 |

购物车相关的接口**都返回更新后的整车汇总**，前端拿到直接重绘，不用自己算角标：

```json
{ "items": [ { "productId": 1, "name": "无线蓝牙耳机 Pro", "price": 399.0,
               "emoji": "🎧", "quantity": 2, "subtotal": 798.0 } ],
  "quantity": 2, "total": 798.0 }
```

## 登录验证码（Redis 版）

登录必须先过图形验证码。整个流程只有三步：

```
前端                        后端                         Redis
GET /api/auth/captcha  →  随机生成 4 个字符
                          画成 PNG，转 base64        →  SET mall:captcha:<uuid> = "H8NF" EX 300
                       ←  {captchaId, image}
用户看图输入 H8NF
POST /api/auth/login   →  拿 captchaId 去 Redis 查 ────→  GET 再 DEL（一次性）
                          一致才继续校验账号密码
```

四个设计点，每个都是为了让验证码**真的有用**：

1. **字符绝不下发**。接口只返回图片的 base64，答案只在服务端和 Redis 里。
   如果返回 `{"code":"H8NF"}` 让前端自己画，抓个包就能看到答案，验证码等于没做。
2. **一次性**。校验时不管对错都立刻 `DEL`，一个码只能试一次 ——
   否则攻击者拿同一个 `captchaId` 可以无限次试密码，验证码就白加了。
   前端在登录失败后会自动换一张，用户不会卡住。
3. **用 Redis 而不是 session/内存**。它自带 TTL（300 秒），过期自动删除，
   不用写任何清理逻辑；将来多实例部署也能共用，session 就不行。
4. **先校验验证码，再查数据库**。顺序不能反 ——
   否则脚本仍然可以毫秒级地试用户名密码，验证码拦不住任何东西。

> 想临时关掉（本地调试嫌麻烦）：启动时加 `--app.captcha.enabled=false`，
> 或者在 `application.properties` 里改 `app.captcha.enabled=false`。
> 生产环境务必保持 `true`。

**Redis 挂掉会怎样？** 登录接口返回 **503** + `验证码服务暂不可用（Redis 未连接），请稍后再试`，
而不是一句看不懂的 500。本机 Redis 是 Windows 服务（名为 `Redis`），
`services.msc` 里能看到它是否在跑。

## 数据库设计

```
t_user        用户：id / username(唯一) / nickname / avatar / email / phone / bio
                    / salt / password_hash / created_at
t_product     商品：id / name / price / original_price / category / emoji / rating / sales / tag / description
t_cart_item   购物车：id / user_id / product_id / quantity / updated_at
              UNIQUE(user_id, product_id) —— 一个用户对一件商品只有一行
              外键 -> t_user（级联删除）、t_product
```

四个值得注意的设计点：

1. **密码永远不存明文**。`t_user` 里是 `salt` + `password_hash` 两列，
   `password_hash = sha256(salt + ":" + 密码)`，每个用户的盐都不同（实现见 `Passwords.java`）。
   `User` 的这两个 getter 上加了 `@JsonIgnore`，防止手滑把散列值随 JSON 发给浏览器。
2. **头像存「路径」不存图片**。`t_user.avatar` 里是 `/uploads/avatar/1_a1b2c3.png` 这样的路径，
   图片文件落在磁盘的 `uploads/` 目录（`app.upload-dir` 配置）。
   好处：查用户时不用拖着一坨二进制、图片可由容器直接发出、备份策略可以分开。
3. **购物车不存商品名和价格**，只存 `product_id` + `quantity`；
   查的时候 `JOIN t_product` 一次性带出商品信息（1 条 SQL，避免 N+1 查询）。
4. **分类不写死在代码里**，`categories()` 用 `SELECT DISTINCT category` 查，
   往库里加一条新分类的商品，页面分类条会自动多一项。

### 数据库升级（已有数据的库怎么加字段）

`init.sql` 开头有 `DROP TABLE`，重复导入会清空数据。如果你**已经导入过、库里有数据**，
用增量脚本，它只加字段、不动数据，而且可以重复执行：

```powershell
cmd /c "sql\migrate_v2_profile.sql"
```

（这个脚本用存储过程查 `information_schema` 判断字段是否已存在，
因为 MySQL 5.7 没有 `ADD COLUMN IF NOT EXISTS`。字段齐全时会直接跳过。）

## 四层测试范本（重点）

测试代码在 `src/test/java/`，按「**离 Spring 越远越好**」的原则分层：

| 测试类 | 用例 | 层次 | 耗时 |
|---|---|---|---|
| `DemoApplicationTests` | 1 | 冒烟 | ~2.4 s |
| `CalculatorServiceTest` | 29 | 单元 | ~0.08 s |
| `TodoServiceTest` | 16 | 单元 | ~0.01 s |
| `TodoServiceEdgeCaseTest` | 9 | 单元 | ~0.8 s |
| `HelloControllerTest` | 4 | Web | ~0.3 s |
| `TodoControllerTest` | 11 | Web | ~0.6 s |
| `TodoApiIntegrationTest` | 7 | 集成 | ~1.7 s |

> 值得记住：**45 个单元测试的总耗时（0.09 秒）比 1 个冒烟测试（2.4 秒）还少。**

### 第一层：纯单元测试
`CalculatorServiceTest.java` —— 不启动 Spring，直接 `new` 对象。

- 为什么快：没有容器启动开销
- 演示了：`@BeforeEach`、`assertEquals`、`assertThrows`、`assertAll`、`@ParameterizedTest`、`@Nested`、边界值
- **判断标准**：如果一个类不依赖 Spring 也能 new 出来，就该这样测

### 第二层：业务逻辑测试
`TodoServiceTest.java` —— 有内部状态，需要处理「测试之间互相污染」。

- 关键点：`@BeforeEach` 里调用 `clear()`，保证每个用例从空白开始
- 演示了：Optional 断言、集合断言、综合场景串联
- **踩坑提醒**：不清理状态会出现「单独跑通过、一起跑失败」

### 第三层：Web 层测试
`TodoControllerTest.java` —— 用 MockMvc 模拟 HTTP，用 `@MockBean` 替换 Service。

- `@WebMvcTest(TodoController.class)`：只启动 Web 层，比 `@SpringBootTest` 快很多
- `@MockBean`：把 Service 换成 Mockito 假对象，用 `given(...)` 约定行为
- 断言的是「状态码」和「响应 JSON」，不是 Java 方法返回值
- **注意**：`@WebMvcTest` 不会加载 `@Service`，所以必须自己 `@MockBean`

### 第四层：集成测试
`TodoApiIntegrationTest.java` —— `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`，真实启动 Tomcat 发 HTTP 请求。

- 能发现单元测试发现不了的问题：Bean 装配、JSON 序列化、全局异常处理器是否生效、静态资源能否访问
- 注意：这里注入的是**真实**的 Service，而且它是单例，`@BeforeEach` 里必须清理数据
- `RANDOM_PORT` 会随机分配端口，避免和本机 8080 冲突

### 补充

- `DemoApplicationTests.java` —— `@SpringBootTest` 冒烟测试，验证所有 Bean 能装配成功
- `HelloControllerTest.java` —— `/hello` 接口 + **XSS 转义**的安全断言
- `TodoServiceEdgeCaseTest.java` —— 边界值（超长 / 特殊字符 / 各种空白）+ AssertJ 高级断言

## 建议的练习

完整 9 道练习见 [`docs/测试指南.md`](docs/测试指南.md#十练习)。入门的三道：

1. **给 `CalculatorService` 加 `int sum(int[] nums)`**，为它写测试（覆盖空数组、null）
2. **参数化改写**：把 `CalculatorServiceTest` 里的用例改成 `@CsvSource`
3. **给 `Todo` 加 `equals()`/`hashCode()`**，然后打开 `TodoServiceEdgeCaseTest` 里被注释的那行断言
4. **换个断言风格**：用 AssertJ 的 `assertThat(x).isEqualTo(y)` 重写加法测试
5. **验证交互**：用 `then(todoService).should(times(1)).findAll()` 断言调用次数

新增的三道（针对商城）：

6. **给 `Passwords` 写纯单元测试**：同一个密码配两个不同的盐，散列值必须不同；
   `matches()` 对错误密码返回 false
7. **给 `UserService` 写测试**：用户名重复、密码太短、用户名含空格 —— 期望都是 `IllegalArgumentException`。
   它能 `new` 出来（`new UserService(jdbcTemplate)`），所以既可以用 Mockito 假 JdbcTemplate 做纯单元测试，
   也可以接 H2 内存库做真 SQL 测试
8. **给 `CartController` 写 MockMvc 测试**：重点测「没有会话时返回 401」，
   以及「A 用户的会话读不到 B 用户的购物车」——这类越权是最值得写成测试的一类 bug

## 目录结构

```
springboot-demo/
├── pom.xml
├── README.md
├── start.cmd                                    ← 一键启动（检查依赖 + 端口，前台运行）
├── stop.cmd                                     ← 一键停止（只杀本项目进程，不误伤别的项目）
├── sql/
│   ├── init.sql                                ← 建库 + 建表 + 18 条商品 + 演示账号
│   ├── migrate_v2_profile.sql                  ← 增量升级：给用户加个人资料字段（不清数据）
│   └── import.cmd                              ← 一键导入（双击即可，绕开 PowerShell 的 < 限制）
├── uploads/                                    ← 用户上传的头像（运行时自动创建，已 gitignore）
│   └── avatar/
├── docs/
│   └── 测试指南.md                             ← 教学报告
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java            启动类
    │   │   ├── HelloController.java            /hello 动态页面
    │   │   ├── model/
    │   │   │   ├── Todo.java                   待办实体
    │   │   │   ├── Product.java                商品实体（含 discountPercent 计算属性）
    │   │   │   ├── User.java                   用户实体（salt / passwordHash 不序列化）
    │   │   │   ├── CartItem.java               购物车一行（含 subtotal 计算属性）
    │   │   │   └── CartSummary.java            购物车汇总（items + 件数 + 总价）
    │   │   ├── service/
    │   │   │   ├── CalculatorService.java      纯逻辑（最好测）
    │   │   │   ├── TodoService.java            内存 CRUD + 业务规则
    │   │   │   ├── ProductService.java         商品查询（JdbcTemplate）
    │   │   │   ├── UserService.java            注册 / 登录校验 / 个人资料 / 改密码
    │   │   │   ├── CaptchaService.java         图形验证码：随机生成 + AWT 画 PNG + Redis 存取
    │   │   │   ├── CartService.java            购物车增删改 + 游客车合并
    │   │   │   ├── FileStorageService.java     头像落盘 + 四道上传防线
    │   │   │   ├── Passwords.java              加盐 SHA-256
    │   │   │   └── Texts.java                  空白判断（全角空格那个坑）
    │   │   └── web/
    │   │       ├── TodoController.java         REST 接口
    │   │       ├── ProductController.java      商品接口（只读）
    │   │       ├── AuthController.java         注册 / 登录 / 退出 / 当前用户
    │   │       ├── CartController.java         购物车接口（全部要求登录）
    │   │       ├── ProfileController.java      个人资料 / 头像上传 / 改密码
    │   │       ├── SessionUser.java            会话里的「当前用户」
    │   │       ├── UnauthorizedException.java  未登录 -> 401
    │   │       └── ApiExceptionHandler.java    全局异常 -> 400 / 401
    │   ├── config/
    │   │   └── WebConfig.java                  把 uploads/ 挂成 /uploads/** 静态资源
    │   └── resources/
    │       ├── application.properties          端口 / 数据源 / 连接池 / 上传配置
    │       └── static/index.html               商城主页（列表 + 详情 + 登录 + 购物车 + 个人资料）
    └── test/java/com/example/demo/
        ├── DemoApplicationTests.java              冒烟
        ├── TodoApiIntegrationTest.java            第四层：真实 HTTP
        ├── service/CalculatorServiceTest.java     第一层：纯单元
        ├── service/TodoServiceTest.java           第二层：业务逻辑
        ├── service/TodoServiceEdgeCaseTest.java   边界值 + AssertJ
        ├── web/TodoControllerTest.java            第三层：Web 层
        └── web/HelloControllerTest.java           /hello + XSS 安全断言
```

## 四个真实的教训

这个项目的测试红过四次，四次原因完全不同 —— 这正好是测试工作中最需要区分的情况。

### 第一次：测试写错了（不是代码错）

```
TodoServiceTest.toggle_shouldFlipBackAndForth:140 expected: <false> but was: <true>
```

`toggle()` 返回的是「切换**之后**」的状态，而断言按「切换**之前**」写。

**处理：改测试。**

### 第二次：代码真的有 bug

```
TodoServiceEdgeCaseTest.add_fullWidthSpace_shouldThrow <<< FAILURE!
```

`String.trim()` 只去除码点 ≤ U+0020 的字符，而中文输入法常见的**全角空格 `\u3000`** 不在其中。
结果是用户能创建出一条「看起来是空的」待办。

**处理：改代码** —— 用 `Character.isWhitespace()` + `Character.isSpaceChar()` 重新实现空白判断。
修复后用真实 HTTP 复验：

```
POST {"title":"\u3000"}                      → 400 待办内容不能为空      ✓
POST {"title":"\u3000\u3000测试全角\u3000\u3000"} → 201 title="测试全角"   ✓
POST {"title":"   半角空格测试   "}           → 201 回归通过            ✓
```

### 第三次：测试过了，但过的是「过期的构建产物」

改完首页后跑 `mvn test`，冒出来一个失败：

```
TodoApiIntegrationTest.indexPage_isServed:162
Expecting actual: "<!DOCTYPE html> ... 小确幸商城 ..." to contain: "Spring Boot 示例首页"
```

看着像是我把首页改坏了，其实是**这个断言早就失效了，只是一直没被发现**。

原因：页面静态资源是从 **classpath**（也就是 `target/classes/static/index.html`）读的，
而首页从「待办版」换成「商城版」之后，没人重新构建过。于是测试一直在拿**旧构建产物**做断言，
当然一直是绿的。这次因为改后端要重新编译，`resources` 插件把新首页拷了过去，它才暴露出来。

**怎么确认的**：把上一次打好的 jar 解开看里面那份首页，发现还是待办版：

```powershell
& "$env:JAVA_HOME\bin\jar.exe" xf target\springboot-demo-1.0.0.jar BOOT-INF/classes/static/index.html
# 这份 html 里有 todo-list、没有 小确幸商城 —— 证明上次打包发生在换首页之前
```

**处理：改测试** —— 断言改成当前页面真正该有的东西（`小确幸商城` + 商品列表容器 `product-grid`）。

> **教训**：`mvn test` 测的是**构建产物**，不是源码文件本身。
> 静态资源、`application.properties` 这类不参与编译的东西，
> 改了源码但没触发 `resources` 重新拷贝时，测试验证的可能还是旧内容。
> 遇到「本地明明改了却不生效」，先 `mvn clean`，或者直接看一眼 `target/classes` 里的那一份。

> **测试失败时的第一反应**：先确认到底是「代码有 bug」「我的期望写错了」还是「测的根本不是新代码」。
> 判断依据是回到需求问一句——「用户期望的行为到底是什么？」
>
> 完整复盘见 [`docs/测试指南.md` 第八节](docs/测试指南.md#八案例复盘测试抓出的真实缺陷)。

### 第四次：代码里的「假交集」—— 搜索永远搜不到东西（最隐蔽的一次）

上线后反馈：**搜索框输什么都提示「没有找到匹配的商品」**，而点分类筛选完全正常。

出问题的代码在 `ProductController.list()`：

```java
List<Product> byCat = productService.findByCategory(category);   // 第一条 SQL
if (!isBlank(keyword)) {
    List<Product> byKw = productService.search(keyword);         // 第二条 SQL
    byCat.retainAll(byKw);                                       // ← 在这里出事
}
```

`List.retainAll()` 求交集靠的是元素的 `equals()`，而 `Product` **没有重写 equals()**，
比的实际是对象地址。两条 SQL 各自 `new` 出一批 Product 实例，地址全不一样，
交集必然是空集 —— 于是只要填了关键词，结果就被清空。

**为什么以前没暴露**：这段代码是「内存版」时代写的。那时 `findByCategory()` 和
`search()` 返回的是**同一批对象**（只是列表被复制了一份），地址相同，`retainAll`
恰好能工作。改成查数据库之后，每次查询都产出新对象，这个隐含假设才被打破 ——
**同一个 bug，换了存储实现才现形。**

**怎么发现**：直接按接口打一遍，一眼就看出来：

```
?category=数码   -> 5 条    ✓
?keyword=耳机    -> 0 条    ✗  可库里明明有「无线蓝牙耳机 Pro」
```

**处理：改代码** —— 不再手工求交集，把两个条件合并进一条 SQL 的 `WHERE`
（新方法 `ProductService.find(category, keyword)`）：

```sql
SELECT ... FROM t_product
WHERE category = ? AND (name LIKE ? OR description LIKE ?)
ORDER BY id
```

修完复验（最后三行是「通配符必须转义」的证据）：

```
?keyword=耳机               -> 1 条  无线蓝牙耳机 Pro
?keyword=PRO                -> 1 条  无线蓝牙耳机 Pro   （大小写不敏感）
?keyword=降噪               -> 1 条  无线蓝牙耳机 Pro   （命中的是卖点，不是名称）
?category=数码&keyword=耳机  -> 1 条
?category=数码&keyword=咖啡  -> 0 条   （咖啡豆属于食品，交集为空本来就是对的）
?keyword=%                  -> 0 条   （未转义的话会匹配全部 18 条）
?keyword=_                  -> 0 条   （未转义的话匹配任意单字，同样是全部）
```

> **教训一**：`equals()` 表达的是「这两个对象是不是同一个值」，不是「这两行是不是同一条记录」。
> 对象一旦来自两次独立查询，答案就变了。能用主键比就别用 equals，
> 能让数据库一次算完的判断，就别搬到内存里自己拼。
>
> **教训二**：这类 bug 单元测试很难发现 —— 它只在「真实数据 + 两次独立查询」的组合下才出现。
> 前面的 77 个用例全是绿的，因为 `Product` 根本没有测试。
> 这也是为什么**要测新代码**：不是覆盖率数字好看，而是有些错只有测了才会露头。

### 覆盖率报告还告诉我们什么

除了上面说的「分母能被稀释」，剩下几处未覆盖，逐一判断后**都决定不处理**：

| 位置 | 未覆盖内容 | 判断 |
|---|---|---|
| `DemoApplication.java:13-14` | `main()` 方法 | 正常，`@SpringBootTest` 不会调用 `main()`，也几乎无法测 |
| `Todo.java:57` | `toString()` | 低价值，只是拼字符串没有分支 |
| `TodoService.java:140,143` | `strip()` 里的 `start < end` false 分支 | **不可达**：调用方已保证输入非空 |
| `TodoController.java:63` | `request == null` 分支 | **死代码**：`@RequestBody` 必填，Spring 在进入方法前就返回 400 |
| `ApiExceptionHandler` 的 401 分支 | 新增的 `UnauthorizedException` 处理 | **待补**：给 `CartController` 补一个「未登录访问购物车」的 MockMvc 用例即可（练习 8） |

**最后两处特别有价值**：覆盖率报告帮你发现「**代码写多了**」——
那些永远走不到的防御性分支，要么删掉，要么补测试。

> ⚠️ **100% 覆盖率不是目标。** 覆盖率是发现盲区的工具，不是考核指标。
