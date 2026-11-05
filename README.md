# 翰墨学堂 · 书法培训管理系统

依据 `design/` 下的交互原型与用户管理逻辑评审稿开发的正式项目，包含管理后台、Java 后端、微信小程序与部署/迁移文档。

## 技术栈

| 端 | 技术 |
|---|---|
| 小程序 | 微信小程序原生（WXML / WXSS / JS） |
| 管理后台 | Vue 3 + Vite 6 + JavaScript + Ant Design Vue 4 + Pinia + Vue Router + Axios |
| 后端 | Java 17 + Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + Spring Security + JWT + springdoc-openapi |
| 数据库 | MySQL 8（生产）；H2 MySQL 模式（本地开发/自动测试） |
| 文件存储 | 本地磁盘（生产 `UPLOAD_DIR`，当前默认 `/data/hanmo/uploads`） |

## 项目结构

```
.
├── design/                 交互原型与用户管理逻辑评审稿（只读参考）
├── server/                 Spring Boot 后端
│   └── uploads/demo/       本地演示环境图片
├── admin-web/              Vue 3 管理后台
├── miniprogram/            微信小程序原生工程（29 页）
├── sql/
│   ├── schema.sql          MySQL 8 表结构（38 张表）
│   ├── seed.sql            本地演示数据（H2 / 演示环境）
│   └── seed-prod.sql       生产干净初始化（无演示学员/家长/作品）
├── docs/
│   ├── 00-开发进度与续接说明.md
│   ├── 01-表结构说明.md
│   ├── 02-接口文档.md
│   ├── 03-功能介绍与用户操作手册.md
│   ├── 04-部署手册.md
│   └── 05-服务器迁移指南.md
├── deploy/                 Nginx、systemd、env 模板、备份/发布脚本
└── README.md
```

## 业务范围

按原型 V3.0 与用户管理逻辑定稿实现：

- 机构介绍、课程体系、试听预约、报名登记（线下缴费）
- 学员档案、多子女、课时账户、上课记录、成长相册
- 班级排课、周课表、教师/教室/学员冲突校验、点名消课、请假审批、补课安排
- 作品墙、家长授权、作品上传、审核、评论审核
- 统计看板、教师/班级/出勤/续费报表、CSV 导出、续费充值
- 课程与定价、班型价格、内容大纲
- 基础数据、机构内容、环境图片、公告、师资、资质
- 用户与权限：管理者 / 教务 / 教师 / 家长
- 登录与未登录态：后台账号密码登录；小程序微信登录 + 游客浏览

明确不做：校区、活动/活动价、优惠券、招生海报、导入、消息通知、线上支付、短信验证码。

## 用户管理口径（已定稿）

- 后台账号与小程序账号分开；一个账号只能拥有一种身份。
- 角色 4 个：管理者（唯一账号）、教务、教师、家长；不做自定义角色。
- 仅管理者可修改用户与权限；管理者账号不可新增、停用、删除。
- 后台密码 BCrypt；连续输错 5 次锁定 5 分钟；不做二次验证。
- 家长微信登录自动建号；绑定子女需教务审核；一个学员最多 2 个有效监护人、1 个主联系人。
- 教师账号由后台师资团队创建；教师微信登录后按手机号自动匹配；无短信验证、无自助申请。
- 教师账号与家长账号互斥；老师兼家长需使用另一个微信账号注册家长账号。
- 游客可浏览机构介绍、课程、师资、作品、试听预约；报名、课表、课时、请假、我的需登录。

## 本地开发

### 后端（H2 文件数据库）

```bash
cd server
mvn -q -DskipTests package
java -jar target/hanmo-server.jar --spring.profiles.active=dev
```

- 首次使用先初始化数据库（建表 + 演示数据）：
  ```
  scripts\h2-db.bat            # 数据库已存在则跳过
  scripts\h2-db.bat -Reset     # 删库重建，恢复演示数据
  ```
- 数据文件：`server/data/hanmo.mv.db`，重启后端数据不丢，可用 DBeaver 直接连接
- 地址：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- Swagger：`http://localhost:8080/swagger-ui.html`
- 内置 H2 控制台：`http://localhost:8080/h2-console`
- 默认管理员：`admin / Admin@123456`
- 默认教务：`jiaowu / Jiaowu@123456`
- 建表与演示数据 SQL：`server/src/main/resources/db/schema-h2.sql`、`data-h2.sql`（由 `scripts/gen-h2.py` 从 `sql/` 生成）

### 后端（本地 MySQL，DBeaver 联调用）

与线上一致的一套本地库，独立实例（**3307** 端口），不占用/不影响机器上其他 MySQL：

```bash
scripts\mysql-hanmo-start.bat          # 启动 3307 实例
cd server
java -jar target/hanmo-server.jar --spring.profiles.active=local
scripts\mysql-hanmo-stop.bat           # 停止
```

- 连接：`127.0.0.1:3307`，库 `hanmo_calligraphy`，用户 `hanmo`，密码 `Hanmo@2026`
- 数据目录：`D:\devtools\mysql-hanmo\data`，配置 `D:\devtools\mysql-hanmo\my.ini`
- Spring 配置：`server/src/main/resources/application-local.yml`
- 重建数据：`mysql -h127.0.0.1 -P3307 -uroot -pHanmo@2026 < sql/schema.sql`，再导入 `sql/seed.sql`

### 管理后台

```bash
cd admin-web
npm install
npm run dev -- --host 127.0.0.1
```

- 地址：`http://127.0.0.1:5173`
- Vite 已代理 `/api` 与 `/uploads` 到 `http://localhost:8080`

### 小程序

1. 微信开发者工具导入 `miniprogram/`。
2. 开发阶段使用测试 AppID 或替换 `miniprogram/project.config.json` 中的 `appid`。
3. 在开发者工具中勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」。
4. `miniprogram/config.js` 的 `development` 指向 `http://127.0.0.1:8080`。
5. 登录页「开发者联调」可用家长/教师演示登录；或输入 code `seed_parent_wang` / `seed_teacher_wang`。
6. 上线前把 `ENV` 改为 `production`，替换 `production.baseUrl/apiBase` 为 HTTPS 域名。

## 生产部署

生产环境使用 MySQL 8，不要执行 `sql/seed.sql`（演示数据）。

```bash
# 1. 建库并导入表结构（root 执行）
mysql -uroot -p < sql/schema.sql

# 2. 导入生产干净数据（只含角色、基础字典、管理员、机构占位资料）
mysql -uroot -p hanmo_calligraphy < sql/seed-prod.sql

# 3. 配置环境变量并启动
# 详见 docs/04-部署手册.md
```

生产初始化后：

- 管理员：`admin / Admin@123456`，首次登录后立即修改密码。
- 在「机构内容」填写真实机构资料、上传环境图片、创建教师。
- 在「基础数据」「课程与定价」维护业务内容。
- 小程序 AppID/AppSecret 写入 `/etc/hanmo/hanmo.env`，并配置微信 request/uploadFile 合法域名。

## 测试情况

- 后端接口测试：
  - 小程序公开 + 家长 + 教师只读接口 32 项通过；
  - 请假、点名幂等、作品上传、报名取消、绑定/解绑 8 项写操作通过；
  - 管理端报表、点名、班级学员、排课冲突、续费、作品授权/上传 6 项通过；
  - 图片上传与 `/uploads/**` 静态访问通过。
- 管理端 `npm run build` 通过；Playwright 全 13 页回归通过。
- 权限边界回归 13 项通过：教务可读课程/基础字典/机构资料，但不能写课程、师资、基础数据、机构内容；不能访问用户管理；家长不能上传图片。
- 家长解绑审核流程通过：审核期间仍可查看学员，审核通过后解除绑定并返回 403；教务待审核列表同时展示新绑定与解绑申请。
- 作品授权撤回联动通过：撤回后该学员已展示作品自动下架；无有效授权时审核展示返回 400。
- 补课安排接口通过：已通过请假可安排课次，重复安排返回 400，家长端补课状态同步为已安排。
- 后台手工报名接口通过：可创建待确认报名，重复有效课程返回 400，可取消。
- 小程序已通过官方 `miniprogram-compiler`（wcc/wcsc）编译全部 WXML/WXSS，29 个页面空数据渲染全部通过；JS 语法、标签结构、页面文件完整性检查通过。**尚未完成微信开发者工具真机截图/体验版验收**，上线前需用真实 AppID 在开发者工具再编译一次。

## 当前进度

- [x] 原型 V3.0 与用户管理逻辑定稿
- [x] 数据库 38 表、演示数据、生产初始化数据、表结构文档
- [x] 后端认证、用户权限、学员、报名缴费、课程、班级排课、点名消课、请假、作品、机构内容、报表、续费、上传
- [x] 管理后台 13 个页面
- [x] 微信小程序 29 个页面（家长端 / 教师端 / 游客态）
- [x] 接口文档、功能操作手册、部署手册、服务器迁移指南
- [x] Nginx / systemd / env / 备份 / 发布脚本
- [ ] 生产服务器实际部署（需域名、SSH、MySQL 密码、微信 AppID/AppSecret）
- [ ] 小程序真机编译与微信体验版验收
- [ ] 客户验收后的内容录入与试运行

> 上线所需真实信息：域名与 DNS、服务器 SSH、MySQL 密码、微信小程序 AppID/AppSecret。项目内已提供逐步操作手册和脚本。
