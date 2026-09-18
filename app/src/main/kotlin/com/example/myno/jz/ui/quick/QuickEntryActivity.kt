package com.example.myno.jz.ui.quick

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myno.jz.R
import com.example.myno.jz.databinding.ActivityQuickEntryBinding
import com.example.myno.jz.ui.bills.AddBillFragment

class QuickEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityQuickEntryBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        if (savedInstanceState == null) {

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.quickEntryContainer,
                    AddBillFragment.newQuickEntryInstance()
                )
                .commit()
        }
    }

    override fun onBackPressed() {

        finish()
    }
}