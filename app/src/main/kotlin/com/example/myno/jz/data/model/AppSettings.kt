package com.example.myno.jz.data.model

/**
 * 应用锁类型
 */
enum class LockType {
    NONE,
    PIN,
    PATTERN
}

/**
 * 主题模式
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * 默认记账类型
 */
enum class DefaultBillType {
    EXPENSE,
    INCOME
}

/**
 * 记账成功后的行为
 */
enum class AfterAddAction {
    HOME,
    BILL_LIST,
    CONTINUE
}

/**
 * 日期格式
 */
enum class DateFormatType {
    YMD,
    YMD_SLASH,
    MD
}

/**
 * 金额小数位
 */
enum class DecimalPlaces {
    TWO,
    ZERO
}

/**
 * 统计周期
 */
enum class StatisticsPeriod {
    NATURAL_MONTH,
    LAST_30_DAYS,
    NATURAL_YEAR
}

/**
 * 应用设置
 */
data class AppSettings(

    /**
     * 应用锁类型
     */
    val lockType: LockType = LockType.NONE,

    /**
     * PIN
     *
     * 后续会进一步改为更加安全的存储方式。
     */
    val pin: String? = null,

    /**
     * 图案锁数据
     */
    val pattern: String? = null,

    /**
     * 主题模式
     */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /**
     * 是否开启动画
     */
    val animationEnabled: Boolean = true,

    /**
     * 默认记账类型
     */
    val defaultBillType: DefaultBillType = DefaultBillType.EXPENSE,

    /**
     * 记账成功后的行为
     */
    val afterAddAction: AfterAddAction = AfterAddAction.HOME,

    /**
     * 日期格式
     */
    val dateFormat: DateFormatType = DateFormatType.YMD,

    /**
     * 金额小数位
     */
    val decimalPlaces: DecimalPlaces = DecimalPlaces.TWO,

    /**
     * 统计周期
     */
    val statisticsPeriod: StatisticsPeriod = StatisticsPeriod.NATURAL_MONTH,

    /**
     * 深色模式
     *
     * 保留旧字段，兼容之前版本数据。
     */
    val darkMode: Boolean = false,

    /**
     * 是否自动结转上月余额
     */
    val autoCarryBalance: Boolean = true,

    /**
     * 默认账户
     */
    val defaultAccountId: String? = null,

    /**
     * 默认分类
     */
    val defaultCategoryId: String? = null
)