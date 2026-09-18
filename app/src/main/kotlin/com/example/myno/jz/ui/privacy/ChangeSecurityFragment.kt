package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myno.jz.R
import com.example.myno.jz.databinding.FragmentChangeSecurityBinding
import com.example.myno.jz.ui.email.EmailVerificationFragment

class ChangeSecurityFragment : Fragment() {

    private var _binding: FragmentChangeSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentChangeSecurityBinding.inflate(
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

    setup()

    parentFragmentManager.setFragmentResultListener(
        "email_verification_result",
        viewLifecycleOwner
    ) { _, result ->

        val verified =
            result.getBoolean(
                "verified",
                false
            )

        val action =
            result.getString(
                "action"
            )

        if (!verified) {
            return@setFragmentResultListener
        }

        when (action) {

            EmailVerificationFragment.ACTION_CHANGE_PASSWORD -> {

                openChangePassword()
            }

            EmailVerificationFragment.ACTION_CHANGE_PATTERN -> {

                openChangePattern()
            }
        }
    }
}
private fun openChangePassword() {

    parentFragmentManager
        .beginTransaction()
        .replace(
            R.id.fragmentContainer,
            ChangePasswordFragment()
        )
        .addToBackStack(null)
        .commit()
}

private fun openChangePattern() {

    parentFragmentManager
        .beginTransaction()
        .replace(
            R.id.fragmentContainer,
            ChangePatternFragment()
        )
        .addToBackStack(null)
        .commit()
}
    private fun setup() {

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.itemChangePassword.setOnClickListener {

            openEmailVerification(
                EmailVerificationFragment.ACTION_CHANGE_PASSWORD
            )
        }

        binding.itemChangePattern.setOnClickListener {

            openEmailVerification(
                EmailVerificationFragment.ACTION_CHANGE_PATTERN
            )
        }
    }

    private fun openEmailVerification(
        action: String
    ) {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                EmailVerificationFragment.newInstance(
                    action
                )
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}