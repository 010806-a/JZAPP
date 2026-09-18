# MoneyBook 项目完整开发记录

> 最后更新：2026-09-16（本次为**全量代码核对更新 + 账单/首页 UI 重构更新**——通读了 `JZ_backup_20260916_031727.zip` 中全部 84 个 Kotlin 文件后重写，纠正了旧文档严重落后于实际代码的问题）
> **2026-09-18 补充审查（只读，未改代码）：** 通读 `JZ_backup_20260918_120622.zip` 全部 85 个 Kotlin 文件，确认文件结构与 09-16 相比没有增减；09-16 记录的 `MineFragment` 密码入口命名不一致问题已修复；其余结构性问题（`HomeFragment` 臃肿、`ImportPreviewFragment` 职责混杂、`AssetsFragment` 手写 View、`BackupRepository` 范围不全、密码/图案 Fragment 重复代码、`JsonDataStore` 重复 CRUD）原样存在，且硬编码中文文件数由 36/85 上升到 43/85。详见 `REFACTOR_20260918.md`。
> 项目名称：MoneyBook
> 项目目录：JZ
> applicationId / namespace：`com.example.myno.jz`
> 项目类型：Android 离线个人记账应用
> 开发语言：Kotlin
> UI：XML + ViewBinding + Material 3
> 数据存储：Gson + JSON（无 Room、无 SQLite）+ SharedPreferences（隐私锁）
> 网络：仅用于「安全邮箱」验证码收发（jakarta.mail），其余功能完全离线
> 当前开发工具：AndroidIDE / Trae AI

---

> ### 🚧 当前开发断点（本次核对后更新）
>
> **重要发现：旧文档（2026-09-14 版）严重落后于实际代码。** 实际代码中，分类管理、预算管理、统计页面、隐私锁（PIN/图案锁）、安全邮箱找回密码 **均已完整实现**，比旧文档记录的进度超前很多。
>
> **已完整实现且已接入导航（非占位）：**
> 默认账户/分类/记账类型/记账后行为、分类管理（新增/编辑/隐藏/删除保护/拖拽排序）、预算管理（总预算+分类预算）、统计页面（月度趋势图/分类占比饼图/支出趋势，基于 MPAndroidChart）、隐私锁（PIN 密码锁 + 图案锁，PBKDF2 加密存储于 SharedPreferences，启动时校验）、安全邮箱绑定与验证（QQ/163/126/新浪/Gmail/Outlook/自定义 IMAP+SMTP，用于忘记密码/图案时重置）、快捷记账入口（桌面长按快捷方式 → QuickEntryActivity）、首页快捷操作自定义（QuickActionStore，独立 JSON 文件）。
>
> **仍是空壳 / 未实现（不要误以为已完成）：**
> - `utils/ExcelExporter.kt` —— **空类**，"导出账单"菜单点击仍是 `showComingSoon()` 占位 Toast
> - `utils/NaturalLanguageParser.kt` —— **空类**，未接入任何输入框
> - `ui/backup/BackupRepository.kt` —— 备份/恢复**只处理 `records.json`（账单）一个文件**，账户/分类/预算/转账/设置均未纳入备份，注释里也写明"后续加入"
> - `AppSettings.lockType / pin / pattern` 三个字段——**已被 `PrivacyLockStore`（SharedPreferences + PBKDF2）取代**，目前是无人读写的死字段，仅为兼容旧数据保留
> - `CategoryActivity` / `BudgetActivity` / `BackupActivity` / `LockActivity` 四个类——**全部是内容为空的历史遗留壳类**（每个仅 5 行），AndroidManifest 中未注册，真正功能都在对应 Fragment 里（`CategoryManageFragment` / `BudgetManageFragment` / `BackupFragment` / `PrivacyLockFragment`+`PrivacyLockVerifyFragment`）
>
> **下一步建议开发顺序：** ① 备份恢复补全其余 5 个 JSON 文件 → ② Excel 导出实装 → ③ 自然语言记账实装 → ④ 日期格式/小数位全局应用到账单列表与统计页 → ⑤ 全项目回归测试。

---

## 目录

1. [项目定位](#1-项目定位)
2. [重要开发规则](#2-重要开发规则)
3. [用户开发习惯](#3-用户开发习惯)
4. [开发节奏](#4-开发节奏)
5. [真实项目目录结构](#5-真实项目目录结构)
6. [Gradle / Android 环境](#6-gradle--android-环境)
7. [数据架构](#7-数据架构)
8. [数据文件结构规范](#8-数据文件结构规范)
9. [数据模型](#9-数据模型)
10. [ID 与数据关联规则](#10-id-与数据关联规则)
11. [功能模块详解](#11-功能模块详解)
12. [核心业务规则](#12-核心业务规则)
13. [数据删除 / 安全规则](#13-数据删除--安全规则)
14. [导入数据生命周期](#14-导入数据生命周期)
15. [开发规范](#15-开发规范)
16. [禁止事项](#16-禁止事项)
17. [测试数据与验证状态](#17-测试数据与验证状态)
18. [当前功能状态一览](#18-当前功能状态一览)
19. [当前开发路线](#19-当前开发路线)
20. [AI 接手工作协议](#20-ai-接手工作协议)
21. [项目变更日志](#21-项目变更日志)
22. [功能扩展建议（未来可选，非当前路线）](#22-功能扩展建议未来可选非当前路线)

---

## 1. 项目定位

MoneyBook 是一款以离线为核心的个人记账应用，功能包括：

- 记录收入 / 支出，管理多个资金账户（微信、支付宝、银行卡、现金等），账户间转账
- 微信 / 支付宝账单导入（XLSX/CSV，含重复检测、转账识别、中性交易过滤）
- 账户流水查看、账单详情、编辑与删除
- 分类管理（增删改、隐藏、拖拽排序）
- 预算管理（总预算 + 分类预算）
- 统计分析（月度趋势、分类占比、支出趋势，图表化）
- 备份与恢复（**目前仅账单**）
- 默认账户 / 默认分类 / 默认记账类型 / 记账后行为
- 主题切换、动画开关
- **PIN 密码锁 / 图案锁**（应用启动隐私锁）
- **安全邮箱**绑定与验证码校验（用于忘记密码/图案时找回）
- 首页快捷操作自定义 + 桌面长按快捷方式快速记一笔
- 崩溃捕获与本地日志（`CrashHandler` / `AppLogger`）
- 所有账本数据默认存储在本机

**核心原则：** 简单 · 离线 · 本地 · 稳定 · 可维护 · 数据可控

---

## 2. 重要开发规则

后续开发必须尽量保持当前项目架构，**不能随便更换技术栈**。

**当前技术：** Kotlin、XML、ViewBinding、Fragment、LiveData、ViewModel、Gson、JSON、SharedPreferences、Material 3、MPAndroidChart、jakarta.mail

**除非用户明确要求，否则不要改成：** Jetpack Compose、Room、SQLite、服务器数据库、云端数据库、网络同步架构

**改动幅度必须最小化：** AI 写的代码往往只有 AI 自己能看懂、能改，在可读性、可复用性、可扩展性上明显比不上资深工程师亲自设计的代码——人写的功能在设计之初就经过仔细斟酌，后续改动时通常只需加一两个字段；而 AI 改动时容易不必要地牵连七八个文件，用户又没有能力逐个审查这些改动是否合理。因此：

- 每次修改前先想清楚"最小、局部"的改法，优先复用已有结构，不因为图省事就大范围重构或跨多个文件改动
- 如果一个看起来很小的需求（比如加一个字段）却发现需要连带修改七八个文件，应先停下来，向用户说明原因和影响范围，而不是直接执行
- 不引入用户无法审查、也未明确要求的架构调整

---

## 3. 用户开发习惯

用户不熟悉 Kotlin 和 Android 项目结构，因此修改代码时：

- 优先给**完整文件**，而不是"把这几行放进去"式的片段
- 流程：文件路径 → 完整代码 → 替换原文件 → 保存 → 编译 → 测试
- 每次只推进一个阶段，改动前先说明会影响哪些文件

---

## 4. 开发节奏

每次只推进一个阶段：**修改 → 编译 → 安装 → 测试 → 用户确认 → 下一步**

不要一次性修改大量无关功能。

---

## 5. 真实项目目录结构（2026-09-16 优化版）

以下树状图按当前优化后的工作目录重新核对生成。重点列出**全部 Kotlin 代码文件**，并单独列出 Android 资源数量；空壳/未完成功能也明确保留，避免后续 AI 误删。

### 5.1 Kotlin 代码树

```text
app/src/main/kotlin/com/example/myno/jz/
├── MoneyBookApplication.kt
├── data/
│   ├── email/
│   │   └── EmailConnectionTester.kt
│   ├── imports/
│   │   ├── CsvBillParser.kt
│   │   ├── ImportBillItem.kt
│   │   ├── ImportDuplicateChecker.kt
│   │   └── XlsxBillParser.kt
│   ├── local/
│   │   ├── DefaultDataInitializer.kt
│   │   └── JsonDataStore.kt
│   ├── model/
│   │   ├── Account.kt
│   │   ├── AppSettings.kt
│   │   ├── BackupData.kt
│   │   ├── Bill.kt
│   │   ├── Budget.kt
│   │   ├── Category.kt
│   │   ├── EmailConfig.kt
│   │   ├── EmailProvider.kt
│   │   ├── EmailVerification.kt
│   │   ├── PrivacyLockConfig.kt
│   │   └── Transfer.kt
│   └── repository/
│       ├── BackupRepository.kt
│       ├── EmailConfigStore.kt
│       ├── EmailCredentialStore.kt
│       ├── EmailVerificationStore.kt
│       ├── FinanceRepository.kt
│       └── PrivacyLockStore.kt
├── ui/
│   ├── account/
│   │   └── AccountActivity.kt
│   ├── addbill/
│   │   └── AddBillBottomSheet.kt
│   ├── assets/
│   │   ├── AccountDetailFragment.kt
│   │   ├── AccountFlowAdapter.kt
│   │   ├── AccountFlowFragment.kt
│   │   ├── AccountFlowItem.kt
│   │   ├── AccountSettingsFragment.kt
│   │   ├── AddAccountFragment.kt
│   │   ├── AssetsFragment.kt
│   │   ├── EditTransferFragment.kt
│   │   ├── TransferDetailFragment.kt
│   │   └── TransferFragment.kt
│   ├── backup/
│   │   ├── BackupActivity.kt                # 空壳，未使用
│   │   ├── BackupFragment.kt                 # 实际备份/恢复 UI
│   │   └── BackupViewModel.kt
│   ├── bills/
│   │   ├── AddBillFragment.kt
│   │   ├── BillAdapter.kt
│   │   ├── BillDetailFragment.kt
│   │   ├── BillsFragment.kt
│   │   ├── ImportBillFragment.kt
│   │   ├── ImportPreviewAdapter.kt
│   │   └── ImportPreviewFragment.kt
│   ├── budget/
│   │   ├── BudgetActivity.kt                 # 空壳，未使用
│   │   ├── BudgetAdapter.kt
│   │   ├── BudgetManageFragment.kt
│   │   └── BudgetViewModel.kt
│   ├── category/
│   │   ├── CategoryActivity.kt               # 空壳，未使用
│   │   ├── CategoryManageAdapter.kt
│   │   └── CategoryManageFragment.kt
│   ├── common/
│   │   └── FragmentNavigation.kt             # 统一 Fragment 导航模板
│   ├── email/
│   │   ├── EmailConfigFragment.kt
│   │   └── EmailVerificationFragment.kt
│   ├── home/
│   │   ├── CategoryDonutChartView.kt
│   │   ├── HomeFragment.kt
│   │   ├── QuickAction.kt
│   │   ├── QuickActionAdapter.kt
│   │   ├── QuickActionStore.kt
│   │   └── RecentBillAdapter.kt
│   ├── importbill/
│   │   └── ImportBillActivity.kt
│   ├── lock/
│   │   └── LockActivity.kt                   # 空壳，未使用
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   ├── mine/
│   │   └── MineFragment.kt
│   ├── privacy/
│   │   ├── ChangePasswordFragment.kt
│   │   ├── ChangePatternFragment.kt
│   │   ├── ChangeSecurityFragment.kt
│   │   ├── PatternLockView.kt
│   │   ├── PrivacyLockFragment.kt
│   │   ├── PrivacyLockVerifyFragment.kt
│   │   ├── ResetPasswordFragment.kt
│   │   └── ResetPatternFragment.kt
│   ├── quick/
│   │   └── QuickEntryActivity.kt
│   ├── settings/
│   │   └── SettingsFragment.kt                # 本次第二阶段瘦身
│   └── statistics/
│       └── StatisticsFragment.kt
└── utils/
    ├── AppLogger.kt
    ├── BackupManager.kt                        # 空壳，未使用
    ├── CrashHandler.kt
    ├── ExcelExporter.kt                         # 空壳，未实现
    ├── FinanceCalculator.kt
    └── NaturalLanguageParser.kt                # 空壳，未实现
```

### 5.2 Android 资源树概览

当前资源目录共核对：`color 4`、`drawable 50`、`drawable-v24 1`、`layout 40`、`menu 1`、`mipmap-anydpi-v26 2`、各密度 launcher 资源共 10、`values 4`、`values-night 2`、`xml 3`。本次静态引用扫描未确认出可安全删除的 drawable，因此没有继续删资源。

```text
app/src/main/res/
├── color/                 # 4
├── drawable/              # 50
├── drawable-v24/          # 1
├── layout/                # 40
├── menu/                  # 1
├── mipmap-anydpi-v26/     # 2
├── mipmap-hdpi/           # 2
├── mipmap-mdpi/           # 2
├── mipmap-xhdpi/          # 2
├── mipmap-xxhdpi/         # 2
├── mipmap-xxxhdpi/        # 2
├── values/                # 4
├── values-night/          # 2
└── xml/                   # 3
```

### 5.3 项目根目录文档

```text
JZ/
├── AGENTS.md
├── CHANGELOG.md
├── GEMINI.md
├── MoneyBook_项目完整开发记录.md             # 权威详细文档
├── MoneyBook_项目完整开发记录_2026-09-16_UI更新.md
├── README.md
├── REFACTOR_20260916.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    └── proguard-rules.pro
```

> 注意：`app-debug.apk` 不在当前优化 ZIP 的项目根目录中；不能把它写进实际树状图。`gradlew` 已补回可执行权限，但当前环境无网络，无法下载 Gradle 9.0.0，因此构建状态仍以“未完成本地编译验证”记录。
---

## 6. Gradle / Android 环境

| 项目 | 版本 |
|---|---|
| Kotlin | 2.1.0 |
| AGP | 8.13.0 |
| compileSdk | 36 |
| targetSdk | 34 |
| minSdk | 23 |
| JVM | 17 |

**主要依赖：**
- androidx.core:core-ktx 1.18.0
- androidx.appcompat:appcompat 1.8.0
- com.google.android.material:material 1.14.0
- androidx.constraintlayout:constraintlayout 2.2.2
- androidx.activity:activity 1.10.1
- androidx.fragment:fragment 1.8.9
- androidx.lifecycle:lifecycle 2.9.2
- androidx.recyclerview:recyclerview 1.4.0
- com.google.code.gson:gson 2.13.1
- **com.github.PhilJay:MPAndroidChart:v3.1.0**（新增，用于统计页折线图/饼图）
- **org.eclipse.angus:jakarta.mail:2.0.3**（新增，用于安全邮箱 IMAP/SMTP 收发验证码）

**权限：** `AndroidManifest.xml` 新增 `android.permission.INTERNET`（仅用于安全邮箱功能，其余功能不联网）。

**XLSX 解析未使用第三方库**（如 Apache POI），而是手写 `ZipInputStream` + `XmlPullParser` 直接解析 `xl/sharedStrings.xml` 和 sheet XML，体积更小但只覆盖导入所需的最小子集。

---

## 7. 数据架构

```
UI → ViewModel → FinanceRepository → JsonDataStore → JSON 文件（6 个，账本核心数据）
UI → QuickActionStore → quick_actions.json（首页快捷操作，独立于 FinanceRepository）
UI → PrivacyLockStore → SharedPreferences「privacy_lock」（隐私锁密码/图案哈希）
UI → EmailConfigStore / EmailCredentialStore / EmailVerificationStore → SharedPreferences（安全邮箱配置与验证码）
UI → BackupRepository → app 私有目录 backup/ 下的备份 JSON 文件（目前仅含 records）
```

无 Room、无 SQLite、无网络数据库；网络仅用于安全邮箱的 IMAP/SMTP 请求。

---

## 8. 数据文件结构规范

### 8.1 账本核心 JSON（`JsonDataStore` 管理，位于 app 私有目录）

| 文件 | 类型 | 用途 / 备注 |
|---|---|---|
| `records.json` | `List<Bill>` | 保存所有普通收入/支出账单。`Bill.id` 必须唯一；`amount` 始终保存正数；**不保存 Transfer** |
| `accounts.json` | `List<Account>` | 保存账户 |
| `categories.json` | `List<Category>` | 保存分类 |
| `budgets.json` | `List<Budget>` | 保存预算 |
| `transfers.json` | `List<Transfer>` | 保存所有账户转账 |
| `settings.json` | `AppSettings`（单个对象，不是 List） | 保存全局设置（注意：其中 `lockType/pin/pattern` 字段已废弃，见 §9.4） |

### 8.2 其他独立存储（**不在**上述 6 个文件、也**不在**备份范围内）

| 存储位置 | 内容 | 管理类 |
|---|---|---|
| `quick_actions.json`（app 私有目录，独立文件） | 首页快捷操作列表 | `QuickActionStore` |
| SharedPreferences「privacy_lock」 | 隐私锁开关/类型/密码或图案的 PBKDF2 哈希+盐值 | `PrivacyLockStore` |
| SharedPreferences（安全邮箱相关） | 邮箱地址/服务商/IMAP-SMTP 配置、验证码哈希（含过期时间/尝试次数） | `EmailConfigStore` / `EmailCredentialStore` / `EmailVerificationStore` |
| app 私有目录 `backup/` | 手动创建的备份 JSON 文件（仅含 `records`） | `BackupRepository` |
| app 私有目录 `logs/moneybook.log` | 运行日志 | `AppLogger` |

---

## 9. 数据模型

### 9.1 Bill（账单）
`data/model/Bill.kt`（结构未变）

```kotlin
enum class BillType { EXPENSE, INCOME }

data class Bill(
    val id: String,
    val type: BillType,
    val amount: Double,
    val categoryId: String,
    val accountId: String,
    val note: String = "",
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "",
    val sourceTransactionId: String = ""
)
```
规则：收入 → 账户余额 + amount；支出 → 账户余额 − amount。

### 9.2 Account（账户）
`data/model/Account.kt`（结构未变）

```kotlin
enum class AccountType { BANK_CARD, WECHAT, ALIPAY, CASH, OTHER }

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val icon: String = "wallet",
    val includeInTotalAssets: Boolean = true,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)
```

- `balance` 是**初始余额**，不是实时余额；实时余额 = 初始余额 + 收入 − 支出 − 转账转出 + 转账转入
- 账户匹配必须使用 `account.id`（UUID），显示用 `account.name`

### 9.3 Transfer（转账）
`data/model/Transfer.kt`（结构未变，转账既不是收入也不是支出）

### 9.4 AppSettings（设置）
`data/model/AppSettings.kt`

```kotlin
enum class LockType { NONE, PIN, PATTERN }              // ⚠️ 已废弃，见下方说明
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class DefaultBillType { EXPENSE, INCOME }
enum class AfterAddAction { HOME, BILL_LIST, CONTINUE }
enum class DateFormatType { YMD, YMD_SLASH, MD }
enum class DecimalPlaces { TWO, ZERO }
enum class StatisticsPeriod { NATURAL_MONTH, LAST_30_DAYS, NATURAL_YEAR }

data class AppSettings(
    val lockType: LockType = LockType.NONE,   // ⚠️ 死字段，无代码读写
    val pin: String? = null,                  // ⚠️ 死字段，无代码读写
    val pattern: String? = null,              // ⚠️ 死字段，无代码读写
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val animationEnabled: Boolean = true,
    val defaultBillType: DefaultBillType = DefaultBillType.EXPENSE,
    val afterAddAction: AfterAddAction = AfterAddAction.HOME,
    val dateFormat: DateFormatType = DateFormatType.YMD,
    val decimalPlaces: DecimalPlaces = DecimalPlaces.TWO,
    val statisticsPeriod: StatisticsPeriod = StatisticsPeriod.NATURAL_MONTH,
    val darkMode: Boolean = false,
    val autoCarryBalance: Boolean = true,
    val defaultAccountId: String? = null,
    val defaultCategoryId: String? = null
)
```

> **⚠️ 重要：** `lockType / pin / pattern` 三个字段的原始代码注释写着"后续会进一步改为更加安全的存储方式"——这个"后续"已经发生：真正的隐私锁实现是 `PrivacyLockStore`（见 §9.6），存储在独立的 SharedPreferences 中，用 PBKDF2WithHmacSHA1（120000 次迭代、256 位密钥、随机盐）加密。`AppSettings` 里这三个字段目前**没有任何代码读写它们**，只是为了兼容旧版本数据结构而保留。**不要在这三个死字段上继续开发**，隐私锁相关需求应改 `PrivacyLockStore` / `PrivacyLockConfig`。

### 9.5 Category（分类）
`data/model/Category.kt`

```kotlin
enum class CategoryType { EXPENSE, INCOME }

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val icon: String,           // emoji 文本
    val isSystem: Boolean = false,   // 系统默认分类，不可删除
    val visible: Boolean = true,     // 有历史账单的分类只隐藏不物理删除
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 9.6 Budget（预算）
`data/model/Budget.kt`

```kotlin
enum class BudgetType { TOTAL, CATEGORY }

data class Budget(
    val id: String,
    val year: Int,
    val month: Int,
    val type: BudgetType,
    val categoryId: String? = null,   // TOTAL 类型可为空
    val amount: Double,
    val enabled: Boolean = true,
    val warningPercent: Int = 80      // 预警比例
)
```

### 9.7 PrivacyLockConfig（隐私锁配置）
`data/model/PrivacyLockConfig.kt`，由 `PrivacyLockStore` 读写（SharedPreferences「privacy_lock」，非 JSON 文件）

```kotlin
enum class PrivacyLockType { NONE, PASSWORD, PATTERN }

data class PrivacyLockConfig(
    val enabled: Boolean = false,
    val type: PrivacyLockType = PrivacyLockType.NONE,
    val credentialHash: String = "",   // PBKDF2WithHmacSHA1，120000 次迭代，256 位
    val salt: String = "",
    val updatedAt: Long = 0L
)
```

### 9.8 EmailConfig / EmailProvider / EmailVerification（安全邮箱）
`data/model/EmailConfig.kt` / `EmailProvider.kt` / `EmailVerification.kt`

```kotlin
data class EmailConfig(
    val email: String = "",
    val provider: String = "CUSTOM",
    val imapHost: String = "", val imapPort: Int = 993, val imapSsl: Boolean = true,
    val smtpHost: String = "", val smtpPort: Int = 465, val smtpSsl: Boolean = true,
    val configured: Boolean = false,
    val verified: Boolean = false,
    val updatedAt: Long = 0L
)

enum class EmailProvider(val displayName: String) {
    QQ("QQ邮箱"), NETEASE_163("163邮箱"), NETEASE_126("126邮箱"),
    SINA("新浪邮箱"), GMAIL("Gmail"), OUTLOOK("Outlook / Hotmail"), CUSTOM("自定义邮箱")
}

data class EmailVerification(
    val email: String,
    val codeHash: String,
    val createdAt: Long,
    val expiresAt: Long,
    val attempts: Int = 0
)
```

用途：作为隐私锁的"找回密码/图案"通道——用户绑定安全邮箱后，忘记 PIN/图案时可通过邮箱验证码重置。

### 9.9 ImportBillItem
`data/imports/ImportBillItem.kt`：`EXPENSE / INCOME / NEUTRAL / UNKNOWN`，用于微信/支付宝/CSV/XLSX 导入预览、重复检测、转账识别。

### 9.10 FinanceRepository

`data/repository/FinanceRepository.kt` 统一管理 Bills / Accounts / Categories / Budgets / Transfers / Settings，接口未变：

```
getBills() / createBill() / updateBill() / deleteBill()
getAccounts() / addAccount() / updateAccount() / deleteAccount()
getCategories() / addCategory() / updateCategory() / deleteCategory()
getBudgets() / addBudget()
getTransfers() / addTransfer() / updateTransfer() / deleteTransfer()
getSettings() / saveSettings()
clearAllData()
```

### 9.11 MainViewModel / MainActivity

- **MainViewModel**：LiveData `bills / accounts / categories`；`loadData() / refresh() / getRepository() / updateAccount() / addAccount() / deleteAccount()`
- **MainActivity**：底部导航（首页/账单/统计/资产/我的），`onCreate()` 中新增 `checkPrivacyLock()`——若隐私锁已开启，启动时先显示 `PrivacyLockVerifyFragment` 校验，通过后才进入主界面

---

## 10. ID 与数据关联规则

| 字段 | 必须对应 |
|---|---|
| `Bill.id` | 账单唯一 ID |
| `Bill.accountId` | 必须是 `accounts.json` 中存在的 `Account.id` |
| `Bill.categoryId` | 必须是 `categories.json` 中存在的 `Category.id` |
| `Transfer.id` | 转账唯一 ID |
| `Transfer.fromAccountId` / `toAccountId` | 必须是存在的 `Account.id` |
| `AppSettings.defaultAccountId` | 必须保存 `Account.id`，不保存账户名称 |
| `AppSettings.defaultCategoryId` | 必须保存 `Category.id`，不保存分类名称 |
| `Budget.categoryId` | CATEGORY 类型必须对应存在的 `Category.id`；TOTAL 类型可为空 |

**修改账户名称时：** 只能改 `Account.name`，**绝不能改 `Account.id`**——否则会导致历史 `Bill.accountId`、`Transfer.fromAccountId`/`toAccountId` 全部失效。

---

## 11. 功能模块详解

### 11.1 主题与配色

主题：`Theme.Material3.DayNight.NoActionBar`，支持跟随系统/浅色/深色，正常工作。

| | 浅色 | 深色 |
|---|---|---|
| background | #F7F8FC | #101218 |
| surface | #FFFFFF | #191C24 |
| primary | #4F6EF7 | #8FA2FF |
| text_primary | #1F2430 | #F2F3F7 |
| text_secondary | #8A909F | #9DA3B0 |
| divider | #E9EBF0 | #2A2E38 |
| income | #22A06B | #4FD18B |
| expense | #E85D75 | #FF6B7A |

### 11.2 首页
`ui/home/HomeFragment.kt` + `RecentBillAdapter.kt` + `CategoryDonutChartView.kt`（自绘环形图）+ `QuickActionAdapter` / `QuickActionStore`。已支持账单概览、资产、预算、最近账单、可自定义的快捷操作区。日历相关入口目前直接跳转完整账单页（代码注释说明：暂无独立日历页，避免出现占位提示）。

### 11.3 账单模块
`ui/bills/`（BillsFragment / AddBillFragment / BillAdapter / BillDetailFragment）支持列表、新增、详情、编辑、删除；本次 UI 重构以 1148×2492 参考图为视觉基准，并保持 XML + ViewBinding + Material 3。

#### 11.3.1 账单页 UI 规范（2026-09-16）
- 顶部标题：`账单`；副标题：`记录每一笔 · 让生活更清晰`。
- 右上角固定功能为 **`账单导入`**，点击进入 `ImportBillFragment`；这里是外部微信/支付宝等账单文件导入入口，**不得替换成“日历账单”**。
- 月份选择仅负责选择**年 + 月**，不得在月份控件中选择“全部 / 支出 / 收入”，使用资源 `ic_arrow_down` 显示下拉箭头。
- 本月收支卡片使用资源 `bg_home_money_a` 作为背景；收入、支出金额均来自当前选择月份的真实数据。
- 筛选行保留三个互斥状态：`全部`、`支出`、`收入`，并增加右侧 `筛选` 入口；筛选图标使用 `sx.xml`，进入详情/列表的右箭头统一使用 `ic_arrow_right`。
- `全部 / 支出 / 收入` 必须有明确的选中状态：选中项使用品牌蓝背景 + 白色文字，未选中项使用白色背景 + 深色文字；默认进入账单页时 `全部` 为选中状态。用户点击“全部”后必须立即看到明显的颜色变化，不能出现“已经在全部页但按钮看不出来”的问题。
- 精确筛选支持：**指定某一天 + 当天开始时间 + 当天结束时间**；日期限制在当前选择月份内；开始/结束时间均精确到分钟（HH:mm），结束时间不得早于开始时间。
- 精确筛选与 `全部 / 支出 / 收入` 叠加生效：先限定当前月份，再按收支类型，再按指定日期和分钟级时间范围过滤。
- 筛选按钮在存在有效筛选时显示 `已筛选`，帮助用户确认当前列表不是默认全量状态；切换月份后自动清除日期/时间筛选。
- 账单列表项点击进入 `BillDetailFragment`；列表右侧进入箭头统一使用 `ic_arrow_right`。
- 账单时间展示采用 `MM-dd HH:mm`，确保日期和分钟信息同时可见。
- 首页“今日收支 → 查看全部”和“本月支出分类 → 查看详情”的右侧箭头也统一改为资源 `ic_arrow_right`，不要继续使用文本字符 `>`。


`ui/bills/`（BillsFragment / AddBillFragment / BillAdapter / BillDetailFragment）支持列表、新增、详情、编辑、删除；默认账户/分类/记账类型/记账后行为已全部接入（见 §11.7）。

`ui/addbill/AddBillBottomSheet.kt` 与 `AddBillFragment.newQuickEntryInstance()`（供 `QuickEntryActivity` 使用）并存，是两个不同的"记一笔"入口，改动记账逻辑时需同时确认两处是否都要更新。

### 11.4 资产 / 账户模块
`ui/assets/` 下功能不变，均已完成：AssetsFragment、AccountDetailFragment、AccountFlowFragment/Adapter/Item（按日期分组、全部/收入/支出/转账筛选、搜索）、AccountSettingsFragment、AddAccountFragment。

### 11.5 转账模块
`ui/assets/`（TransferFragment / TransferDetailFragment / EditTransferFragment）已完成，规则未变。

### 11.6 账单导入
`data/imports/`（XlsxBillParser / CsvBillParser / ImportDuplicateChecker）+ `ui/bills/`（ImportBillFragment / ImportPreviewFragment / ImportPreviewAdapter）。

**XLSX 解析方式：** 不依赖 Apache POI 等第三方库，手写 `ZipInputStream` 解压 + `XmlPullParser` 解析 `xl/sharedStrings.xml` 与工作表 XML。

**微信账单表头（已测试）：** 交易时间、交易类型、交易对方、商品、收/支、金额(元)、支付方式、当前状态、交易单号、商户单号、备注

**重复检测：** 优先 `source + sourceTransactionId`；备用规则为类型相同 + 金额误差 ≤0.01 + 时间误差 ≤60秒 + 商户匹配。

**中性交易 / 转账映射 / 系统隐藏账户（经营账户、日利加）：** 规则未变，详见旧版描述——经营账户提现/取现/转出→经营账户→银行卡；日利加转出→日利加→经营账户；转入日利加→经营账户→日利加；两个系统账户 `enabled=false`、`includeInTotalAssets=false`、`type=OTHER`。

导入流程与生命周期规则见 [第14章](#14-导入数据生命周期)。

### 11.7 我的页面 / 设置

`ui/mine/MineFragment.kt` 是所有二级功能的统一入口，目前跳转关系：

| 菜单项 | 目标 | 状态 |
|---|---|---|
| 分类管理 | `CategoryManageFragment` | 已完成 |
| 预算管理 | `BudgetManageFragment` | 已完成 |
| 资产管理 | `AssetsFragment` | 已完成 |
| 备份与恢复 | `BackupFragment` | **部分完成**（仅账单） |
| 隐私锁 | `PrivacyLockFragment` | 已完成 |
| 修改密码 / 图案 | `ChangePasswordFragment` / `ChangePatternFragment` | 已完成 |
| 安全邮箱 | `ChangeSecurityFragment` → `EmailConfigFragment` | 已完成 |
| 设置 | `SettingsFragment` | 已完成 |
| 导出账单 | `showComingSoon("导出账单")` | **占位，未实现** |

`ui/settings/SettingsFragment.kt` 支持：主题模式、动画效果、默认记账类型、记账成功后行为、日期格式、默认账户、默认分类、金额小数位、统计周期。主题/动画修改后立即生效；**日期格式与小数位设置端已完成，但尚未全局应用到账单列表/流水/详情/统计页**（这一点与旧文档描述一致，仍未解决）。

**默认账户 / 默认分类 / 默认记账类型 / 记账成功后行为：** 均已全部完成，逻辑与旧文档记录一致（`AddBillFragment.updateAccountSpinner()` / `updateCategorySpinner()` / `loadDefaultBillType()` / `handleAfterAddAction()`），无需重复开发。

### 11.8 分类管理（已完成）
`ui/category/CategoryManageFragment.kt`（787 行）+ `CategoryManageAdapter.kt`。

- 支出 / 收入两个分类列表切换
- 新增 / 编辑分类（名称 + emoji 图标）
- `ItemTouchHelper` 实现拖拽排序（更新 `sortOrder`）
- 删除保护：`isSystem=true` 的系统分类禁止删除；有历史账单引用的分类禁止物理删除，只能隐藏（`visible=false`）
- 与记账页共用同一套 `categories` LiveData，改动后调用 `viewModel.refresh()`
- 真正入口是 Fragment，`CategoryActivity` 是未使用的空壳类

### 11.9 预算管理（已完成）
`ui/budget/BudgetManageFragment.kt`（835 行）+ `BudgetAdapter.kt` + `BudgetViewModel.kt`。

- 支持总预算（`BudgetType.TOTAL`）和分类预算（`BudgetType.CATEGORY`）
- 按年/月维度管理，`warningPercent` 控制预警比例（默认 80%）
- `BudgetActivity` 是未使用的空壳类，真正入口是 Fragment

### 11.10 统计页面（已完成）
`ui/statistics/StatisticsFragment.kt`（1278 行），基于 **MPAndroidChart** 实现：

- 月度收支趋势折线图（`LineChart`）
- 分类占比饼图（`PieChart`）
- 支出趋势图
- 月份选择器、分类统计明细列表
- 使用 `settings.statisticsPeriod` 控制统计周期（自然月/近30天/自然年）

### 11.11 隐私锁 · PIN / 图案锁（已完成）

模块：`data/repository/PrivacyLockStore.kt`（256 行）+ `data/model/PrivacyLockConfig.kt` + `ui/privacy/`（PrivacyLockFragment 设置页、PrivacyLockVerifyFragment 启动校验页、PatternLockView 九宫格自绘控件、ChangePasswordFragment、ChangePatternFragment）。

- 加密方案：`PBEKeySpec` + `SecretKeyFactory`（PBKDF2WithHmacSHA1），**120000 次迭代**，**256 位密钥**，随机盐，存储在 SharedPreferences「privacy_lock」（**不是** JSON 文件，**不在**当前备份范围内）
- `MainActivity.onCreate()` 中调用 `checkPrivacyLock()`：若已启用，启动即显示 `PrivacyLockVerifyFragment`，校验通过才能进入主界面
- **与 `AppSettings.lockType/pin/pattern` 无关**——那三个字段是旧方案的死字段，已完全被本模块取代（见 §9.4）
- `LockActivity` 是未使用的空壳类

### 11.12 安全邮箱 · 密码找回（已完成）

模块：`data/model/EmailConfig.kt` / `EmailProvider.kt` / `EmailVerification.kt` + `data/repository/EmailConfigStore.kt` / `EmailCredentialStore.kt` / `EmailVerificationStore.kt` + `data/email/EmailConnectionTester.kt`（350 行，基于 jakarta.mail 做 IMAP/SMTP 连接测试）+ `ui/email/EmailConfigFragment.kt`（986 行）/ `EmailVerificationFragment.kt`（440 行）+ `ui/privacy/ChangeSecurityFragment.kt` / `ResetPasswordFragment.kt` / `ResetPatternFragment.kt`。

- 支持 QQ / 163 / 126 / 新浪 / Gmail / Outlook / 自定义邮箱，预置常见服务商的 IMAP/SMTP 地址和端口
- 流程：绑定邮箱 → 发送验证码测试连接 → 验证通过后标记 `verified=true` → 忘记 PIN/图案时可通过邮箱验证码重置（`ResetPasswordFragment` / `ResetPatternFragment` 内部调用 `PrivacyLockStore` 完成重置）
- 需要 `INTERNET` 权限，是目前项目中**唯一联网的功能**
- 配置和验证码均存储在 SharedPreferences，不在 JSON 备份范围内

### 11.13 首页快捷操作 + 桌面快捷方式（已完成）

- `ui/home/QuickAction.kt` / `QuickActionAdapter.kt` / `QuickActionStore.kt`：首页快捷操作区可自定义显示/排序，独立存储在 `quick_actions.json`（与 `JsonDataStore` 管理的 6 个核心文件分开）
- `ui/quick/QuickEntryActivity.kt`：桌面长按图标弹出的应用快捷方式（`res/xml/shortcuts.xml` 配置）对应的独立 Activity，内部直接加载 `AddBillFragment.newQuickEntryInstance()` 实现快速记一笔，已在 `AndroidManifest.xml` 中注册（`exported=false`）

### 11.14 崩溃捕获与日志（已完成，非用户可见功能）

- `utils/CrashHandler.kt`：`MoneyBookApplication.onCreate()` 中安装全局 `UncaughtExceptionHandler`
- `utils/AppLogger.kt`：本地日志写入 app 私有目录 `logs/moneybook.log`

### 11.15 尚未实现的模块

- **Excel 导出**（`utils/ExcelExporter.kt`）：**空类**，"我的 → 导出账单"仍是占位 Toast
- **自然语言记账**（`utils/NaturalLanguageParser.kt`）：**空类**，未接入任何页面
- **备份恢复完整化**（`BackupRepository.kt`）：目前只备份/恢复 `records.json`；`accounts / categories / budgets / transfers / settings` 均未纳入，代码注释写明"后续分类、设置等 JSON 加入后，直接扩展这里即可"
- **日期格式 / 小数位全局应用**：设置项已保存，但账单列表、流水、详情、统计页尚未读取这两个设置来格式化显示

### 11.16 历史遗留空壳类（不要误认为已完成，也不要重建）

`CategoryActivity`、`BudgetActivity`、`BackupActivity`、`LockActivity`、`AccountActivity`、`ImportBillActivity`、`utils/BackupManager.kt` —— 均为内容为空或未被 `AndroidManifest.xml` 引用的历史结构。**当前 Manifest 中只注册了 `MainActivity` 和 `QuickEntryActivity`**，其余全部功能都通过 Fragment + 底部导航/`MineFragment` 二级菜单实现。不要仅因文件存在就认为功能完成，也不要在未确认的情况下重建第二套同类系统。

---

## 12. 核心业务规则

1. **资产余额公式：** `balance = account.balance + income − expense − transferOut + transferIn`
2. **负资产问题：** 若导入后出现负余额，通常是因为历史账单开始前的初始余额未正确设置为真实值——应修正初始余额，而不是改动计算公式
3. **统计规则：** 只有 `Bill.INCOME` / `Bill.EXPENSE` 参与收支统计；`Transfer` 永远不计入收入或支出
4. **账户显示规则：** `enabled=true` 才在普通资产页显示；`includeInTotalAssets=true` 才计入总资产；系统账户（经营账户、日利加）不参与普通总资产
5. **重复导入保护：** 优先 `source + sourceTransactionId`，否则按类型/金额/时间/商户备用匹配
6. **隐私锁与安全邮箱相互独立于账本数据**：修改隐私锁/邮箱相关功能不会影响 `records/accounts/categories/budgets/transfers/settings` 六个 JSON 文件

---

## 13. 数据删除 / 安全规则

| 操作 | 规则 |
|---|---|
| **删除 Bill** | 允许直接删除。`records.json` 更新；不修改账户初始余额、账户 ID、分类 |
| **删除 Transfer** | 允许删除。`transfers.json` 更新；来源/目标账户重新计算余额 |
| **删除 Account** | 以下情况禁止删除：① 系统账户　② 账户存在 Bill　③ 账户存在 Transfer |
| **删除 Category** | 若存在历史 Bill 使用该分类，不能物理删除；优先禁用分类（`visible=false`） |
| **清空所有数据** | 必须二次确认；清空全部 6 个 JSON 文件后重新执行 `DefaultDataInitializer`（不影响隐私锁/邮箱/快捷操作等独立存储） |

---

## 14. 导入数据生命周期

任何导入文件**不得直接写入 `records.json`**，必须经过：

```
解析 → 预览 → 重复检测 → 转账识别 → 账户匹配 → 用户确认 → 保存
```

---

## 15. 开发规范

- **RecyclerView：** 需要完整滚动的列表避免 `ScrollView + RecyclerView(wrap_content)`；推荐固定顶部区 + `FrameLayout` + `RecyclerView(match_parent)`
- **编译错误处理：** 不要凭猜测修改；优先让用户提供真实 Build Output 或具体报错
- **不要重复造轮子：** 已有 `Bill / Account / Transfer / AppSettings / Category / Budget / PrivacyLockConfig / EmailConfig / FinanceRepository / JsonDataStore / PrivacyLockStore / MainViewModel`，优先复用
- **不要破坏已有 JSON 字段：** 新增字段需提供默认值，尽量不删除旧字段
- **隐私锁/邮箱相关改动不要误接到 `AppSettings.lockType/pin/pattern`**——那是死字段，真正逻辑在 `PrivacyLockStore`

---

## 16. 禁止事项

未经用户明确同意，不允许：

- 改用 Room / SQLite / Compose
- 引入服务器、登录系统、网络同步（安全邮箱的 IMAP/SMTP 除外，这是已确认的现有功能）
- 删除 Gson 或 JsonDataStore
- 修改 `applicationId` / `namespace` / JSON 文件名称
- 删除已有模型字段，或修改已有字段含义
- 新建第二套 FinanceRepository，或第二套 Account / Bill / Transfer 模型
- 创建重复的记账页面
- 把 Transfer 当成 Bill，或计入收入/支出
- 用账户名称代替 `accountId` 保存
- 用 UUID 后四位识别银行卡
- 修改历史账单的 `accountId` 来"适配"默认账户
- 为了修复余额问题而修改余额计算公式
- 重新启用 `CategoryActivity` / `BudgetActivity` / `BackupActivity` / `LockActivity` 这些空壳类，或在其中重新实现已在对应 Fragment 中完成的功能

---

## 17. 测试数据与验证状态

### 17.1 微信 XLSX 导入测试
输入 701 条 → 解析结果：372 支出 + 255 收入 + 74 中性 = 701（校验通过）

### 17.2 转账流水测试
72 条转账，历史 Bug（ScrollView 嵌套 RecyclerView 导致只显示 3 条）已修复。

### 17.3 账户余额测试
现金、微信、支付宝、银行卡账户的收入/支出/转入/转出均验证正确。

### 17.4 本次核对确认代码中存在且已接入导航的模块
在原有 17 个核心文件基础上，新增确认：CategoryManageFragment/Adapter、BudgetManageFragment/Adapter/ViewModel、StatisticsFragment、BackupFragment/ViewModel/BackupRepository、PrivacyLockStore/Config、PrivacyLockFragment/VerifyFragment/PatternLockView/ChangePasswordFragment/ChangePatternFragment/ChangeSecurityFragment/ResetPasswordFragment/ResetPatternFragment、EmailConfigFragment/VerificationFragment、EmailConfigStore/CredentialStore/VerificationStore、EmailConnectionTester、QuickEntryActivity、QuickAction/Adapter/Store、CategoryDonutChartView、MoneyBookApplication、CrashHandler、AppLogger

### 17.5 本次核对确认为空壳 / 未实现
`ExcelExporter`、`NaturalLanguageParser`、`BackupManager`、`CategoryActivity`、`BudgetActivity`、`BackupActivity`、`LockActivity`（均为空类或未注册于 Manifest）

### 17.6 本次核对未做的事
未执行 `gradlew build` 编译验证（当前环境无法联网拉取 Gradle 依赖/运行 Android 工具链），本文档结论均基于**源码静态阅读**，不代表已通过实际编译或运行测试。

---

## 18. 当前功能状态一览

| 功能 | 状态 | 当前说明 |
|---|---|---|
| JSON 本地存储 / Gson | 已完成 | `JsonDataStore` + 既有 JSON 文件结构保留 |
| Bill / Account / Transfer / Category / Budget 模型 | 已完成 | 字段语义未改 |
| AppSettings / FinanceRepository / MainViewModel / MainActivity | 已完成 | 核心架构保留 |
| 首页（快捷操作、分类环形图、最近账单） | 已完成 | 未做破坏性拆分；**09-18 复核：`HomeFragment` 仍 2031 行、职责过多，"日历账单"入口实际打开普通账单列表，见 `REFACTOR_20260918.md`** |
| 账单新增 / 详情 / 列表 | 已完成 | 保留现有入口和筛选逻辑 |
| 资产页 / 账户详情 / 流水 / 新增 / 设置 | 已完成 | 账户关联继续使用 `Account.id`；**09-18 复核：账户卡片仍是手写 `LinearLayout`/`addView`（非 RecyclerView），且图标/间距为裸像素值未做 dp 转换** |
| 转账 / 详情 / 编辑 / 删除 | 已完成 | `Transfer` 不计入收支统计 |
| CSV / XLSX 导入 / 预览 / 重复检测 / 转账识别 | 已完成 | 导入链路保留 |
| 默认账户 / 默认分类 / 默认记账类型 / 记账后行为 | 已完成 | 设置保存逻辑保留 |
| 分类管理 | 已完成 | 新增/编辑/隐藏/删除保护/拖拽排序 |
| 预算管理 | 已完成 | 总预算 + 分类预算 |
| 统计页面 | 已完成 | 趋势图 + 分类占比等 |
| 隐私锁 | 已完成 | PIN / 图案，`PrivacyLockStore` + PBKDF2 |
| 安全邮箱 | 已完成 | IMAP/SMTP，仅用于安全邮箱验证/找回 |
| 首页快捷操作 + 桌面快捷方式 | 已完成 | `quick_actions.json` + `QuickEntryActivity` |
| 崩溃捕获 / 本地日志 | 已完成 | `CrashHandler` + `AppLogger` |
| 主题切换 / 动画设置 | 已完成 | 设置端可保存 |
| 设置页代码/布局瘦身 | **已完成（本次）** | 单选 Dialog 与重复 Row/Card Style 已统一 |
| 备份恢复 | 部分完成 | 当前只处理 `records.json`，其余核心 JSON 尚未纳入（09-18 复核：问题原样存在，属于数据安全风险，建议优先处理） |
| Excel 导出 | 未实现 | `ExcelExporter.kt` 仍为空类，保留占位 |
| 自然语言记账 | 未实现 | `NaturalLanguageParser.kt` 仍为空类，未接入入口 |
| 日期格式 / 金额小数位全局应用 | 未完成 | 设置可保存，但显示层尚未全面读取 |
| Android 正式编译验证 | 未完成 | 当前环境无法联网下载 Gradle 9.0.0 |
| "我的"页"数据统计"入口 | **接线未完成** | 点击仍是占位 Toast，但底部导航"统计" Tab 功能已完整实现，属于低成本可修复问题（09-18 新记录） |
| 密码/图案 修改与重置代码重复 | 未处理 | `ChangePasswordFragment`/`ResetPasswordFragment`、`ChangePatternFragment`/`ResetPatternFragment` 各自一对代码 90% 以上相同（09-18 新记录） |
| `JsonDataStore` CRUD 重复 | 未处理 | Bill/Account/Category/Budget/Transfer 五组 get/save/add/update/delete 结构高度相似，可用泛型收敛（09-18 新记录） |

## 19. 当前开发路线

当前代码已经进入“**先保留未完成功能，再继续局部瘦身与补功能**”阶段。瘦身不等于删除功能：任何尚未实现但已有入口/占位/模型的能力都继续保留。

```text
① 备份恢复补全
   └─ BackupRepository：accounts/categories/budgets/transfers/settings

② Excel 导出实装
   └─ 从空类 ExcelExporter.kt 开始，接入现有“导出账单”入口

③ 自然语言记账实装
   └─ 从空类 NaturalLanguageParser.kt 开始，先确定输入入口与解析规则

④ 日期格式全局应用
   └─ settings.dateFormat → 账单列表/详情/流水/统计

⑤ 金额小数位全局应用
   └─ settings.decimalPlaces → 账单/详情/流水/统计/资产

⑥ 深层代码瘦身（逐模块）
   └─ Home → Bills/ImportPreview → Statistics → EmailConfig
   └─ 原则：拆职责、抽通用状态/计算，不改变现有功能与数据语义

⑦ 全项目回归测试

⑧ 发布版本
```

本次已完成的是设置页这一局部瘦身，不把第⑥项一次性扩大到所有高复杂度 Fragment，以降低未编译环境下的回归风险。
## 20. AI 接手工作协议

当用户说"**继续开发 MoneyBook**"时，AI **不应该**重新询问：项目是什么、用什么语言、是否使用 Room、是否联网、当前有哪些功能——本文档已提供这些信息。

AI 应该首先：

1. 阅读本文档
2. 确认当前开发断点（见文档顶部，或第 19 章）
3. 明确本次只开发一个功能
4. 请求 / 检查对应源码（**不要假设** `ExcelExporter` / `NaturalLanguageParser` / `BackupActivity` 等空壳类里有内容）
5. 给出完整文件
6. 用户替换代码并编译
7. 根据真实编译结果继续
8. 功能测试通过后更新本文档

**其余原则：**

1. 不要从零重新设计 MoneyBook；不要建议 Room 或 Compose；不要改服务器架构或 JSON 存储方案（安全邮箱的 IMAP/SMTP 是已确认的例外）
2. 不要删除现有数据模型，不要随便修改已有 JSON 字段含义
3. 不要创建重复的 Repository 或重复的 Account/Bill/Transfer 模型
4. 修改 Kotlin 文件时给完整文件；修改 XML 时给完整 XML
5. 一次只开发一个功能；修改后让用户编译，确认后再进行下一步
6. 遇到错误先看真实 Build Output，不要凭猜测大范围修改
7. `Transfer` 永远不计入收入/支出；`accountId`/`defaultAccountId` 必须用账户 ID 而非名称
8. 系统账户"经营账户""日利加"不能作为普通资产显示
9. **隐私锁/安全邮箱功能改动，去看 `PrivacyLockStore`/`EmailConfigStore` 等，不要去改 `AppSettings.lockType/pin/pattern`**
10. **不要重新激活 `CategoryActivity`/`BudgetActivity`/`BackupActivity`/`LockActivity` 这些空壳类**

**当前断点：**
```
①备份恢复补全其余 5 个 JSON → ②Excel 导出实装 → ③自然语言记账实装 → ④日期格式/小数位全局应用
```

若用户说"继续开发 MoneyBook"，应先跟用户确认从①~④中选哪一项开始，而不是自行假设。

---

## 21. 项目变更日志

### 2026-09-18 — 全量只读审查（未修改代码）
- 通读 `JZ_backup_20260918_120622.zip` 全部 85 个 Kotlin 文件，确认文件结构相对 09-16 无增减。
- 确认 09-16 记录的 `MineFragment` 密码入口命名不一致问题已修复。
- 确认 `HomeFragment` 臃肿、`ImportPreviewFragment` 职责混杂、`AssetsFragment` 手写 View、`BackupRepository` 范围不全、密码/图案 Fragment 重复、`JsonDataStore` 重复 CRUD 等问题原样存在。
- 新发现：`AssetsFragment` 账户卡片使用裸像素值，未做 dp 转换；硬编码中文文件数由 36/85 升至 43/85。
- 完整细节与逐页功能状态见 `REFACTOR_20260918.md`。

### 2026-09-16 — 第二阶段：设置页与资源层瘦身
- `SettingsFragment.kt`：将 7 组重复的单选设置弹窗统一为 `showChoiceDialog()`，保留原有选项、默认值映射和保存逻辑。
- `fragment_settings.xml`：9 个设置 Row 统一使用 `MoneyBookSettingsRow`；5 个设置 Card 统一使用 `MoneyBookSettingsCard`，减少重复 XML 属性。
- `values/styles.xml`：新增 `MoneyBookSettingsCard` 通用样式。
- 删除项目中的 `.acside` 编辑器缓存目录，不参与 Android 构建。
- 全项目 drawable 静态引用扫描未确认出可安全删除资源，因此本阶段没有误删 drawable。
- 保留 `ExcelExporter`、`NaturalLanguageParser`、`BackupManager` 以及 4 个空壳 Activity，不因“瘦身”而删除潜在未完成功能。
- 新增/维护 `ui/common/FragmentNavigation.kt`，统一 Fragment transaction 模板。
- 更新 `README.md`、`AGENTS.md`、`GEMINI.md`、`CHANGELOG.md`、本完整开发记录及 UI 更新记录，使功能状态、当前断点和代码树一致。

### 2026-09-16 — 验证状态
- Android XML 全量解析通过。
- Kotlin 大括号结构静态检查通过。
- Gradle Wrapper 已补回可执行权限。
- 尝试执行 `./gradlew :app:assembleDebug --no-daemon`，因环境无法访问 `services.gradle.org` 下载 Gradle 9.0.0，未完成真实 Android 编译；因此本次 ZIP **不能声明为已编译通过**。

### 2026-09-16 — 全量代码核对
- 通读最新项目代码并纠正文档状态：分类管理、预算管理、统计、隐私锁、安全邮箱、首页快捷操作、桌面快捷方式等均按实际源码记录为已完成。
- 明确记录 `ExcelExporter.kt`、`NaturalLanguageParser.kt`、`BackupManager.kt` 为空类，以及 `CategoryActivity.kt`、`BudgetActivity.kt`、`BackupActivity.kt`、`LockActivity.kt` 为空壳且未注册 Manifest。
- 明确 `AppSettings.lockType/pin/pattern` 为死字段，隐私锁实际由 `PrivacyLockStore` + `PrivacyLockConfig` 负责。

### 2026-09-14
- 完成默认分类设置端与接入；修复底部导航文字裁剪；资产页账户图标改 PNG。

### 2026-09-13
- 完善项目完整开发文档；确认真实项目树；确认默认账户设置。

### 2026-09-06
- 修复 Buildozer 项目相关问题。

### 2026-08
- 完成账户流水；完成转账编辑；完成微信 XLSX 导入；完成中性交易识别。

## 22. 功能扩展建议（未来可选，非当前路线）

以下是在第 19 章「当前开发路线」之外，讨论过的可选功能建议。**均为纯离线方案**（不引入新的联网依赖，安全邮箱的 IMAP/SMTP 除外，那是已有功能）。这些不是已确认要做的需求，只是候选清单，实际是否开发、开发顺序需要用户单独决定，不要在没有明确指示时主动开工。

### 22.1 优先级高：把已有功能补完

与第 19 章重复，仅在此再次强调优先级最高：备份恢复覆盖全部 6 个 JSON 文件（并支持导出到外部存储/分享，而不只是留在 app 私有目录）、Excel 导出、自然语言记账、日期格式与小数位全局应用。

### 22.2 低成本、纯本地计算，可以直接基于现有数据模型实现

- **账单搜索**：按备注关键字、金额区间、分类组合搜索全部账单（`AccountFlowFragment` 已有部分筛选/搜索逻辑，可抽取复用到全局账单列表）
- **年度/月度总结卡片**：基于 `FinanceRepository` 已有数据统计生成一张可分享的图片/卡片（收支对比、Top5 分类、最大单笔支出等），不需要新的数据模型
- **常用备注/标签**：记账时快速选择历史高频备注，纯本地统计 `Bill.note` 出现频率即可，无需新字段
- **批量操作**：账单列表多选后批量删除、批量改分类（导入后经常需要批量修正），复用现有 `deleteBill`/`updateBill` 接口

### 22.3 中等工作量，需要新增字段或新模型，但仍然纯本地

- **周期性账单/订阅提醒**（房租、话费、会员）：需要新增一个 `RecurringBill` 模型 + 本地通知（`AlarmManager`/`WorkManager`，均不需要联网），到期自动生成 `Bill` 或提醒用户手动记
- **信用卡还款日提醒**：在 `Account` 模型上加"账单日/还款日"可选字段，配合本地通知提醒，不需要新增整个模型
- **手动汇率的多币种支持**：如果只需要手动输入汇率（不需要实时汇率联网获取），可以在 `Account` 或 `AppSettings` 上加币种字段，纯本地换算
- **统计图表下钻**：`StatisticsFragment` 饼图点击某个分类后跳转到该分类下的账单列表，纯 UI 层改动，不涉及数据结构

### 22.4 成本较高，建议谨慎评估后再做

- **多账本/家庭账本隔离**：需要给 `Bill`/`Account`/`Category`/`Budget`/`Transfer` 全部加 `bookId` 字段并改造 `FinanceRepository` 的查询逻辑，改动面很大，与项目"最小改动"原则有明显冲突，如果要做建议单独立项讨论，不要当作普通功能顺手加
- **OCR 识别小票**：纯本地 OCR（如 ML Kit 离线模型）技术上可行，但需要引入新的重量级依赖、处理相机权限和图片流程，工作量和体积都会明显上升
- **桌面 Widget 小组件**：展示今日/本月收支，技术上独立于主 App，但需要额外维护一套 RemoteViews UI 和刷新逻辑

---

*文档结束 — 项目：MoneyBook · 包名：com.example.myno.jz*
*当前阶段：核心记账+资产+导入+分类+预算+统计+隐私锁+安全邮箱均已完成 · 当前断点：备份恢复补全 / Excel 导出 / 自然语言记账 / 日期格式与小数位全局应用（四选一，需与用户确认顺序）*
