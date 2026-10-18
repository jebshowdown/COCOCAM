package com.group_a1.isocam

import android.R
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

object FeedbackHelper {
    fun show(activity: Activity, message: String) {
        val root = activity.findViewById<ViewGroup>(R.id.content)
        val snackbar = TextView(activity).apply {
            text = message
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#E6333333"))
            }
            setPadding(40, 24, 40, 24)
            gravity = Gravity.CENTER
            elevation = 10f
            alpha = 0f
            translationY = 50f
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 150
        }
        root.addView(snackbar, params)
        snackbar.animate().translationY(0f).alpha(1f).setDuration(250).withEndAction {
            snackbar.postDelayed({
                snackbar.animate().translationY(50f).alpha(0f).setDuration(250).withEndAction {
                    root.removeView(snackbar)
                }.start()
            }, 2000)
        }.start()
    }
}