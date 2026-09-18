package com.example.myno.jz.data.model

enum class EmailProvider(
    val displayName: String
) {
    QQ(
        "QQ邮箱"
    ),

    NETEASE_163(
        "163邮箱"
    ),

    NETEASE_126(
        "126邮箱"
    ),

    SINA(
        "新浪邮箱"
    ),

    GMAIL(
        "Gmail"
    ),

    OUTLOOK(
        "Outlook / Hotmail"
    ),

    CUSTOM(
        "自定义邮箱"
    )
}