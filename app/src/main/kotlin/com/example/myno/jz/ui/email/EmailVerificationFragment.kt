package com.example.myno.jz.ui.email

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.email.EmailConnectionTester
import com.example.myno.jz.data.repository.EmailConfigStore
import com.example.myno.jz.data.repository.EmailCredentialStore
import com.example.myno.jz.data.repository.EmailVerificationStore
import com.example.myno.jz.data.repository.VerificationResult
import com.example.myno.jz.databinding.FragmentEmailVerificationBinding
import java.util.concurrent.Executors
import kotlin.random.Random

class EmailVerificationFragment : Fragment() {

    companion object {

        const val ARG_ACTION = "verification_action"

        const val ACTION_PRIVACY_LOCK =
            "privacy_lock"

        const val ACTION_CHANGE_PASSWORD =
            "change_password"

        const val ACTION_CHANGE_PATTERN =
            "change_pattern"

        const val ACTION_FORGOT_PASSWORD =
            "forgot_password"

        const val ACTION_FORGOT_PATTERN =
            "forgot_pattern"

        const val ACTION_CHANGE_EMAIL = "change_email"
        fun newInstance(
            action: String
        ): EmailVerificationFragment {

            return EmailVerificationFragment().apply {

                arguments =
                    Bundle().apply {
                        putString(
                            ARG_ACTION,
                            action
                        )
                    }
            }
        }
    }

    private var _binding: FragmentEmailVerificationBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var configStore: EmailConfigStore

    private lateinit var credentialStore:
        EmailCredentialStore

    private lateinit var verificationStore:
        EmailVerificationStore

    private val executor =
        Executors.newSingleThreadExecutor()

    private var action: String =
        ACTION_PRIVACY_LOCK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEmailVerificationBinding.inflate(
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

        action =
            arguments?.getString(
                ARG_ACTION
            ) ?: ACTION_PRIVACY_LOCK

        configStore =
            EmailConfigStore(
                requireContext()
            )

        credentialStore =
            EmailCredentialStore(
                requireContext()
            )

        verificationStore =
            EmailVerificationStore(
                requireContext()
            )

        setup()
    }

    private fun setup() {

        val config =
            configStore.getConfig()

        if (
            !config.configured ||
            !config.verified ||
            config.email.isBlank()
        ) {

            Toast.makeText(
                requireContext(),
                "请先完成安全邮箱配置",
                Toast.LENGTH_LONG
            ).show()

            parentFragmentManager
                .popBackStack()

            return
        }

        binding.tvEmail.text =
            "验证码将发送到：${config.email}"

        binding.btnBack.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }

        binding.btnSendCode.setOnClickListener {
            sendCode()
        }

        binding.btnVerify.setOnClickListener {
            verifyCode()
        }

        updateCountdown()
    }

    private fun sendCode() {

        val config =
            configStore.getConfig()

        if (
            !config.configured ||
            !config.verified
        ) {

            showStatus(
                "安全邮箱尚未完成验证"
            )

            return
        }

        if (
            !verificationStore.canResend()
        ) {

            val seconds =
                verificationStore
                    .getRemainingSeconds()

            showStatus(
                "${seconds} 秒后可以重新发送验证码"
            )

            return
        }

        val authCode =
            credentialStore.getAuthCode()

        if (authCode.isBlank()) {

            showStatus(
                "邮箱授权码不存在，请重新配置邮箱"
            )

            return
        }

        val verificationCode =
            Random.nextInt(
                100000,
                1000000
            ).toString()

        setLoading(true)

        executor.execute {

            val result =
                EmailConnectionTester
                    .sendVerificationCode(
                        config,
                        authCode,
                        verificationCode
                    )

            if (result.isFailure) {

                showResult(
                    "验证码发送失败：${
                        result.exceptionOrNull()
                            ?.message
                            ?: "未知错误"
                    }"
                )

                return@execute
            }

            verificationStore.saveCode(
                config.email,
                verificationCode
            )

            requireActivity()
                .runOnUiThread {

                    setLoading(false)

                    binding.tvStatus.text =
                        "验证码已发送，请检查邮箱"

                    Toast.makeText(
                        requireContext(),
                        "验证码已发送",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateCountdown()
                }
        }
    }

    private fun verifyCode() {

        val config =
            configStore.getConfig()

        val code =
            binding.etCode.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (code.length != 6) {

            binding.etCode.error =
                "请输入6位验证码"

            return
        }

        val result =
            verificationStore.verify(
                config.email,
                code
            )

        when (result) {

            VerificationResult.SUCCESS -> {

                Toast.makeText(
                    requireContext(),
                    "邮箱验证成功",
                    Toast.LENGTH_SHORT
                ).show()

                onVerificationSuccess()
            }

            VerificationResult.EMAIL_MISMATCH -> {

                showStatus(
                    "验证邮箱不匹配"
                )
            }

            VerificationResult.EXPIRED -> {

                showStatus(
                    "验证码已过期，请重新发送"
                )
            }

            VerificationResult.INVALID_CODE -> {

                showStatus(
                    "验证码错误，请重新输入"
                )
            }

            VerificationResult.TOO_MANY_ATTEMPTS -> {

                showStatus(
                    "验证码错误次数过多，请重新发送验证码"
                )
            }
        }
    }

    private fun onVerificationSuccess() {

        /*
         * 当前阶段：
         * 验证成功后返回上一页。
         *
         * 下一阶段会根据 action
         * 进入对应的密码/图案设置页面。
         */

        parentFragmentManager
            .setFragmentResult(
                "email_verification_result",
                Bundle().apply {

                    putBoolean(
                        "verified",
                        true
                    )

                    putString(
                        "action",
                        action
                    )
                }
            )

        parentFragmentManager
            .popBackStack()
    }

    private fun updateCountdown() {

        val seconds =
            verificationStore
                .getRemainingSeconds()

        if (seconds > 0) {

            binding.btnSendCode.isEnabled =
                false

            binding.tvCountdown.text =
                "${seconds} 秒后可以重新发送验证码"

        } else {

            binding.btnSendCode.isEnabled =
                true

            binding.tvCountdown.text =
                ""
        }
    }

    private fun setLoading(
        loading: Boolean
    ) {

        requireActivity()
            .runOnUiThread {

                binding.btnSendCode.isEnabled =
                    !loading

                binding.btnVerify.isEnabled =
                    !loading
            }
    }

    private fun showStatus(
        message: String
    ) {

        binding.tvStatus.text =
            message
    }

    private fun showResult(
        message: String
    ) {

        requireActivity()
            .runOnUiThread {

                setLoading(false)

                binding.tvStatus.text =
                    message

                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {

        executor.shutdownNow()

        super.onDestroyView()

        _binding = null
    }
}