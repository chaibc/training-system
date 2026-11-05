# deploy 目录说明

| 路径 | 用途 |
|------|------|
| `nginx/hanmo.conf` | Nginx 反向代理 + 管理端 SPA + 上传文件静态服务模板 |
| `systemd/hanmo-server.service` | 后端 systemd 服务单元 |
| `env/hanmo.env.example` | 生产环境变量模板，复制到 `/etc/hanmo/hanmo.env` |
| `scripts/backup.sh` | MySQL + 上传文件备份脚本，带 14 天保留 |
| `scripts/health-check.sh` | 服务、HTTPS、磁盘、日志健康检查 |
| `scripts/deploy-jar.sh` | 服务器端替换 jar、备份旧版本并重启 |
| `scripts/logrotate-hanmo` | 日志轮转配置 |
| `seed-uploads/demo/` | 可选的演示环境图片；生产环境请由机构后台上传真实图片 |

完整步骤见 `docs/04-部署手册.md` 和 `docs/05-服务器迁移指南.md`。

## 快速路径

1. 本机打包：
   ```bash
   cd server && mvn -q -DskipTests package
   cd ../admin-web && npm ci && npm run build
   ```
2. 上传：
   ```bash
   scp server/target/hanmo-server.jar root@服务器:/tmp/
   scp -r admin-web/dist/* root@服务器:/tmp/hanmo-admin/
   ```
3. 服务器执行部署手册中的初始化步骤。
4. 健康检查：
   ```bash
   bash /data/hanmo/scripts/health-check.sh
   ```
