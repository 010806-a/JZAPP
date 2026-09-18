package com.example.myno.jz.ui.email

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myno.jz.R
import com.example.myno.jz.data.email.EmailConnectionTester
import com.example.myno.jz.data.model.EmailConfig
import com.example.myno.jz.data.model.EmailProvider
import com.example.myno.jz.data.repository.EmailConfigStore
import com.example.myno.jz.data.repository.EmailCredentialStore
import com.example.myno.jz.databinding.FragmentEmailConfigBinding
import java.util.concurrent.Executors

class EmailConfigFragment : Fragment() {

    private var _binding: FragmentEmailConfigBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var configStore: EmailConfigStore
    private lateinit var credentialStore: EmailCredentialStore

    private val executor =
        Executors.newSingleThreadExecutor()

    private val providers =
        EmailProvider.entries.toList()

    /**
     * 当前页面本次是否完成了
     * IMAP + SMTP 双重测试
     */
    private var connectionTestPassed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEmailConfigBinding.inflate(
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

        configStore =
            EmailConfigStore(requireContext())

        credentialStore =
            EmailCredentialStore(requireContext())

        /*
         * 接收旧邮箱验证结果。
         *
         * 已经配置邮箱时：
         * 点击修改配置
         * ↓
         * 验证旧邮箱
         * ↓
         * 验证成功
         * ↓
         * 进入邮箱编辑模式
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
                EmailVerificationFragment.ACTION_CHANGE_EMAIL
            ) {

                enterEditModeAfterVerification()
            }
        }

        setupProviderSpinner()
        setupButtons()
        loadConfig()
    }

    /**
     * 旧邮箱验证成功后进入编辑模式
     */
    private fun enterEditModeAfterVerification() {

        val config =
            configStore.getConfig()

        /*
         * 验证成功后重新加载原配置，
         * 防止编辑页面为空。
         */
        showEditMode(config)

        binding.tvStatus.text =
            "旧邮箱验证成功，请修改邮箱配置"
    }

    // =========================================================
    // 邮箱服务商
    // =========================================================

    private fun setupProviderSpinner() {

        val names =
            providers.map {
                it.displayName
            }

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                names
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerProvider.adapter =
            adapter

        binding.spinnerProvider.setOnItemSelectedListener(
            object :
                android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    /*
                     * 只有编辑模式才允许改变服务器。
                     */
                    if (
                        binding.layoutEditMode.visibility ==
                        View.VISIBLE
                    ) {

                        applyProviderPreset(
                            providers[position]
                        )

                        connectionTestPassed = false
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }
        )
    }

    private fun applyProviderPreset(
        provider: EmailProvider
    ) {

        when (provider) {

            EmailProvider.QQ -> {

                binding.etImapHost.setText(
                    "imap.qq.com"
                )

                binding.etImapPort.setText(
                    "993"
                )

                binding.switchImapSsl.isChecked =
                    true

                binding.etSmtpHost.setText(
                    "smtp.qq.com"
                )

                binding.etSmtpPort.setText(
                    "465"
                )

                binding.switchSmtpSsl.isChecked =
                    true
            }

            EmailProvider.NETEASE_163 -> {

                binding.etImapHost.setText(
                    "imap.163.com"
                )

                binding.etImapPort.setText(
                    "993"
                )

                binding.switchImapSsl.isChecked =
                    true

                binding.etSmtpHost.setText(
                    "smtp.163.com"
                )

                binding.etSmtpPort.setText(
                    "465"
                )

                binding.switchSmtpSsl.isChecked =
                    true
            }

            EmailProvider.NETEASE_126 -> {

                binding.etImapHost.setText(
                    "imap.126.com"
                )

                binding.etImapPort.setText(
                    "993"
                )

                binding.switchImapSsl.isChecked =
                    true

                binding.etSmtpHost.setText(
                    "smtp.126.com"
                )

                binding.etSmtpPort.setText(
                    "465"
                )

                binding.switchSmtpSsl.isChecked =
                    true
            }

            EmailProvider.SINA -> {

                binding.etImapHost.setText(
                    "imap.sina.com"
                )

                binding.etImapPort.setText(
                    "993"
                )

                binding.switchImapSsl.isChecked =
                    true

                binding.etSmtpHost.setText(
                    "smtp.sina.com"
                )

                binding.etSmtpPort.setText(
                    "465"
                )

                binding.switchSmtpSsl.isChecked =
                    true
            }

            EmailProvider.GMAIL -> {

                binding.etImapHost.setText(
                    "imap.gmail.com"
                )

                binding.etImapPort.setText(
                    "993"
                )

                binding.switchImapSsl.isChecked =
                    true

                binding.etSmtpHost.setText(
                    "smtp.gmail.com"
                )

                binding.etSmtpPort.setText(
                    "465"
                )

                binding.switchSmtpSsl.isChecked =
                    true
            }

            EmailProvider.OUTLOOK -> {

                binding.etImapHost.setText(
                    "outlook.office365.com"
                )

                binding.etImapPort.setText(
                    "993"
                )

                binding.switchImapSsl.isChecked =
                    true

                binding.etSmtpHost.setText(
                    "smtp.office365.com"
                )

                binding.etSmtpPort.setText(
                    "587"
                )

                binding.switchSmtpSsl.isChecked =
                    false
            }

            EmailProvider.CUSTOM -> {
                // 自定义邮箱不修改服务器
            }
        }
    }

    // =========================================================
    // 加载配置
    // =========================================================

    private fun loadConfig() {

        val config =
            configStore.getConfig()

        if (
            config.configured &&
            config.verified &&
            config.email.isNotBlank()
        ) {

            showViewMode(config)

        } else {

            showEditMode(config)
        }
    }

    // =========================================================
    // 查看模式
    // =========================================================

    private fun showViewMode(
        config: EmailConfig
    ) {

        binding.layoutViewMode.visibility =
            View.VISIBLE

        binding.layoutEditMode.visibility =
            View.GONE

        binding.tvViewStatus.text =
            "✓ 已验证"

        binding.tvViewEmail.text =
            config.email

        binding.tvViewProvider.text =
            getProviderDisplayName(
                config.provider
            )

        binding.tvViewImap.text =
            buildServerText(
                config.imapHost,
                config.imapPort,
                config.imapSsl
            )

        binding.tvViewSmtp.text =
            buildServerText(
                config.smtpHost,
                config.smtpPort,
                config.smtpSsl
            )

        binding.tvViewCredential.text =
            "已安全保存 · 不显示"
    }

    private fun getProviderDisplayName(
        providerName: String
    ): String {

        return providers
            .firstOrNull {
                it.name == providerName
            }
            ?.displayName
            ?: providerName
    }

    private fun buildServerText(
        host: String,
        port: Int,
        ssl: Boolean
    ): String {

        val mode =
            if (ssl) {
                "SSL/TLS"
            } else {
                "STARTTLS"
            }

        return "$host:$port · $mode"
    }

    // =========================================================
    // 编辑模式
    // =========================================================

    private fun showEditMode(
        config: EmailConfig
    ) {

        binding.layoutViewMode.visibility =
            View.GONE

        binding.layoutEditMode.visibility =
            View.VISIBLE

        /*
         * 如果有旧配置，
         * 把旧配置填入编辑框。
         */
        if (config.email.isNotBlank()) {

            binding.etEmail.setText(
                config.email
            )

            binding.etImapHost.setText(
                config.imapHost
            )

            binding.etImapPort.setText(
                config.imapPort.toString()
            )

            binding.switchImapSsl.isChecked =
                config.imapSsl

            binding.etSmtpHost.setText(
                config.smtpHost
            )

            binding.etSmtpPort.setText(
                config.smtpPort.toString()
            )

            binding.switchSmtpSsl.isChecked =
                config.smtpSsl

            val index =
                providers.indexOfFirst {
                    it.name == config.provider
                }

            if (index >= 0) {

                binding.spinnerProvider.setSelection(
                    index
                )
            }

            binding.tvStatus.text =
                if (config.configured) {
                    "修改邮箱配置后，需要重新测试 IMAP + SMTP"
                } else {
                    "尚未配置邮箱"
                }

        } else {

            binding.tvStatus.text =
                "请配置邮箱并测试 IMAP + SMTP 连接"
        }

        /*
         * 进入编辑模式后必须重新测试。
         */
        connectionTestPassed = false
    }

    // =========================================================
    // 按钮
    // =========================================================

    private fun setupButtons() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        /*
         * 已经配置邮箱：
         *
         * 修改前必须验证旧邮箱。
         *
         * 没有配置邮箱：
         * 直接进入编辑模式。
         */
        binding.btnEditConfig.setOnClickListener {

            val config =
                configStore.getConfig()

            if (
                config.email.isBlank() ||
                !config.configured ||
                !config.verified
            ) {

                enterEditModeAfterVerification()

                return@setOnClickListener
            }

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    EmailVerificationFragment
                        .newInstance(
                            EmailVerificationFragment
                                .ACTION_CHANGE_EMAIL
                        )
                )
                .addToBackStack(null)
                .commit()
        }

        binding.btnTestConnection.setOnClickListener {

            testConnection()
        }

        binding.btnSave.setOnClickListener {

            saveConfig()
        }
    }

    // =========================================================
    // 收集配置
    // =========================================================

    private fun collectConfig(): EmailConfig? {

        val email =
            binding.etEmail.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val imapHost =
            binding.etImapHost.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val smtpHost =
            binding.etSmtpHost.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val imapPort =
            binding.etImapPort.text
                ?.toString()
                ?.toIntOrNull()

        val smtpPort =
            binding.etSmtpPort.text
                ?.toString()
                ?.toIntOrNull()

        if (
            email.isBlank() ||
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            binding.etEmail.error =
                "请输入正确的邮箱地址"

            return null
        }

        if (imapHost.isBlank()) {

            binding.etImapHost.error =
                "请输入 IMAP 服务器"

            return null
        }

        if (
            imapPort == null ||
            imapPort !in 1..65535
        ) {

            binding.etImapPort.error =
                "请输入正确端口"

            return null
        }

        if (smtpHost.isBlank()) {

            binding.etSmtpHost.error =
                "请输入 SMTP 服务器"

            return null
        }

        if (
            smtpPort == null ||
            smtpPort !in 1..65535
        ) {

            binding.etSmtpPort.error =
                "请输入正确端口"

            return null
        }

        val position =
            binding.spinnerProvider.selectedItemPosition

        if (
            position < 0 ||
            position >= providers.size
        ) {

            Toast.makeText(
                requireContext(),
                "请选择邮箱服务商",
                Toast.LENGTH_SHORT
            ).show()

            return null
        }

        val provider =
            providers[position]

        return EmailConfig(
            email = email,
            provider = provider.name,
            imapHost = imapHost,
            imapPort = imapPort,
            imapSsl =
                binding.switchImapSsl.isChecked,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpSsl =
                binding.switchSmtpSsl.isChecked,
            configured = false,
            verified = false,
            updatedAt =
                System.currentTimeMillis()
        )
    }

    // =========================================================
    // 测试 IMAP + SMTP
    // =========================================================

    private fun testConnection() {

        val config =
            collectConfig()
                ?: return

        val authCode =
            binding.etAuthCode.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (authCode.isBlank()) {

            binding.etAuthCode.error =
                "请输入邮箱授权码"

            return
        }

        connectionTestPassed = false

        setLoading(true)

        executor.execute {

            val smtpResult =
                EmailConnectionTester.testSmtp(
                    config,
                    authCode
                )

            if (smtpResult.isFailure) {

                showResult(
                    "SMTP连接失败：${
                        smtpResult.exceptionOrNull()
                            ?.message ?: "未知错误"
                    }"
                )

                return@execute
            }

            val imapResult =
                EmailConnectionTester.testImap(
                    config,
                    authCode
                )

            if (imapResult.isFailure) {

                showResult(
                    "SMTP连接成功，但 IMAP连接失败：${
                        imapResult.exceptionOrNull()
                            ?.message ?: "未知错误"
                    }"
                )

                return@execute
            }

            connectionTestPassed = true

            showResult(
                "IMAP + SMTP 连接测试成功，请点击“保存邮箱配置”"
            )
        }
    }

    // =========================================================
    // 保存配置
    // =========================================================

    private fun saveConfig() {

        if (!connectionTestPassed) {

            Toast.makeText(
                requireContext(),
                "请先测试 IMAP + SMTP 连接",
                Toast.LENGTH_LONG
            ).show()

            binding.tvStatus.text =
                "请先测试 IMAP + SMTP 连接，测试成功后才能保存"

            return
        }

        val config =
            collectConfig()
                ?: return

        val authCode =
            binding.etAuthCode.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (authCode.isBlank()) {

            binding.etAuthCode.error =
                "请输入邮箱授权码"

            connectionTestPassed = false

            return
        }

        setLoading(true)

        executor.execute {

            /*
             * 保存前再次复核 SMTP。
             */
            val smtpResult =
                EmailConnectionTester.testSmtp(
                    config,
                    authCode
                )

            if (smtpResult.isFailure) {

                connectionTestPassed = false

                showResult(
                    "保存前 SMTP复核失败：${
                        smtpResult.exceptionOrNull()
                            ?.message ?: "未知错误"
                    }"
                )

                return@execute
            }

            /*
             * 保存前再次复核 IMAP。
             */
            val imapResult =
                EmailConnectionTester.testImap(
                    config,
                    authCode
                )

            if (imapResult.isFailure) {

                connectionTestPassed = false

                showResult(
                    "保存前 IMAP复核失败：${
                        imapResult.exceptionOrNull()
                            ?.message ?: "未知错误"
                    }"
                )

                return@execute
            }

            /*
             * 加密保存授权码。
             */
            val credentialSaved =
                credentialStore.saveAuthCode(
                    authCode
                )

            if (!credentialSaved) {

                showResult(
                    "邮箱授权码保存失败"
                )

                return@execute
            }

            val savedConfig =
                config.copy(
                    configured = true,
                    verified = true,
                    updatedAt =
                        System.currentTimeMillis()
                )

            val configSaved =
                configStore.saveConfig(
                    savedConfig
                )

            if (!configSaved) {

                showResult(
                    "邮箱配置保存失败"
                )

                return@execute
            }

            connectionTestPassed = true

            requireActivity().runOnUiThread {

                setLoading(false)

                Toast.makeText(
                    requireContext(),
                    "邮箱配置保存成功",
                    Toast.LENGTH_SHORT
                ).show()

                showViewMode(
                    savedConfig
                )
            }
        }
    }

    // =========================================================
    // Loading
    // =========================================================

    private fun setLoading(
        loading: Boolean
    ) {

        requireActivity().runOnUiThread {

            binding.btnTestConnection.isEnabled =
                !loading

            binding.btnSave.isEnabled =
                !loading

            binding.btnEditConfig.isEnabled =
                !loading

            if (loading) {

                binding.tvStatus.text =
                    "正在测试邮箱连接，请稍候……"
            }
        }
    }

    private fun showResult(
        message: String
    ) {

        requireActivity().runOnUiThread {

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