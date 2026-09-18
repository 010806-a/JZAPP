package com.example.myno.jz.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

import com.example.myno.jz.R
import com.example.myno.jz.data.local.DefaultDataInitializer
import com.example.myno.jz.data.model.ThemeMode
import com.example.myno.jz.data.repository.FinanceRepository
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.ActivityMainBinding
import com.example.myno.jz.ui.assets.AssetsFragment
import com.example.myno.jz.ui.bills.BillsFragment
import com.example.myno.jz.ui.home.HomeFragment
import com.example.myno.jz.ui.mine.MineFragment
import com.example.myno.jz.ui.privacy.PrivacyLockVerifyFragment
import com.example.myno.jz.ui.statistics.StatisticsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    /**
     * 隐私锁
     */
    private lateinit var privacyLockStore: PrivacyLockStore

    /**
     * 当前是否正在进行隐私锁验证
     */
    private var privacyLockChecking = false

    /**
     * 当前 Activity 生命周期内是否已经验证成功
     */
    private var privacyUnlocked = false

    /**
     * 进入隐私锁验证前的页面
     */
    private var pageBeforePrivacyLock = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {

        // 应用主题
        applyTheme()

        super.onCreate(savedInstanceState)

        // 初始化隐私锁
        privacyLockStore = PrivacyLockStore(this)

        /**
         * 接收 PrivacyLockVerifyFragment
         * 返回的验证结果
         */
        supportFragmentManager.setFragmentResultListener(
            "privacy_lock_verify_result",
            this
        ) { _, result ->

            val verified =
                result.getBoolean(
                    "verified",
                    false
                )

            if (verified) {

                // 验证成功
                privacyUnlocked = true

                // 允许下一次检查
                privacyLockChecking = false

                // 恢复进入隐私锁前的页面
                restorePageBeforePrivacyLock()
            }
        }

        // 动画设置
        applyAnimationSetting()

        // 初始化默认数据
        DefaultDataInitializer.initialize(this)

        // 初始化 ViewBinding
        binding =
            ActivityMainBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        // 刷新主页面数据
        viewModel.refresh()

        // 初始化底部导航
        setupBottomNavigation()

        /**
         * 第一次进入 Activity 时显示首页
         */
        if (savedInstanceState == null) {

            showFragment(
                HomeFragment()
            )

            binding.bottomNavigation.selectedItemId =
                R.id.nav_home
        }
    }

    /**
     * Activity 恢复到前台时检查隐私锁
     */
    override fun onResume() {

        super.onResume()

        checkPrivacyLock()
    }

    /**
     * 检查是否需要显示隐私锁验证页面
     */
    private fun checkPrivacyLock() {

        val config =
            privacyLockStore.getConfig()

        // 没有启用隐私锁
        if (!config.enabled) {

            privacyUnlocked = true

            return
        }

        // 当前已经验证成功
        if (privacyUnlocked) {
            return
        }

        // 已经正在验证
        if (privacyLockChecking) {
            return
        }

        /**
         * 记录进入隐私锁之前的页面
         */
        pageBeforePrivacyLock =
            binding.bottomNavigation.selectedItemId
                .takeIf {
                    it != 0
                }
                ?: R.id.nav_home

        privacyLockChecking = true

        /**
         * 打开隐私锁验证页面
         *
         * 注意：
         * 这里不加入返回栈，
         * 防止用户按返回键绕过隐私锁。
         */
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                PrivacyLockVerifyFragment()
            )
            .commit()
    }

    /**
     * 恢复进入隐私锁之前的页面
     */
    private fun restorePageBeforePrivacyLock() {

        when (pageBeforePrivacyLock) {

            R.id.nav_home -> {

                showFragment(
                    HomeFragment()
                )
            }

            R.id.nav_bills -> {

                showFragment(
                    BillsFragment()
                )
            }

            R.id.nav_statistics -> {

                showFragment(
                    StatisticsFragment()
                )
            }

            R.id.nav_assets -> {

                showFragment(
                    AssetsFragment()
                )
            }

            R.id.nav_settings -> {

                showFragment(
                    MineFragment()
                )
            }

            else -> {

                pageBeforePrivacyLock =
                    R.id.nav_home

                showFragment(
                    HomeFragment()
                )
            }
        }

        binding.bottomNavigation.selectedItemId =
            pageBeforePrivacyLock
    }

    /**
     * 根据 settings.json 应用主题
     */
    private fun applyTheme() {

        val repository =
            FinanceRepository(this)

        val settings =
            repository.getSettings()

        when (settings.themeMode) {

            ThemeMode.SYSTEM -> {

                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }

            ThemeMode.LIGHT -> {

                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
            }

            ThemeMode.DARK -> {

                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
            }
        }
    }

    /**
     * 应用页面动画设置
     */
    private fun applyAnimationSetting() {

        val repository =
            FinanceRepository(this)

        val settings =
            repository.getSettings()

        if (settings.animationEnabled) {

            window.setWindowAnimations(
                android.R.style.Animation_Activity
            )

        } else {

            window.setWindowAnimations(0)
        }
    }

    /**
     * 初始化底部导航
     */
    private fun setupBottomNavigation() {

        binding.bottomNavigation.setOnItemSelectedListener { item ->

            // 隐私锁验证过程中禁止切换页面
            if (privacyLockChecking) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {

                R.id.nav_home -> {

                    showFragment(
                        HomeFragment()
                    )

                    true
                }

                R.id.nav_bills -> {

                    showFragment(
                        BillsFragment()
                    )

                    true
                }

                R.id.nav_statistics -> {

                    showFragment(
                        StatisticsFragment()
                    )

                    true
                }

                R.id.nav_assets -> {

                    showFragment(
                        AssetsFragment()
                    )

                    true
                }

                R.id.nav_settings -> {

                    showFragment(
                        MineFragment()
                    )

                    true
                }

                else -> false
            }
        }
    }

    /**
     * 显示 Fragment
     */
    private fun showFragment(
        fragment: Fragment
    ) {

        // 隐私锁验证过程中禁止切换页面
        if (privacyLockChecking) {
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()
    }
}