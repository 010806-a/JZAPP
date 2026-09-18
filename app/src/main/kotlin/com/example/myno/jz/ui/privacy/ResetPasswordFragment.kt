package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.FragmentResetPasswordBinding

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var lockStore: PrivacyLockStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentResetPasswordBinding.inflate(
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

        lockStore =
            PrivacyLockStore(requireContext())

        setup()
    }

    private fun setup() {

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {

        PasswordSetupHelper.save(
            context = requireContext(),
            lockStore = lockStore,
            passwordInput = binding.etPassword,
            confirmInput = binding.etPasswordConfirm,
            successMessage = "密码重置成功",
            failureMessage = "密码重置失败"
        ) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}