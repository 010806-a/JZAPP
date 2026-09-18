# GEMINI.md — MoneyBook（JZ）

本文件与 AGENTS.md 约束一致；详细内容以 `MoneyBook_项目完整开发记录.md` 为准。

给任何 AI 编码助手（Claude Code、Codex、Cursor 等）在本仓库工作前必读的规则。

**权威文档：** [`MoneyBook_项目完整开发记录.md`](./MoneyBook_项目完整开发记录.md)。本文件是浓缩版 + 硬性规则。

## 0. 项目定位

- Kotlin + XML/ViewBinding + Material 3
- Gson + JSON 本地存储；无 Room/SQLite/Compose
- 仅安全邮箱功能使用 IMAP/SMTP
- 用户不熟悉 Kotlin/Android：修改 Kotlin/XML 时提供完整文件，不给需要自行拼接的片段

## 1. 当前代码基线（2026-09-16）

本仓库已完成一次“代码核对 + 两阶段保守瘦身”。当前新增/优化内容包括：
- `ui/common/FragmentNavigation.kt`：统一 Fragment transaction 模板
- `MineFragment`：重复导航逻辑整理，保留所有既有入口
- `fragment_mine.xml`：压缩重复卡片结构，保留 binding ID
- `SettingsFragment.kt`：重复单选 Dialog 统一为 `showChoiceDialog()`
- `fragment_settings.xml`：9 个 Row 使用 `MoneyBookSettingsRow`，5 个 Card 使用 `MoneyBookSettingsCard`
- `values/styles.xml`：新增 `MoneyBookSettingsCard`
- 清理 `.acside` 编辑器缓存
- 未确认安全可删除的 drawable 均保留

## 2. 不得删除的未完成功能

以下文件目前为空/占位，但代表明确的未完成功能或历史兼容结构，**不要为了瘦身删除或重新激活**：

- `utils/ExcelExporter.kt`
- `utils/NaturalLanguageParser.kt`
- `utils/BackupManager.kt`
- `ui/category/CategoryActivity.kt`
- `ui/budget/BudgetActivity.kt`
- `ui/backup/BackupActivity.kt`
- `ui/lock/LockActivity.kt`

上述 Activity 未在 Manifest 注册；真实功能分别在对应 Fragment 中。

`AppSettings.lockType/pin/pattern` 为死字段。隐私锁真实实现是 `PrivacyLockStore` + `PrivacyLockConfig`，不要重新把旧字段接回去。

## 3. 架构硬约束

- 不引入 Room / SQLite / Compose / 服务器登录 / 网络同步
- 不删除 Gson 或 `JsonDataStore`
- 不改 `applicationId` / `namespace` / JSON 文件名
- 不删除模型字段、不改变字段语义
- 不创建第二套 Repository、核心模型或记账入口
- `Transfer` 永远不计入收入/支出
- 账户关联只用 `Account.id`
- 余额公式固定：`balance = account.balance + income − expense − transferOut + transferIn`

## 4. 当前功能断点

已完成：默认账户/分类/记账类型/记账后行为、分类、预算、统计、隐私锁 PIN/图案、安全邮箱找回、首页快捷操作、桌面快捷方式、主题/动画等。

剩余：
1. 备份恢复扩展到其余核心 JSON
2. Excel 导出
3. 自然语言记账
4. 日期格式/金额小数位全局应用
5. 在上述功能稳定后继续分模块深层瘦身与回归测试

## 5. 工作方式

一次聚焦一个模块/阶段；即使用户提出“大范围优化”，也应分阶段提交，避免同时改动数据层与多个高复杂度 UI。

每次修改后：静态检查 → 本地可用环境编译 → 用户设备测试 → 更新主文档与 CHANGELOG。没有真实 Build Output 时，不得声称 Android 编译成功。

## 6. 完成后文档同步

任何结构/功能变更后同步：
- `MoneyBook_项目完整开发记录.md`
- `MoneyBook_项目完整开发记录_2026-09-16_UI更新.md`（若涉及本次 UI/瘦身基线）
- `README.md`
- `GEMINI.md`
- `REFACTOR_20260916.md`
- `CHANGELOG.md`
