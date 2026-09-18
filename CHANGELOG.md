# Changelog

## 2026-09-16 — 代码核对、两阶段瘦身与文档同步

### Changed
- 完成最新项目 ZIP 的代码结构重新核对，并把真实 Kotlin 代码树同步到完整开发文档。
- `ui/common/FragmentNavigation.kt` 统一 Fragment transaction 模板。
- `MineFragment` 与 `fragment_mine.xml` 做保守瘦身，减少重复导航与重复卡片结构，保留既有入口和 binding ID。
- `SettingsFragment.kt` 统一重复单选设置 Dialog。
- `fragment_settings.xml` 的 9 个设置 Row 使用 `MoneyBookSettingsRow`，5 个设置 Card 使用 `MoneyBookSettingsCard`。
- `styles.xml` 新增 `MoneyBookSettingsCard`。
- 清理 `.acside` 编辑器缓存。
- 全项目 drawable 静态扫描没有确认出可安全删除资源，因此不删除 drawable。
- `README.md`、`AGENTS.md`、`GEMINI.md`、`REFACTOR_20260916.md`、完整开发记录和 UI 更新记录全部同步到同一版本状态。

### Preserved
- `ExcelExporter.kt`、`NaturalLanguageParser.kt`、`BackupManager.kt` 空类保留。
- `CategoryActivity.kt`、`BudgetActivity.kt`、`BackupActivity.kt`、`LockActivity.kt` 空壳保留，未重新注册。
- Gson/JsonDataStore、JSON 文件名、模型字段、Transfer 统计规则、Account.id 关联规则全部保持不变。

### Verification
- XML 静态解析通过。
- Kotlin 大括号结构检查通过。
- Gradle Wrapper 已补回可执行权限。
- Android `assembleDebug` 未能完成：当前环境无法访问 `services.gradle.org` 下载 Gradle 9.0.0。不得视为编译通过。

## [Unreleased]

### 待开发
- 备份恢复扩展：`accounts/categories/budgets/transfers/settings`
- Excel 导出实装
- 自然语言记账实装
- 日期格式 / 金额小数位全局应用

## 2026-09-16 — 账单页 / 首页 UI 与筛选
- 账单页按最终参考 UI 调整。
- 本月收支卡片使用 `bg_home_money_a`。
- 月份选择仅年/月；下拉箭头统一 `ic_arrow_down`。
- 右上角保留外部“账单导入”入口。
- `全部 / 支出 / 收入` 增加明确选中状态。
- 筛选支持当前月份内的某一天 + 开始/结束时间，精确到分钟。
- 列表及首页相关查看入口统一使用 `ic_arrow_right`。

## 2026-09-14
- 完成默认分类设置端与接入。
- 修复底部导航文字裁剪。
- 资产页账户图标改 PNG。

## 2026-09-13
- 完善项目完整开发文档；确认真实项目树与默认账户设置。

## 2026-09-06
- 修复 Buildozer 项目相关问题。

## 2026-08
- 完成账户流水、转账编辑、微信 XLSX 导入、中性交易识别。
