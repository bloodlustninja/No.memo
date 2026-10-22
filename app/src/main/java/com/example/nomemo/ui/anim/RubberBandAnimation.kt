package com.example.nomemo.ui.anim

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object RubberBandAnimation {

    private val decelerateInterpolator = DecelerateInterpolator()
    private val overshootTouchInterpolator = OvershootInterpolator(2.5f)
    private val overshootClickInterpolator = OvershootInterpolator(2.8f)

    @SuppressLint("ClickableViewAccessibility")
    fun applyRubberBandTouchEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.92f)
                        .scaleY(0.92f)
                        .setDuration(80)
                        .setInterpolator(decelerateInterpolator)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(250)
                        .setInterpolator(overshootTouchInterpolator)
                        .start()
                }
            }
            false
        }
    }

    fun animateRubberBandClick(view: View, onAnimationEnd: () -> Unit = {}) {
        view.animate()
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(70)
            .setInterpolator(decelerateInterpolator)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(280)
                    .setInterpolator(overshootClickInterpolator)
                    .withEndAction {
                        onAnimationEnd()
                    }
                    .start()
            }
            .start()
    }
}