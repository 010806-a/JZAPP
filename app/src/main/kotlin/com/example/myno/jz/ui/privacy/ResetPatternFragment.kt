package com.example.myno.jz.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myno.jz.data.repository.PrivacyLockStore
import com.example.myno.jz.databinding.FragmentResetPatternBinding

class ResetPatternFragment : Fragment() {

    private var _binding: FragmentResetPatternBinding? = null
    private val binding get() = _binding!!

    private lateinit var lockStore: PrivacyLockStore

    private var firstPattern: String? = null
    private var waitingForSecondPattern = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentResetPatternBinding.inflate(
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
                        handlePattern(pattern)
                    }
                }
            )
    }

    private fun handlePattern(
        pattern: String
    ) {

        val pointCount =
            pattern.split("-").size

        if (pointCount < 4) {

            binding.tvStatus.text =
                "图案至少需要连接4个点"

            binding.patternLockView
                .resetPattern()

            return
        }

        if (!waitingForSecondPattern) {

            firstPattern = pattern
            waitingForSecondPattern = true

            binding.tvStatus.text =
                "图案已记录，请再次绘制相同图案确认"

            binding.patternLockView
                .resetPattern()

            return
        }

        if (pattern != firstPattern) {

            firstPattern = null
            waitingForSecondPattern = false

            binding.tvStatus.text =
                "两次图案不一致，请重新设置"

            binding.patternLockView
                .resetPattern()

            return
        }

        savePattern(pattern)
    }

    private fun savePattern(
        pattern: String
    ) {

        val success =
            lockStore.savePattern(pattern)

        if (!success) {

            Toast.makeText(
                requireContext(),
                "图案重置失败",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "图案重置成功",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager
            .popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}