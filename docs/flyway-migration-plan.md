# Flyway 迁移通道收敛方案（评估稿，P2-24）

> 状态：**方案评估，未实施**。实施前需用户确认切换窗口。
> 背景：审计报告 P2-24——当前 schema 演进有四条并行通道，各自为政且有实际漂移史。

## 1. 现状：四通道与职责

| 通道 | 位置 | 生效条件 | 现状问题 |
|---|---|---|---|
| `init.sql` | `backend/db/init.sql`（约 78KB） | 仅 docker-compose 首启空卷（`docker-entrypoint-initdb.d`） | 与实体列集漂移（曾有 users.role/status 遗留列、email NOT NULL 漂移，后者已于 2026-10-06 修复）；含大量历史迁移块 |
| `init_windows.sql` | 同目录 | 手动 Navicat 导入（Windows 友好版） | 同上，且与 init.sql 双份维护 |
| `install-schema.sql` | 同目录 | 安装向导「初始化数据库」步（classpath 打进 jar） | 文件头自认漂移风险；与 JPA 实体靠人工同步（token_version/version 列均为双通道手工同步） |
| `SchemaAlignmentRunner` | `db/SchemaAlignmentRunner.java` | 每次启动（仅 MySQL），补列/补索引/建缺失表，失败只告警 | 是存量库的事实迁移器，但无版本化、不可回溯、语义是"尽力对齐"而非迁移 |

另有 dev profile `ddl-auto: update`（Hibernate 直接建表/补列）——开发期实际第五通道，
是历史上"install 向导建管理员 500（token_version 无默认值）"一类问题的温床。

## 2. 目标形态

- **单一权威**：Flyway 持有 schema 全量真相；JPA `ddl-auto` 全 profile 收敛为 `validate`（dev 也一样）；
- **V1 基线**：以当前 `install-schema.sql`（新装语义、最新列集）整理为 `V1__baseline.sql`，
  仅含 `CREATE TABLE`/索引/种子，去掉全部历史迁移块与废弃表/视图（interactions 已删，media/media_tags 视图计划 DROP）；
- **增量迁移**：此后任何实体变更 = 新增 `V{n}__xxx.sql`，禁止再改 V1；
- **存量库兼容**：`flyway.baseline-on-migrate=true` + `baseline-version=1`——
  已运行的库（无论经 init.sql 还是安装向导建的）打上 baseline 标记后只执行 V2+ 增量；
- **SchemaAlignmentRunner 退役路径**：其"补列兜底"职责由迁移接管；保留只读的安装状态探测部分
  （InstallStatusService 独立，不受影响），对齐逻辑删除或缩成一个"报告不一致但不改库"的校验模式。

## 3. 实施步骤（建议拆三个 PR）

1. **PR-1 基线**：引入 `flyway-core` + `flyway-mysql` 依赖；生成 `V1__baseline.sql`
   （以 install-schema.sql 为底，剔除 interactions 等已删对象、对齐实体列集）；全 profile
   `ddl-auto: validate`；H2 测试配置切 `spring.flyway.enabled=true`（V1 需同时兼容 MySQL/H2 语法，
   现有 test resources 已是 `MODE=MySQL`，风险可控）。
2. **PR-2 存量切换**：`baseline-on-migrate` 打开；对三个手工 SQL 文件降级为"仅归档"
   （移入 `backend/db/legacy/` 并加废弃头）；README/CONFIG_GUIDE 部署章节改写为
   「空库 → 直接启动，Flyway 建表；升级 → 换 jar 启动自动迁移」；安装向导「初始化数据库」步
   改为触发 `Flyway.migrate()`（或复用 Spring 托管的 Flyway bean），防重装锁逻辑不变。
3. **PR-3 退役对齐器**：SchemaAlignmentRunner 缩为一致性校验（发现缺列打 ERROR 日志，
   提示检查 Flyway），删除全部 ALTER 逻辑；删除 `alter_upload_record_add_dimensions.sql`
   等遗留脚本。

## 4. 风险与注意

- **安装向导时序**：向导的"初始化建表"步在 users 表为空的全新库执行；Flyway 接管后该步
  等价于跑 V1——需保证 `install.lock`/`install_state` 的建表也进迁移（现由 Runner 建）；
- **H2 与 MySQL 方言**：V1 需避免 MySQL 专属语法（`ENGINE=InnoDB`、`BIT(1)`、
  `DATETIME(6) ON UPDATE` 等），或按 vendor 目录拆 `spring.flyway.locations`；
  这是本方案最大的隐性工作量；
- **不可回退假设**：Flyway 社区版无 down 迁移，升级前仍建议 `mysqldump`（写进 README 升级章节）；
- **多实例并发**：Flyway 自带锁，docker 多副本启动安全。

## 5. 验收清单

- [ ] 全新空库：直接启动 → Flyway V1 建全量表 → 安装向导可走通；
- [ ] 存量库（经向导建表 + Runner 对齐过）：启动 → baseline 打标 → 无损跳到最新；
- [ ] H2 测试套全绿（Flyway 驱动建表，`ddl-auto: validate` 通过）；
- [ ] `init.sql`/`init_windows.sql`/`install-schema.sql` 移出 jar 与 compose 挂载；
- [ ] README 部署章节与 CONFIG_GUIDE 同步新口径。
