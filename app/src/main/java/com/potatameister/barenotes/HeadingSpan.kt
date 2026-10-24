package com.potatameister.barenotes

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class HeadingSpan(val level: Int) : MetricAffectingSpan() {

    override fun updateDrawState(tp: TextPaint) = apply(tp)

    override fun updateMeasureState(paint: TextPaint) = apply(paint)

    private fun apply(paint: TextPaint) {
        paint.typeface = Typeface.create(paint.typeface ?: Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize *= if (level == 1) 1.5f else 1.25f
    }
}
