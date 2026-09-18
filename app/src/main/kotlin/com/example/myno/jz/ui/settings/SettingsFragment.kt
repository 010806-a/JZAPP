package com.example.myno.jz.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.model.AfterAddAction
import com.example.myno.jz.data.model.AppSettings
import com.example.myno.jz.data.model.Category
import com.example.myno.jz.data.model.CategoryType
import com.example.myno.jz.data.model.DateFormatType
import com.example.myno.jz.data.model.DecimalPlaces
import com.example.myno.jz.data.model.DefaultBillType
import com.example.myno.jz.data.model.StatisticsPeriod
import com.example.myno.jz.data.model.ThemeMode
import com.example.myno.jz.data.repository.FinanceRepository
import com.example.myno.jz.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: FinanceRepository

    private var settings: AppSettings = AppSettings()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        repository = FinanceRepository(requireContext())

        loadSettings()
        setupButtons()
        updateViews()
    }

    /**
     * 读取设置
     */
    private fun loadSettings() {
        settings = repository.getSettings()
    }

    /**
     * 设置所有点击事件
     */
    private fun setupButtons() {

        // 返回
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 主题模式
        binding.itemTheme.setOnClickListener {
            showThemeDialog()
        }

        // 动画效果
        binding.itemAnimation.setOnClickListener {
            showAnimationDialog()
        }

        // 默认记账类型
        binding.itemDefaultType.setOnClickListener {
            showDefaultTypeDialog()
        }

        // 记账成功后
        binding.itemAfterAdd.setOnClickListener {
            showAfterAddDialog()
        }

        // 默认账户
        binding.itemDefaultAccount.setOnClickListener {
            showDefaultAccountDialog()
        }

        // 默认分类
        binding.itemDefaultCategory.setOnClickListener {
            showDefaultCategoryDialog()
        }

        // 日期格式
        binding.itemDateFormat.setOnClickListener {
            showDateFormatDialog()
        }

        // 金额小数位
        binding.itemDecimal.setOnClickListener {
            showDecimalDialog()
        }

        // 统计周期
        binding.itemStatisticsPeriod.setOnClickListener {
            showStatisticsPeriodDialog()
        }
    }

    /**
     * 更新界面显示
     */
    private fun updateViews() {

        // 主题模式
        binding.tvThemeValue.text = when (settings.themeMode) {
            ThemeMode.SYSTEM -> "跟随系统"
            ThemeMode.LIGHT -> "浅色模式"
            ThemeMode.DARK -> "深色模式"
        }

        // 动画效果
        binding.tvAnimationValue.text =
            if (settings.animationEnabled) {
                "开启"
            } else {
                "关闭"
            }

        // 默认记账类型
        binding.tvDefaultType.text = when (settings.defaultBillType) {
            DefaultBillType.EXPENSE -> "支出"
            DefaultBillType.INCOME -> "收入"
        }

        // 记账成功后
        binding.tvAfterAdd.text = when (settings.afterAddAction) {
            AfterAddAction.HOME -> "返回首页"
            AfterAddAction.BILL_LIST -> "进入账单"
            AfterAddAction.CONTINUE -> "继续记账"
        }

        // 默认账户
        val defaultAccountId = settings.defaultAccountId

        val defaultAccountName = if (defaultAccountId.isNullOrBlank()) {
            "未设置"
        } else {
            repository.getAccounts()
                .firstOrNull { it.id == defaultAccountId }
                ?.name
                ?: "未设置"
        }

        binding.tvDefaultAccount.text = defaultAccountName

        // 默认分类
        val defaultCategoryId = settings.defaultCategoryId

        val defaultCategoryName = if (defaultCategoryId.isNullOrBlank()) {
            "未设置"
        } else {
            repository.getCategories()
                .firstOrNull { it.id == defaultCategoryId }
                ?.name
                ?: "未设置"
        }

        binding.tvDefaultCategory.text = defaultCategoryName

        // 日期格式
        binding.tvDateFormat.text = when (settings.dateFormat) {
            DateFormatType.YMD -> "2026-09-13"
            DateFormatType.YMD_SLASH -> "2026/09/13"
            DateFormatType.MD -> "09-13"
        }

        // 金额小数位
        binding.tvDecimal.text = when (settings.decimalPlaces) {
            DecimalPlaces.TWO -> "2位"
            DecimalPlaces.ZERO -> "0位"
        }

        // 统计周期
        binding.tvStatisticsPeriod.text = when (settings.statisticsPeriod) {
            StatisticsPeriod.NATURAL_MONTH -> "自然月"
            StatisticsPeriod.LAST_30_DAYS -> "最近30天"
            StatisticsPeriod.NATURAL_YEAR -> "自然年"
        }
    }

    /**
     * 默认账户选择
     */
    private fun showDefaultAccountDialog() {

        val accounts = repository.getAccounts()
            .filter { it.enabled }
            .sortedBy { it.sortOrder }

        val items = mutableListOf<String>()
        val accountIds = mutableListOf<String>()

        // 第一项：不设置
        items.add("不设置")
        accountIds.add("")

        // 添加账户
        accounts.forEach { account ->
            items.add(account.name)
            accountIds.add(account.id)
        }

        // 当前选中的账户
        val checkedItem = if (settings.defaultAccountId.isNullOrBlank()) {
            0
        } else {
            val index = accountIds.indexOf(settings.defaultAccountId)

            if (index >= 0) {
                index
            } else {
                0
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("默认账户")
            .setSingleChoiceItems(
                items.toTypedArray(),
                checkedItem
            ) { dialog, which ->

                val accountId = accountIds[which]

                saveSettings(
                    settings.copy(
                        defaultAccountId = accountId.ifBlank {
                            null
                        }
                    )
                )

                dialog.dismiss()
            }
            .show()
    }

    /**
     * 默认分类选择
     */
    private fun showDefaultCategoryDialog() {

        // 支出分类在前，收入分类在后，各自按 sortOrder 排序
        val categories = repository.getCategories()
            .filter { it.visible }
            .sortedWith(
                compareBy<Category> {
                    if (it.type == CategoryType.EXPENSE) 0 else 1
                }.thenBy {
                    it.sortOrder
                }
            )

        val items = mutableListOf<String>()
        val categoryIds = mutableListOf<String>()

        // 第一项：不设置
        items.add("不设置")
        categoryIds.add("")

        // 添加分类，名称后标注收/支类型
        categories.forEach { category ->

            val typeName =
                if (category.type == CategoryType.EXPENSE) {
                    "支出"
                } else {
                    "收入"
                }

            items.add("${category.name}（${typeName}）")
            categoryIds.add(category.id)
        }

        // 当前选中的分类
        val checkedItem = if (settings.defaultCategoryId.isNullOrBlank()) {
            0
        } else {
            val index = categoryIds.indexOf(settings.defaultCategoryId)

            if (index >= 0) {
                index
            } else {
                0
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("默认分类")
            .setSingleChoiceItems(
                items.toTypedArray(),
                checkedItem
            ) { dialog, which ->

                val categoryId = categoryIds[which]

                saveSettings(
                    settings.copy(
                        defaultCategoryId = categoryId.ifBlank {
                            null
                        }
                    )
                )

                dialog.dismiss()
            }
            .show()
    }

    /**
     * 保存设置
     */
    private fun saveSettings(newSettings: AppSettings) {

        val success = repository.saveSettings(newSettings)

        if (!success) {
            return
        }

        val themeChanged =
            settings.themeMode != newSettings.themeMode

        settings = newSettings

        updateViews()

        if (themeChanged) {
            applyTheme(newSettings.themeMode)
        }
    }

    /**
     * 立即应用主题
     */
    private fun applyTheme(themeMode: ThemeMode) {

        val nightMode = when (themeMode) {

            ThemeMode.SYSTEM ->
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

            ThemeMode.LIGHT ->
                AppCompatDelegate.MODE_NIGHT_NO

            ThemeMode.DARK ->
                AppCompatDelegate.MODE_NIGHT_YES
        }

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {

            AppCompatDelegate.setDefaultNightMode(nightMode)

            requireActivity().recreate()
        }
    }

    /**
     * 主题模式
     */
    private fun showThemeDialog() {
        showChoiceDialog(
            title = "主题模式",
            items = listOf("跟随系统", "浅色模式", "深色模式"),
            checkedItem = when (settings.themeMode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            }
        ) { which ->
            val mode = when (which) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            saveSettings(settings.copy(
                themeMode = mode,
                darkMode = mode == ThemeMode.DARK
            ))
        }
    }

    /**
     * 动画效果
     */
    private fun showAnimationDialog() {
        showChoiceDialog(
            title = "动画效果",
            items = listOf("开启", "关闭"),
            checkedItem = if (settings.animationEnabled) 0 else 1
        ) { which ->
            val enabled = which == 0
            saveSettings(settings.copy(animationEnabled = enabled))
            applyAnimationSetting(enabled)
        }
    }

    /**
     * 应用动画设置
     */
    private fun applyAnimationSetting(enabled: Boolean) {

        val window = requireActivity().window

        if (enabled) {
            window.setWindowAnimations(
                android.R.style.Animation_Activity
            )
        } else {
            window.setWindowAnimations(0)
        }
    }

    /**
     * 默认记账类型
     */
    private fun showDefaultTypeDialog() {
        showChoiceDialog(
            title = "默认记账类型",
            items = listOf("支出", "收入"),
            checkedItem = if (settings.defaultBillType == DefaultBillType.EXPENSE) 0 else 1
        ) { which ->
            saveSettings(settings.copy(
                defaultBillType = if (which == 1) DefaultBillType.INCOME else DefaultBillType.EXPENSE
            ))
        }
    }

    /**
     * 记账成功后的行为
     */
    private fun showAfterAddDialog() {
        showChoiceDialog(
            title = "记账成功后",
            items = listOf("返回首页", "进入账单", "继续记账"),
            checkedItem = when (settings.afterAddAction) {
                AfterAddAction.HOME -> 0
                AfterAddAction.BILL_LIST -> 1
                AfterAddAction.CONTINUE -> 2
            }
        ) { which ->
            saveSettings(settings.copy(
                afterAddAction = when (which) {
                    1 -> AfterAddAction.BILL_LIST
                    2 -> AfterAddAction.CONTINUE
                    else -> AfterAddAction.HOME
                }
            ))
        }
    }

    /**
     * 日期格式
     */
    private fun showDateFormatDialog() {
        showChoiceDialog(
            title = "日期格式",
            items = listOf("2026-09-13", "2026/09/13", "09-13"),
            checkedItem = when (settings.dateFormat) {
                DateFormatType.YMD -> 0
                DateFormatType.YMD_SLASH -> 1
                DateFormatType.MD -> 2
            }
        ) { which ->
            saveSettings(settings.copy(
                dateFormat = when (which) {
                    1 -> DateFormatType.YMD_SLASH
                    2 -> DateFormatType.MD
                    else -> DateFormatType.YMD
                }
            ))
        }
    }

    /**
     * 金额小数位
     */
    private fun showDecimalDialog() {
        showChoiceDialog(
            title = "金额小数位",
            items = listOf("2位", "0位"),
            checkedItem = if (settings.decimalPlaces == DecimalPlaces.TWO) 0 else 1
        ) { which ->
            saveSettings(settings.copy(
                decimalPlaces = if (which == 1) DecimalPlaces.ZERO else DecimalPlaces.TWO
            ))
        }
    }

    /**
     * 统计周期
     */
    private fun showStatisticsPeriodDialog() {
        showChoiceDialog(
            title = "统计周期",
            items = listOf("自然月", "最近30天", "自然年"),
            checkedItem = when (settings.statisticsPeriod) {
                StatisticsPeriod.NATURAL_MONTH -> 0
                StatisticsPeriod.LAST_30_DAYS -> 1
                StatisticsPeriod.NATURAL_YEAR -> 2
            }
        ) { which ->
            saveSettings(settings.copy(
                statisticsPeriod = when (which) {
                    1 -> StatisticsPeriod.LAST_30_DAYS
                    2 -> StatisticsPeriod.NATURAL_YEAR
                    else -> StatisticsPeriod.NATURAL_MONTH
                }
            ))
        }
    }

    /**
     * 统一设置页单选弹窗，避免每个设置项重复创建 AlertDialog。
     */
    private fun showChoiceDialog(
        title: String,
        items: List<String>,
        checkedItem: Int,
        onSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), checkedItem) { dialog, which ->
                onSelected(which)
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}