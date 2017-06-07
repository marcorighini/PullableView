package com.marcorighini.pullableview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.databinding.DataBindingUtil
import com.marcorighini.lib.BoundView
import com.marcorighini.lib.anim.AlphaTransformation
import com.marcorighini.lib.anim.TranslateTransformation
import com.marcorighini.pullableview.databinding.ActivitySampleBinding


class SampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivitySampleBinding>(this, R.layout.activity_sample)
        binding.pullableView.boundViews = mutableListOf(
                BoundView(binding.pullableView, listOf(AlphaTransformation(), TranslateTransformation())),
                BoundView(binding.alpheable, listOf(AlphaTransformation())),
                BoundView(binding.transleable, listOf(TranslateTransformation()))
        )
    }
}
