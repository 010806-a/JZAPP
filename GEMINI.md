# GEMINI.md

本文件是提供给 **Gemini / Gemini CLI** 在本仓库中工作时使用的上下文文件。内容与 [`AGENTS.md`](./AGENTS.md) 保持同步（面向所有通用编码 Agent 的规则以那份文件为准），本文件只补充 Gemini CLI 特有的使用建议。完整架构文档见 [`JZAPP_项目完整开发文档.md`](./JZAPP_项目完整开发文档.md)。

## 项目速览

- **项目**：MoneyBook 记账 App，包名 `com.example.myno.jz`。
- **定位**：完全离线的个人记账工具，Kotlin + Android View 体系 + ViewBinding，**无 Compose、无 Room、无 Hilt/Koin**。
- **数据落地**：JSON 文件（`Gson`，存于 `filesDir`）+ `SharedPreferences`（配置/安全数据）+ `AndroidKeyStore`（邮箱授权码加密）。
- **模块速查**：
  - `data/model` 数据模型　`data/local`+`data/repository` 持久化　`data/imports` 账单导入解析　`data/email` 邮件收发
  - `utils` 工具类（日志/崩溃捕获/日期计算/自研 XLSX 导出）
  - `ui/<domain>` 各功能页面（home/bills/assets/budget/category/statistics/backup/email/privacy/settings/mine/quick/main）

## 在本仓库中做修改时，请遵循

1. **先读文档再动代码**：涉及导入解析（`data/imports/*`、`ui/bills/ImportPreviewFragment.kt`）、隐私锁（`ui/privacy/*`、`data/repository/PrivacyLockStore.kt`）、备份（`data/repository/BackupRepository.kt`）这几个模块之前，务必先查阅 `JZAPP_项目完整开发文档.md` 对应章节，这几处逻辑分支多、隐含业务规则密集，容易改错。
2. **保持现有代码风格**：中文注释、"具名参数逐行换行"的格式习惯（见 `AGENTS.md` 示例），不要用格式化工具把已有代码"优化"成紧凑单行，那会制造巨大的无意义 diff。
3. **不要引入新框架**：不要为了"更现代"而引入 Compose、Room、Retrofit、Hilt 等，除非用户在当前对话中明确提出架构升级需求。
4. **数据模型改动要看 `JsonDataStore` 的序列化影响**：`Account`/`Bill`/`Category`/`Budget`/`Transfer`/`AppSettings` 都会被 Gson 直接落地成本地 JSON 文件，新增字段必须给默认值，不能贸然重命名/删除字段。
5. **导出功能零依赖**：`utils/ExcelExporter.kt` 是手写的 OOXML/ZIP 生成器，**不要**引入 Apache POI 等库来"简化"它——这是项目刻意的架构选择（减少体积、避免 minSdk 23 上的兼容性问题）。
6. **日志走 `AppLogger`**：不要新增裸的 `Log.d`/`println`，统一调用 `com.example.myno.jz.utils.AppLogger.i/w/e/crash`。

## 已知的空占位文件（不要误认为是功能入口）

`ui/account/AccountActivity.kt`、`ui/budget/BudgetActivity.kt`、`ui/category/CategoryActivity.kt`、`ui/lock/LockActivity.kt`、`ui/backup/BackupActivity.kt`、`ui/importbill/ImportBillActivity.kt`、`ui/addbill/AddBillBottomSheet.kt`、`utils/BackupManager.kt`、`utils/NaturalLanguageParser.kt` —— 均为空类，未接入 `AndroidManifest.xml`，真正功能在对应的 `XxxFragment` 或 `BackupRepository` 里。详见 `JZAPP_项目完整开发文档.md` 第 11 节。

## 构建/验证注意事项

- 本地/沙箱环境如无网络，`./gradlew` 相关命令大概率无法完整跑通（首次同步需下载 Gradle 依赖），**不要在未确认网络可用的情况下断言构建结果**。
- 项目当前**没有任何单元测试或 Instrumented 测试**。若新增了 `utils/`、`data/imports/` 下的纯逻辑代码，建议主动在 `app/src/test/kotlin/...`（需新建目录）补充 JUnit 测试，而不是假设已有测试基线可以复用。
- 修改 XML 布局中的 `android:id` 时，请同步检查所有通过 ViewBinding（`binding.xxx`）引用它的 Kotlin 代码，ViewBinding 是编译期生成代码，id 不匹配会导致整个 `:app` 模块编译失败，而不仅仅是某个类报错。

## 提交前自检

- [ ] 是否保持了中文注释与逐行换行的代码风格？
- [ ] `data.model` 新增字段是否有默认值？
- [ ] 是否复用 `AppLogger` 记录日志？
- [ ] CSV 与 XLSX 两条导入解析路径的"中性交易"（转账类）处理是否保持一致（目前两者本就不一致，见文档第 11 节，如果你在修其中一个，请判断是否需要顺手同步另一个）？
- [ ] 是否为新增纯逻辑代码补充了单元测试？

## 参考文档

- 全量架构与类清单：[`JZAPP_项目完整开发文档.md`](./JZAPP_项目完整开发文档.md)
- 通用 Agent 规则（更详细版本）：[`AGENTS.md`](./AGENTS.md)
- 人类可读概览：[`README.md`](./README.md)
- 变更记录：[`CHANGELOG.md`](./CHANGELOG.md)
