package com.example.myno.jz.ui.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myno.jz.databinding.FragmentMineBinding
import com.example.myno.jz.data.repository.EmailConfigStore
import com.example.myno.jz.ui.assets.AssetsFragment
import com.example.myno.jz.ui.backup.BackupFragment
import com.example.myno.jz.ui.budget.BudgetManageFragment
import com.example.myno.jz.ui.category.CategoryManageFragment
import com.example.myno.jz.ui.common.openScreen
import com.example.myno.jz.ui.email.EmailConfigFragment
import com.example.myno.jz.ui.email.EmailVerificationFragment
import com.example.myno.jz.ui.privacy.ChangePasswordFragment
import com.example.myno.jz.ui.privacy.ChangePatternFragment
import com.example.myno.jz.ui.privacy.ChangeSecurityFragment
import com.example.myno.jz.ui.privacy.PrivacyLockFragment
import com.example.myno.jz.ui.settings.SettingsFragment
import com.example.myno.jz.ui.statistics.StatisticsFragment

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVersion()
        setupEmailStatus()
        setupClickEvents()
        observeEmailVerificationResult()
    }

    private fun observeEmailVerificationResult() {
        parentFragmentManager.setFragmentResultListener(
            "email_verification_result",
            viewLifecycleOwner
        ) { _, result ->
            if (!result.getBoolean("verified", false)) return@setFragmentResultListener

            when (result.getString("action")) {
                EmailVerificationFragment.ACTION_PRIVACY_LOCK -> openPrivacyLock()
                EmailVerificationFragment.ACTION_CHANGE_PASSWORD -> openChangePassword()
                EmailVerificationFragment.ACTION_CHANGE_PATTERN -> openChangePattern()
            }
        }
    }

    private fun setupVersion() {
        binding.tvVersion.text = "MoneyBook · 本地记账"
    }

    private fun setupEmailStatus() {
        val config = EmailConfigStore(requireContext()).getConfig()

        binding.tvEmailStatus.text = when {
            config.verified && config.email.isNotBlank() ->
                "${config.email} · 已验证"

            config.configured && config.email.isNotBlank() ->
                "${config.email} · 待验证"

            else ->
                "用于账户安全验证"
        }
    }

    private fun setupClickEvents() = with(binding) {
        cardProfile.setOnClickListener {
            showComingSoon("个人信息")
        }

        itemExport.setOnClickListener {
            showComingSoon("导出账单")
        }

        itemBackup.setOnClickListener {
            parentFragmentManager.openScreen(BackupFragment())
        }

        // 数据统计：直接进入现有的完整统计页面
        itemStatistics.setOnClickListener {
            parentFragmentManager.openScreen(StatisticsFragment())
        }

        itemPrivacyLock.setOnClickListener {
            openEmailVerification(
                EmailVerificationFragment.ACTION_PRIVACY_LOCK
            )
        }

        itemChangePassword.setOnClickListener {
            openChangeSecurity()
        }

        itemEmailConfig.setOnClickListener {
            openEmailConfig()
        }

        itemCategory.setOnClickListener {
            parentFragmentManager.openScreen(CategoryManageFragment())
        }

        itemBudget.setOnClickListener {
            parentFragmentManager.openScreen(BudgetManageFragment())
        }

        itemAccount.setOnClickListener {
            parentFragmentManager.openScreen(AssetsFragment())
        }

        itemSettings.setOnClickListener {
            parentFragmentManager.openScreen(SettingsFragment())
        }

        itemAbout.setOnClickListener {
            showAbout()
        }

        itemFeedback.setOnClickListener {
            showComingSoon("意见反馈")
        }
    }

    private fun openChangeSecurity() {
        parentFragmentManager.openScreen(ChangeSecurityFragment())
    }

    private fun openEmailVerification(action: String) {
        parentFragmentManager.openScreen(
            EmailVerificationFragment.newInstance(action)
        )
    }

    private fun openEmailConfig() {
        parentFragmentManager.openScreen(EmailConfigFragment())
    }

    private fun openPrivacyLock() {
        parentFragmentManager.openScreen(PrivacyLockFragment())
    }

    private fun openChangePassword() {
        parentFragmentManager.openScreen(ChangePasswordFragment())
    }

    private fun openChangePattern() {
        parentFragmentManager.openScreen(ChangePatternFragment())
    }

    private fun showAbout() {
        AlertDialog.Builder(requireContext())
            .setTitle("关于 MoneyBook")
            .setMessage(
                "MoneyBook\n\n" +
                    "一款完全离线的个人记账应用。\n\n" +
                    "所有账单、账户、分类和设置数据均保存在本机。\n\n" +
                    "不依赖网络即可使用。"
            )
            .setPositiveButton("知道了", null)
            .show()
    }

    /** 保留尚未接入的入口，避免误删未完成能力。 */
    private fun showComingSoon(name: String) {
        Toast.makeText(
            requireContext(),
            "${name}功能即将接入",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}