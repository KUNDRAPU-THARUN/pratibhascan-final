package com.example.prathibhascanfinal.ui.base

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

abstract class BaseActivity : AppCompatActivity() {
    abstract val viewModel: BaseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    /**
     * Standardized helper to apply system bar paddings to a view.
     * Prevents content from overlapping with status bar, navigation bar, and cutouts.
     */
    protected fun applySystemBarPadding(
        view: View,
        top: Boolean = true,
        bottom: Boolean = true,
        left: Boolean = true,
        right: Boolean = true
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            
            v.updatePadding(
                left = if (left) systemBars.left + displayCutout.left else v.paddingLeft,
                top = if (top) systemBars.top else v.paddingTop,
                right = if (right) systemBars.right + displayCutout.right else v.paddingRight,
                bottom = if (bottom) systemBars.bottom else v.paddingBottom
            )
            insets
        }
    }

    /**
     * Standardized edge-to-edge setup for the root view of an activity.
     */
    protected fun setupEdgeToEdge(rootView: View) {
        enableEdgeToEdge()
        applySystemBarPadding(rootView)
    }
    
    /**
     * Applies only bottom navigation insets, useful for scrollable containers
     * that should reach the bottom of the screen but not hide behind the nav bar.
     */
    protected fun applyBottomInset(view: View, extraPadding: Int = 0) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBars.bottom + extraPadding)
            insets
        }
    }
}
