# MoneyBook（记账本）

一款离线优先的 Android 个人记账应用。

- **applicationId / namespace：** `com.example.myno.jz`
- **语言：** Kotlin
- **UI：** XML + ViewBinding + Material 3（无 Jetpack Compose）
- **数据存储：** Gson + JSON 文件 + SharedPreferences
- **网络：** 仅安全邮箱功能使用 IMAP/SMTP，其余业务保持本地优先

> `MoneyBook_项目完整开发记录.md` 是当前唯一权威详细文档。本 README 与 `AGENTS.md`、`GEMINI.md`、`CHANGELOG.md`、`REFACTOR_20260916.md` 已按 2026-09-16 优化后的代码同步更新。

## 功能状态

| 模块 | 当前状态 |
|---|---|
| 记账、收入/支出、多账户、转账 | ✅ 已完成 |
| 微信/支付宝 CSV/XLSX 导入、重复检测、转账识别 | ✅ 已完成 |
| 账户流水、账单详情、编辑删除 | ✅ 已完成 |
| 分类管理 | ✅ 已完成 |
| 预算管理 | ✅ 已完成 |
| 统计页面 | ✅ 已完成 |
| 默认账户/分类/记账类型/记账后行为 | ✅ 已完成 |
| 隐私锁 PIN/图案 | ✅ 已完成 |
| 安全邮箱验证/找回 | ✅ 已完成 |
| 首页快捷操作 + 桌面快捷方式 | ✅ 已完成 |
| 主题切换/动画 | ✅ 已完成 |
| 崩溃捕获 + 本地日志 | ✅ 已完成 |
| 设置页代码/布局瘦身 | ✅ 2026-09-16 已完成 |
| 备份恢复 | ⚠️ 部分完成，仅 `records.json` |
| Excel 导出 | ❌ `ExcelExporter.kt` 空类，未实现 |
| 自然语言记账 | ❌ `NaturalLanguageParser.kt` 空类，未接入 |
| 日期格式/金额小数位全局应用 | ⚠️ 设置已保存，显示层尚未全面应用 |

## 当前优化原则

本次“瘦身”采用**保守重构**：减少重复代码和 XML 样式，不通过删除未完成功能来制造“文件变少”的假象。

- 不删除 Gson / `JsonDataStore`
- 不改 JSON 文件名和模型字段含义
- 不改 `Transfer` 收支统计规则
- 不改账户关联规则：始终使用 `Account.id`
- 不激活或删除空壳 Activity
- 不删除 Excel / 自然语言 / 备份等未完成能力
- 高复杂度 Fragment 暂不进行一次性激进拆分

## 当前代码树

```text
app/src/main/kotlin/com/example/myno/jz/
├── MoneyBookApplication.kt
├── data/
│   ├── email/        EmailConnectionTester.kt
│   ├── imports/      CsvBillParser.kt / ImportBillItem.kt / ImportDuplicateChecker.kt / XlsxBillParser.kt
│   ├── local/        DefaultDataInitializer.kt / JsonDataStore.kt
│   ├── model/        Account.kt / AppSettings.kt / BackupData.kt / Bill.kt / Budget.kt / Category.kt / EmailConfig.kt / EmailProvider.kt / EmailVerification.kt / PrivacyLockConfig.kt / Transfer.kt
│   └── repository/   BackupRepository.kt / EmailConfigStore.kt / EmailCredentialStore.kt / EmailVerificationStore.kt / FinanceRepository.kt / PrivacyLockStore.kt
├── ui/
│   ├── account/      AccountActivity.kt
│   ├── addbill/      AddBillBottomSheet.kt
│   ├── assets/       10 个账户/流水/转账相关 Kotlin 文件
│   ├── backup/       BackupActivity.kt（空壳）/ BackupFragment.kt / BackupViewModel.kt
│   ├── bills/        7 个账单/导入相关 Kotlin 文件
│   ├── budget/       BudgetActivity.kt（空壳）/ BudgetAdapter.kt / BudgetManageFragment.kt / BudgetViewModel.kt
│   ├── category/     CategoryActivity.kt（空壳）/ CategoryManageAdapter.kt / CategoryManageFragment.kt
│   ├── common/       FragmentNavigation.kt
│   ├── email/        EmailConfigFragment.kt / EmailVerificationFragment.kt
│   ├── home/         首页、快捷操作、分类环形图、最近账单
│   ├── importbill/   ImportBillActivity.kt
│   ├── lock/         LockActivity.kt（空壳）
│   ├── main/         MainActivity.kt / MainViewModel.kt
│   ├── mine/         MineFragment.kt
│   ├── privacy/      隐私锁、修改/找回密码/图案、安全邮箱
│   ├── quick/        QuickEntryActivity.kt
│   ├── settings/     SettingsFragment.kt
│   └── statistics/   StatisticsFragment.kt
└── utils/            AppLogger.kt / BackupManager.kt（空壳） / CrashHandler.kt / ExcelExporter.kt（空壳） / FinanceCalculator.kt / NaturalLanguageParser.kt（空壳）
```

Android `res` 当前核对为：`color 4`、`drawable 50`、`drawable-v24 1`、`layout 40`、`menu 1`、launcher/mipmap 12、`values 4`、`values-night 2`、`xml 3`。未发现可安全确认的未引用 drawable。

## 数据规则

1. `balance = account.balance + income − expense − transferOut + transferIn`
2. `Transfer` 永远不计入收入/支出统计
3. 账户关联一律使用 `Account.id`
4. 系统账户“经营账户”“日利加”不参与普通资产统计
5. 修改账户名称只能修改 `Account.name`，不能修改 `Account.id`

## 构建

```bash
./gradlew assembleDebug
```

当前优化环境尝试过 Gradle 构建，但因无法联网下载 Gradle 9.0.0（`services.gradle.org` DNS/网络失败），没有完成真实 Android 编译验证。请在本地 Android/Gradle 环境执行构建后再作为最终通过依据。

## 文档

- `MoneyBook_项目完整开发记录.md`：完整权威文档、代码树、功能状态、数据规则、路线图
- `AGENTS.md`：AI 编码助手硬性规则
- `GEMINI.md`：Gemini 工具等价规则
- `REFACTOR_20260916.md`：本次瘦身与验证记录
- `CHANGELOG.md`：按日期记录项目变更
