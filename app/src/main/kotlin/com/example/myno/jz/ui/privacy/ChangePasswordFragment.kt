package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var lockStore: PrivacyLockStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentChangePasswordBinding.inflate(
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
            savePassword()
        }
    }

    private fun savePassword() {

        val password =
            binding.etPassword
                .text
                ?.toString()
                .orEmpty()

        val confirmPassword =
            binding.etPasswordConfirm
                .text
                ?.toString()
                .orEmpty()

        if (password.length < 6) {

            binding.etPassword.error =
                "密码至少需要6位"

            return
        }

        if (password != confirmPassword) {

            binding.etPasswordConfirm.error =
                "两次输入的密码不一致"

            return
        }

        val success =
            lockStore.savePassword(password)

        if (!success) {

            Toast.makeText(
                requireContext(),
                "密码保存失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "密码修改成功",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}