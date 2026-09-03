package com.example.prathibhascanfinal

import android.view.View

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
    var lastClickTime: Long = 0
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 500L) {
            lastClickTime = currentTime
            onSafeClick(it)
        }
    }
}

fun View.applySystemBarsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = systemBars.left,
            top = systemBars.top,
            right = systemBars.right,
            bottom = systemBars.bottom
        )
        insets
    }
}

fun View.applyTopInsetPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = systemBars.top)
        insets
    }
}

fun View.applyBottomInsetPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(bottom = systemBars.bottom)
        insets
    }
}

fun View.fadeIn(duration: Long = 500) {
    this.alpha = 0f
    this.visibility = View.VISIBLE
    this.animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
}
