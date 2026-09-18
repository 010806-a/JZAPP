package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myno.jz.R
import com.example.myno.jz.data.model.PrivacyLockType
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.FragmentPrivacyLockVerifyBinding
import com.example.myno.jz.ui.email.EmailVerificationFragment

class PrivacyLockVerifyFragment : Fragment() {

    private var _binding: FragmentPrivacyLockVerifyBinding? = null
    private val binding get() = _binding!!

    private lateinit var lockStore: PrivacyLockStore

    private var unlocked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPrivacyLockVerifyBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        lockStore =
            PrivacyLockStore(requireContext())

        setup()

        /*
         * 接收邮箱验证码验证结果
         *
         * 用于：
         * 忘记密码 → 重置密码
         * 忘记图案 → 重置图案
         */
        parentFragmentManager.setFragmentResultListener(
            "email_verification_result",
            viewLifecycleOwner
        ) { _, result ->

            val verified =
                result.getBoolean(
                    "verified",
                    false
                )

            if (!verified) {
                return@setFragmentResultListener
            }

            when (
                result.getString("action")
            ) {

                EmailVerificationFragment
                    .ACTION_FORGOT_PASSWORD -> {

                    openResetPassword()
                }

                EmailVerificationFragment
                    .ACTION_FORGOT_PATTERN -> {

                    openResetPattern()
                }
            }
        }
    }

    /**
     * 初始化验证界面
     */
    private fun setup() {

        val config =
            lockStore.getConfig()

        /*
         * 没有启用隐私锁
         */
        if (!config.enabled) {

            onUnlockSuccess()

            return
        }

        /*
         * 根据当前隐私锁类型
         * 显示对应验证方式
         */
        when (config.type) {

            PrivacyLockType.PASSWORD -> {

                showPasswordMode()
            }

            PrivacyLockType.PATTERN -> {

                showPatternMode()
            }

            PrivacyLockType.NONE -> {

                onUnlockSuccess()
            }
        }
    }

    /**
     * 密码验证模式
     */
    private fun showPasswordMode() {

        binding.tvTitle.text =
            "输入隐私锁密码"

        binding.tvDescription.text =
            "请输入密码后继续使用 MoneyBook"

        binding.passwordLayout.visibility =
            View.VISIBLE

        binding.patternLockView.visibility =
            View.GONE

        binding.tvPatternStatus.visibility =
            View.GONE

        binding.btnVerify.visibility =
            View.VISIBLE

        /*
         * 密码锁显示：
         * 忘记密码？
         */
        binding.tvForgot.visibility =
            View.VISIBLE

        binding.tvForgot.text =
            "忘记密码？"

        binding.btnVerify.setOnClickListener {

            verifyPassword()
        }

        binding.tvForgot.setOnClickListener {

            openForgotVerification()
        }
    }

    /**
     * 图案验证模式
     */
    private fun showPatternMode() {

        binding.tvTitle.text =
            "绘制解锁图案"

        binding.tvDescription.text =
            "请绘制正确的图案后继续使用 MoneyBook"

        binding.passwordLayout.visibility =
            View.GONE

        binding.btnVerify.visibility =
            View.GONE

        binding.patternLockView.visibility =
            View.VISIBLE

        binding.tvPatternStatus.visibility =
            View.VISIBLE

        /*
         * 图案锁显示：
         * 忘记图案？
         */
        binding.tvForgot.visibility =
            View.VISIBLE

        binding.tvForgot.text =
            "忘记图案？"

        binding.tvPatternStatus.text =
            "请绘制图案"

        binding.tvForgot.setOnClickListener {

            openForgotVerification()
        }

        binding.patternLockView
            .setOnPatternCompleteListener(
                object :
                    PatternLockView
                        .OnPatternCompleteListener {

                    override fun onPatternComplete(
                        pattern: String
                    ) {

                        verifyPattern(pattern)
                    }
                }
            )
    }

    /**
     * 打开忘记密码 / 忘记图案的邮箱验证
     */
    private fun openForgotVerification() {

        val config =
            lockStore.getConfig()

        val action =
            when (config.type) {

                PrivacyLockType.PASSWORD -> {

                    EmailVerificationFragment
                        .ACTION_FORGOT_PASSWORD
                }

                PrivacyLockType.PATTERN -> {

                    EmailVerificationFragment
                        .ACTION_FORGOT_PATTERN
                }

                PrivacyLockType.NONE -> {

                    Toast.makeText(
                        requireContext(),
                        "当前没有启用隐私锁",
                        Toast.LENGTH_SHORT
                    ).show()

                    return
                }
            }

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                EmailVerificationFragment
                    .newInstance(action)
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * 验证密码
     */
    private fun verifyPassword() {

        val password =
            binding.etPassword
                .text
                ?.toString()
                .orEmpty()

        if (password.isBlank()) {

            binding.etPassword.error =
                "请输入密码"

            return
        }

        val success =
            lockStore.verify(password)

        if (success) {

            onUnlockSuccess()

        } else {

            binding.etPassword.error =
                "密码错误"

            binding.etPassword.text?.clear()

            Toast.makeText(
                requireContext(),
                "密码错误，请重新输入",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 验证图案
     */
    private fun verifyPattern(
        pattern: String
    ) {

        val success =
            lockStore.verify(pattern)

        if (success) {

            onUnlockSuccess()

        } else {

            binding.tvPatternStatus.text =
                "图案错误，请重新绘制"

            binding.patternLockView
                .resetPattern()

            Toast.makeText(
                requireContext(),
                "图案错误",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 打开重置密码页面
     */
    private fun openResetPassword() {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                ResetPasswordFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * 打开重置图案页面
     */
    private fun openResetPattern() {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                ResetPatternFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * 验证成功
     *
     * 这里不再直接打开 HomeFragment。
     * MainActivity 会根据进入隐私锁之前的页面恢复界面。
     */
    private fun onUnlockSuccess() {

        if (unlocked) {
            return
        }

        unlocked = true

        /*
         * 通知 MainActivity：
         * 隐私锁已经验证成功
         */
        parentFragmentManager.setFragmentResult(
            "privacy_lock_verify_result",
            Bundle().apply {

                putBoolean(
                    "verified",
                    true
                )
            }
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}