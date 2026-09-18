package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.FragmentChangePatternBinding

class ChangePatternFragment : Fragment() {

    private var _binding: FragmentChangePatternBinding? = null
    private val binding get() = _binding!!

    private lateinit var lockStore: PrivacyLockStore
    private lateinit var patternHelper: PatternSetupHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentChangePatternBinding.inflate(
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

        patternHelper =
            PatternSetupHelper(
                context = requireContext(),
                lockStore = lockStore,
                statusView = binding.tvStatus,
                resetPattern = {
                    binding.patternLockView.resetPattern()
                },
                successMessage = "图案修改成功",
                failureMessage = "图案修改失败"
            ) {
                parentFragmentManager.popBackStack()
            }

        setup()
    }

    private fun setup() {

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.patternLockView
            .setOnPatternCompleteListener(
                object :
                    PatternLockView.OnPatternCompleteListener {

                    override fun onPatternComplete(
                        pattern: String
                    ) {
                        patternHelper.handle(pattern)
                    }
                }
            )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}