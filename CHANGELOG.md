# 更新日志（CHANGELOG）

本文件记录 MoneyBook 项目的重要变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

> 说明：项目此前未维护 CHANGELOG 且无 Git 提交历史可追溯，以下 **[1.0.0]** 条目是基于对当前完整代码快照（`JZ_backup_20260919_065806.zip`）的全量代码审查整理出的**基线（Baseline）记录**，用于标记"文档化起点"，并非按时间线还原的真实开发过程。此后的变更请按时间顺序追加到本文件顶部的 `[Unreleased]` 区块。

## [Unreleased]

### 待办 / 计划中
- 补充单元测试（`FinanceCalculator`、`CsvBillParser`/`XlsxBillParser`、`ImportDuplicateChecker`）
- 统一 CSV 与 XLSX 导入解析器对"中性交易（转账类）"的识别逻辑
- 清理空占位类：`AccountActivity`/`BudgetActivity`/`CategoryActivity`/`LockActivity`/`BackupActivity`/`ImportBillActivity`/`AddBillBottomSheet`/`BackupManager`/`NaturalLanguageParser`
- 补全「我的」页面中"即将接入"的功能入口（个人信息、导出账单、意见反馈）

---

## [1.0.0] - 2026-09-19（基线记录）

首次全量代码梳理与文档化时的功能快照。

### 新增（已实现的核心功能）

- **记账核心**：支出/收入手动记账（`AddBillFragment`），支持连续记账、记账后跳转行为可配置（首页/账单列表/继续记账）
- **账单列表**：月份筛选、类型筛选、精确时间范围筛选，可拖拽悬浮记账按钮
- **资产管理**：多账户（现金/微信/支付宝/银行卡/自定义），账户间转账，账户流水按日期分组展示，账户当前余额动态计算（期初余额 + 收入 − 支出 + 转入 − 转出）
- **分类管理**：支出/收入分类的新增、编辑、拖拽排序、删除（系统预置分类禁止删除）
- **预算管理**：总预算与分类预算，按年月维度设置，支持预警比例
- **统计分析**：基于 MPAndroidChart 的当月分类占比饼图，日/月/年三种周期的收支趋势图
- **账单导入**：
  - `CsvBillParser`：微信 CSV 账单专用解析 + 通用 CSV/TXT 解析（自动探测分隔符与表头列）
  - `XlsxBillParser`：手写 ZIP/XML 解析实现的微信 XLSX 账单解析器，支持 Excel 序列日期
  - `ImportDuplicateChecker`：按来源单号或"类型+金额+时间+商户"模糊匹配去重
  - `ImportPreviewFragment`：导入预览、账户自动匹配（微信/支付宝/经营/日常/银行等多层启发式规则）、分类自动匹配、转账识别与确认落库
- **账单导出**：`ExcelExporter` 零依赖手写生成 XLSX（账单明细/统计汇总/账户汇总三个工作表），附带 `validate()` 自检
- **本地备份/恢复**：`BackupRepository` 全量 JSON 备份，支持向后兼容 v1 格式恢复
- **隐私锁**：PIN 密码 / 图案锁两种模式，PBKDF2WithHmacSHA256（120,000 次迭代）加盐哈希存储；忘记密码走邮箱验证后重置
- **图案锁自绘控件**：`PatternLockView` 手动实现 3×3 九宫格绘制与触摸路径识别
- **邮箱安全验证体系**：
  - 支持 QQ/163/126/新浪/Gmail/Outlook/自定义邮箱预设 IMAP/SMTP 参数
  - `EmailConnectionTester` 基于 Jakarta Mail 实现连接测试与验证码发送
  - 邮箱授权码通过 AndroidKeyStore AES/GCM 加密存储
  - 验证码 SHA-256 哈希存储，5 分钟过期、60 秒重发冷却、5 次尝试上限
- **应用设置**：主题模式（跟随系统/浅色/深色）、动画开关、默认记账类型、记账后行为、日期格式、金额小数位、统计周期、默认账户/分类
- **首页个性化**：快捷入口可拖拽排序与滑动隐藏（`QuickActionStore` 独立持久化）、余额卡片翻转动画、金额可见性开关（隐藏时显示掩码）
- **应用崩溃与日志系统**：`AppLogger` 文件日志 + `CrashHandler` 全局未捕获异常捕获
- **桌面快捷方式**：长按图标「快速记一笔」直达 `QuickEntryActivity`

### 已知限制（本版本仍存在）

- 无自动化测试（单元测试与 Instrumented 测试均缺失）
- 7 个空占位 Activity/类未接入实际功能（详见开发文档第 11 节）
- CSV 与 XLSX 导入解析器对"中性交易"处理逻辑不完全一致
- 多数页面各自持有独立的 `FinanceRepository` 实例，跨页面数据变更不会自动互相刷新
- 本地备份文件为明文 JSON，未加密
- 「我的」页面部分入口（个人信息、导出账单快捷方式、意见反馈）仅为占位提示，未实现
- UI 文案基本硬编码，暂无国际化支持

---

*后续每次发布或完成一批有意义的改动后，请在本文件顶部新增对应版本区块，并遵循「新增 / 变更 / 修复 / 移除」的分类记录方式。*
