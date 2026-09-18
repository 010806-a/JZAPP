package com.example.myno.jz.ui.home

data class QuickAction(
    val id: String,
    val title: String,
    val iconRes: Int,
    val order: Int,
    val visible: Boolean = true
)