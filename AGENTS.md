# AGENTS.md

面向在本仓库中工作的 AI 编程助手（Claude Code、Codex、Cursor 等通用编码 Agent）的操作说明。详细的架构与全量类说明见 [`JZAPP_项目完整开发文档.md`](./JZAPP_项目完整开发文档.md)，人类可读概览见 [`README.md`](./README.md)。若你是 Gemini / Gemini CLI，请优先阅读 [`GEMINI.md`](./GEMINI.md)（内容与本文件同步，仅少量工具差异）。

## 项目一句话说明

MoneyBook（`com.example.myno.jz`）是一个**完全离线**的 Android 记账 App：Kotlin + View/XML + ViewBinding，无 Room、无 DI 框架，数据以 JSON 文件 + SharedPreferences + AndroidKeyStore 保存在本地。**不要**引入云同步、账号体系或联网数据上报之类的架构性变化，除非用户明确要求。

## 构建与验证

- 该沙箱环境**默认无网络**，`./gradlew` 首次同步需要下载依赖，很可能无法在此环境中完整跑通构建；除非确认网络可用，否则**不要**假设 `./gradlew build`/`assembleDebug` 会成功，也不要仅凭"命令已执行"就断言修改无误。
- 若网络可用，标准验证命令：
  ```bash
  ./gradlew :app:assembleDebug
  ./gradlew :app:lint
  ```
- 本项目**没有任何 `test`/`androidTest` 目录**。如果你新增了纯逻辑代码（尤其是 `utils/`、`data/imports/` 下的解析器），**优先补充 JUnit 单元测试**放在 `app/src/test/kotlin/...`（目录需新建），不要假定已有测试基线。
- 修改 XML 布局后，请确认对应 Kotlin 文件里通过 ViewBinding 引用的 id（如 `binding.tvXxx`）与布局中的 `android:id` 完全对应，ViewBinding 是编译期生成的，id 拼写错误会导致整个模块编译失败。

## 代码风格约定（务必遵守，保持与现有代码一致）

1. **注释语言**：全部为**中文** KDoc/行内注释。新增代码请沿用中文注释风格，不要混用英文注释。
2. **格式风格**：本仓库大量代码采用"**每个具名参数独占一行**"的换行风格，例如：
   ```kotlin
   Account(
       id = "cash",
       name = "现金",
       type = AccountType.CASH,
       icon = "cash",
       balance = 0.0,
       sortOrder = 0
   )
   ```
   以及函数调用/声明中形参逐行换行。这不是强制的 Kotlin 官方风格，但为了 diff 干净、代码 review 一致，**在这些区域做增量修改时请沿用相同的换行密度**，不要把整段重新压缩成一行以求"简洁"。
3. **包结构**：新代码请放入与职责匹配的既有包：
   - 纯数据模型（无逻辑）→ `data.model`
   - 本地持久化实现 → `data.local` / `data.repository`
   - 账单导入相关解析/规则 → `data.imports`
   - 邮件网络交互 → `data.email`
   - 与 Android Context 无关的纯函数/工具 → `utils`
   - 页面 → `ui.<功能域>`，Fragment 命名 `XxxFragment`，ViewModel 命名 `XxxViewModel`，Adapter 命名 `XxxAdapter`。
4. **禁止引入的依赖/模式**：
   - 不要引入 Room、DataStore、Retrofit、Hilt/Koin、Compose 等新框架，除非用户明确要求做架构升级——当前项目刻意保持轻量、零 DI、View+ViewBinding。
   - 不要给 `ExcelExporter`/`XlsxBillParser` 引入 Apache POI 等第三方 Office 库——这两个类的设计初衷就是"零依赖手写 OOXML"，引入 POI 会显著增加包体积并偏离原有架构决策。
   - 不要把日志改成 `Log.d` 散落调用；统一使用已有的 `com.example.myno.jz.utils.AppLogger`（`i`/`w`/`e`/`crash`）。
5. **数据模型变更要格外小心**：`Account`/`Bill`/`Category`/`Budget`/`Transfer`/`AppSettings` 等 data class 是**直接被 Gson 序列化进本地 JSON 文件**的（见 `JsonDataStore`），意味着：
   - **新增字段必须提供默认值**（否则老用户的本地 JSON 反序列化会失败或字段为 null 引发崩溃）；
   - **不要重命名/删除已有字段**，除非同时在 `JsonDataStore`/`BackupRepository` 中写好数据迁移逻辑；
   - `Account`/`Category` 里的 `id`（如 `"cash"`/`"wechat"`/`"alipay"`/`"bank"`）被 `DefaultDataInitializer` 与导入模块（`ImportPreviewFragment` 的账户自动匹配）**硬编码引用**，改动这些 id 会破坏导入自动匹配逻辑。
6. **UI 文案**：目前绝大多数中文文案是硬编码在 Kotlin/XML 中的（`strings.xml` 里只有个位数条目），**新增文案时沿用现状（硬编码）即可**，除非用户明确要求做字符串资源化/国际化改造。

## 已知的"陷阱"文件（改动前务必先读一遍上下文）

- `ui/*/XxxActivity.kt`（`AccountActivity`/`BudgetActivity`/`CategoryActivity`/`LockActivity`/`BackupActivity`/`ImportBillActivity`）以及 `ui/addbill/AddBillBottomSheet.kt`：**空占位类，未在 `AndroidManifest.xml` 中注册**，不要误以为它们是入口点去改。真正的入口是同名的 `XxxFragment`。
- `utils/BackupManager.kt`、`utils/NaturalLanguageParser.kt`：空占位类。备份功能的真正实现在 `data/repository/BackupRepository.kt`，不要在 `BackupManager` 里"顺手"实现备份逻辑，命名容易让人混淆但职责不在这里。
- `data/model/BackupData.kt`：定义了但**未被实际使用**（`BackupRepository` 用 Gson `JsonObject` 手工拼装 JSON，未使用这个 data class）。
- `data/imports/CsvBillParser.kt` 与 `data/imports/XlsxBillParser.kt` 对"中性交易"（转账/充值/提现类）的处理**不一致**：CSV 版本直接丢弃，XLSX 版本会保留为 `ImportBillType.NEUTRAL` 交给 `ImportPreviewFragment` 处理成转账。修复其中一个时请意识到另一个可能也需要同步。
- `ui/bills/ImportPreviewFragment.kt`（1920 行）承担了账户自动匹配、分类自动匹配、去重、转账识别等**几乎全部导入业务规则**，是全仓库最复杂、最容易牵一发动全身的文件；改动前建议先搜索相关方法（`findWechatAccount`/`findAlipayAccount`/`resolveTransferAccounts`/`isDuplicateTransfer`/`findCategoryId` 等）通读一遍再动手。
- `MainViewModel` 与各 Fragment 的数据不是自动同步的——多数 Fragment 各自 `FinanceRepository(requireContext())`，不经过 `MainViewModel`。如果你的改动涉及"改了数据但界面没刷新"，很可能是这个已知架构限制，不一定是 bug；参见文档第 4、11 节。

## 提交前检查清单

- [ ] 新增/修改的 `data.model` 字段是否都有默认值？
- [ ] 是否沿用了项目现有的"逐行换行"代码风格与中文注释习惯？
- [ ] 是否复用了 `AppLogger` 而不是裸 `Log`/`println`？
- [ ] 是否误改了未接入 Manifest 的空占位类，以为它是真正入口？
- [ ] 涉及导入解析规则的改动，CSV 与 XLSX 两条路径是否都同步处理？
- [ ] 是否为新增的纯逻辑代码补充了单元测试（`app/src/test/`）？
- [ ] 如果修改了 XML 布局的 id，是否同步更新了所有引用该 ViewBinding 属性的 Kotlin 代码？

## 更详细的参考

- 完整架构、数据流、全部 82 个类的职责说明、安全机制、已知技术债：见 [`JZAPP_项目完整开发文档.md`](./JZAPP_项目完整开发文档.md)。
- 变更记录：见 [`CHANGELOG.md`](./CHANGELOG.md)，请在完成有意义的修改后补充条目。
