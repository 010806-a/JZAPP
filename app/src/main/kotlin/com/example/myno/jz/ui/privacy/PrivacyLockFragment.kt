package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myno.jz.R
import com.example.myno.jz.data.model.PrivacyLockType
import com.example.myno.jz.data.repository.EmailConfigStore
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.FragmentPrivacyLockBinding
import com.example.myno.jz.ui.email.EmailVerificationFragment

class PrivacyLockFragment : Fragment() {

    private var _binding: FragmentPrivacyLockBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var lockStore: PrivacyLockStore
    private lateinit var emailConfigStore: EmailConfigStore

    private var firstPattern: String? = null
    private var waitingForSecondPattern = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPrivacyLockBinding.inflate(
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
            PrivacyLockStore(
                requireContext()
            )

        emailConfigStore =
            EmailConfigStore(
                requireContext()
            )

        setup()

        /*
         * 接收邮箱验证码验证结果
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

            val action =
                result.getString("action")

            if (
                action ==
                EmailVerificationFragment.ACTION_PRIVACY_LOCK
            ) {

                disableLockAfterEmailVerification()
            }
        }
    }

    private fun setup() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }

        binding.radioPassword.setOnCheckedChangeListener {
                _,
                checked ->

            if (checked) {
                showPasswordMode()
            }
        }

        binding.radioPattern.setOnCheckedChangeListener {
                _,
                checked ->

            if (checked) {
                showPatternMode()
            }
        }

        binding.btnSave.setOnClickListener {
            save()
        }

        binding.patternLockView
            .setOnPatternCompleteListener(
                object :
                    PatternLockView.OnPatternCompleteListener {

                    override fun onPatternComplete(
                        pattern: String
                    ) {
                        handlePattern(pattern)
                    }
                }
            )

        /*
         * 关闭隐私锁
         */
        binding.btnDisable.setOnClickListener {
            disableLock()
        }

        val config =
            lockStore.getConfig()

        if (config.enabled) {

            binding.tvStatus.text =
                when (config.type) {

                    PrivacyLockType.PASSWORD ->
                        "当前已启用：密码锁"

                    PrivacyLockType.PATTERN ->
                        "当前已启用：图案锁"

                    PrivacyLockType.NONE ->
                        "当前未启用"
                }

            binding.btnDisable.visibility =
                View.VISIBLE

        } else {

            binding.tvStatus.text =
                "当前未启用隐私锁"

            binding.btnDisable.visibility =
                View.GONE

            binding.radioPassword.isChecked =
                true
        }
    }

    private fun handlePattern(
        pattern: String
    ) {

        if (!waitingForSecondPattern) {

            firstPattern = pattern

            waitingForSecondPattern = true

            binding.tvPatternStatus.text =
                "图案已记录，请再次绘制相同图案确认"

            binding.patternLockView.resetPattern()

            return
        }

        if (pattern != firstPattern) {

            binding.tvPatternStatus.text =
                "两次图案不一致，请重新设置"

            firstPattern = null

            waitingForSecondPattern = false

            binding.patternLockView.resetPattern()

            return
        }

        savePattern(pattern)
    }

    private fun savePattern(
        pattern: String
    ) {

        if (pattern.split("-").size < 4) {

            binding.tvPatternStatus.text =
                "图案至少需要连接4个点"

            binding.patternLockView.resetPattern()

            return
        }

        val success =
            lockStore.savePattern(
                pattern
            )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "图案锁保存失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "图案锁已启用",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager
            .popBackStack()
    }

    private fun showPasswordMode() {

        binding.passwordLayout.visibility =
            View.VISIBLE

        binding.passwordConfirmLayout.visibility =
            View.VISIBLE

        binding.tvPatternTip.visibility =
            View.GONE

        binding.patternLockView.visibility =
            View.GONE

        binding.tvPatternStatus.visibility =
            View.GONE
    }

    private fun showPatternMode() {

        binding.passwordLayout.visibility =
            View.GONE

        binding.passwordConfirmLayout.visibility =
            View.GONE

        binding.tvPatternTip.visibility =
            View.VISIBLE

        binding.patternLockView.visibility =
            View.VISIBLE

        binding.tvPatternStatus.visibility =
            View.VISIBLE

        binding.tvPatternStatus.text =
            "请绘制图案"
    }

    private fun save() {

        if (binding.radioPassword.isChecked) {

            savePassword()

        } else if (binding.radioPattern.isChecked) {

            if (firstPattern == null) {

                Toast.makeText(
                    requireContext(),
                    "请先绘制并确认图案",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            Toast.makeText(
                requireContext(),
                "请按照提示完成图案确认",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun savePassword() {

        val password =
            binding.etPassword.text
                ?.toString()
                .orEmpty()

        val confirm =
            binding.etPasswordConfirm.text
                ?.toString()
                .orEmpty()

        if (password.length < 6) {

            binding.etPassword.error =
                "密码至少6位"

            return
        }

        if (password != confirm) {

            binding.etPasswordConfirm.error =
                "两次输入的密码不一致"

            return
        }

        val success =
            lockStore.savePassword(
                password
            )

        if (!success) {

            Toast.makeText(
                requireContext(),
                "隐私锁保存失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "密码锁已启用",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager
            .popBackStack()
    }

    /**
     * 点击关闭隐私锁
     *
     * 已配置并验证邮箱：
     *      → 进入邮箱验证码
     *
     * 未配置邮箱：
     *      → 不允许关闭
     */
    private fun disableLock() {

        val emailConfig =
            emailConfigStore.getConfig()

        if (
            emailConfig.email.isBlank() ||
            !emailConfig.configured ||
            !emailConfig.verified
        ) {

            Toast.makeText(
                requireContext(),
                "请先配置并验证邮箱后再关闭隐私锁",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        /*
         * 已经配置并验证邮箱，
         * 进入邮箱验证码验证。
         */
        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                EmailVerificationFragment.newInstance(
                    EmailVerificationFragment.ACTION_PRIVACY_LOCK
                )
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * 邮箱验证成功后真正关闭隐私锁
     */
    private fun disableLockAfterEmailVerification() {

        val success =
            lockStore.disable()

        if (success) {

            Toast.makeText(
                requireContext(),
                "邮箱验证成功，隐私锁已关闭",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager
                .popBackStack()

        } else {

            Toast.makeText(
                requireContext(),
                "关闭隐私锁失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}