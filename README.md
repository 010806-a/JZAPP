# MoneyBook（JZ 记账 App）

一款**完全离线**的 Android 个人记账应用。所有账单、账户、分类、预算和设置数据均保存在本机（JSON 文件 + SharedPreferences + Android Keystore），不依赖网络即可正常使用（仅"邮箱安全验证"这一项可选功能需要网络）。

> 包名：`com.example.myno.jz` ｜ 语言：Kotlin ｜ UI：View + ViewBinding（无 Compose）｜ minSdk 23 / targetSdk 34 / compileSdk 36

## 功能特性

- 📒 **记账**：支持支出/收入手动记账，自定义分类、账户，快速记账桌面快捷方式
- 💰 **资产管理**：多账户（现金/微信/支付宝/银行卡/自定义），账户间转账，账户流水明细，动态计算当前余额
- 📊 **统计分析**：当月分类占比饼图、日/月/年三种周期趋势图（基于 MPAndroidChart）
- 🎯 **预算管理**：总预算 + 分类预算，可设置预警比例
- 📥 **账单导入**：支持导入微信 / 通用格式的 **CSV** 与 **XLSX** 账单，自动识别收入/支出/转账类交易，自动匹配账户与分类，导入前可预览与去重
- 📤 **账单导出**：一键导出 XLSX（账单明细 + 统计汇总 + 账户汇总三个工作表），**完全离线自研生成，不依赖任何第三方 Office 库**
- 🔒 **隐私锁**：应用级 PIN 密码 / 图案锁，PBKDF2 加盐哈希存储
- 📧 **邮箱安全验证**：修改隐私锁、找回密码等敏感操作前的邮箱验证码二次确认（支持 QQ/163/126/新浪/Gmail/Outlook/自定义邮箱）
- 💾 **本地备份/恢复**：一键创建/恢复全量数据备份
- 🎨 **个性化**：主题模式（跟随系统/浅色/深色）、首页快捷入口可拖拽排序与隐藏、金额可见性开关、动画开关等

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Kotlin 2.1.0 |
| 架构 | 轻量 MVVM（`AndroidViewModel` + `LiveData`），无 DI 框架 |
| UI | View + XML 布局 + ViewBinding，Material Design 3 |
| 持久化 | Gson 序列化的本地 JSON 文件 + SharedPreferences + AndroidKeyStore |
| 图表 | MPAndroidChart v3.1.0 |
| 邮件 | Jakarta Mail 2.0.3（IMAP/SMTP） |
| 构建 | AGP 8.13.0 + Gradle Kotlin DSL |

## 项目结构

```
app/src/main/kotlin/com/example/myno/jz/
├── data/
│   ├── model/        数据模型（Bill、Account、Category、Budget、Transfer、AppSettings…）
│   ├── local/        JsonDataStore（通用 JSON CRUD）、DefaultDataInitializer（默认数据）
│   ├── repository/   FinanceRepository、BackupRepository、PrivacyLockStore、邮箱相关 Store
│   ├── imports/       CSV/XLSX 账单解析器、去重逻辑
│   └── email/         IMAP/SMTP 连接测试与验证码发送
├── ui/
│   ├── main/          应用外壳：底部导航 + 隐私锁拦截
│   ├── home/          首页：余额卡片、快捷入口、预算预览
│   ├── bills/          账单列表/详情/新增/导入预览
│   ├── assets/        资产（账户）与转账
│   ├── budget/        预算管理
│   ├── category/      分类管理
│   ├── statistics/    统计图表
│   ├── backup/        本地备份/恢复
│   ├── email/         邮箱配置与验证
│   ├── privacy/       隐私锁（设置/校验/修改/重置）
│   ├── settings/      应用设置
│   └── mine/          「我的」功能入口聚合页
└── utils/             日志、崩溃捕获、日期与收支计算、自研 XLSX 导出
```

更完整的架构说明、数据流、全部类清单与已知问题，请见：**[`JZAPP_项目完整开发文档.md`](./JZAPP_项目完整开发文档.md)**。

## 构建

```bash
./gradlew :app:assembleDebug
```

首次构建需要网络下载 Gradle 依赖（AndroidX、Material、MPAndroidChart via JitPack、Jakarta Mail、Gson 等）。

- **minSdk**: 23（Android 6.0）
- **targetSdk**: 34
- **compileSdk**: 36
- **JDK**: 17

## 隐私与安全说明

- 应用**不上传任何数据到服务器**，`allowBackup="false"`，不参与系统级自动云备份。
- 隐私锁密码/图案以 PBKDF2（120,000 次迭代）加盐哈希存储，不可逆、无本地"万能钥匙"。
- 邮箱授权码使用 Android Keystore 生成的 AES 密钥加密存储，密钥不出设备。
- 本地备份文件当前为**明文 JSON**，请妥善保管导出的备份文件。

## 已知限制

- 首个版本，尚无自动化测试。
- 部分菜单入口（个人信息、导出账单快捷入口、意见反馈）尚未实现，点击会提示"即将接入"。
- CSV 与 XLSX 两条导入解析路径对"转账类"交易的识别逻辑尚不完全一致。

更多细节见 [`JZAPP_项目完整开发文档.md`](./JZAPP_项目完整开发文档.md) 第 11 节「已知问题 / 技术债」。

## 面向 AI 编程助手

如果你是 AI 编程助手（Claude Code、Gemini CLI、Codex 等），在修改本仓库前请先阅读：

- [`AGENTS.md`](./AGENTS.md) — 通用编码规范与注意事项
- [`GEMINI.md`](./GEMINI.md) — Gemini CLI 专用补充说明

## 版本记录

见 [`CHANGELOG.md`](./CHANGELOG.md)。
