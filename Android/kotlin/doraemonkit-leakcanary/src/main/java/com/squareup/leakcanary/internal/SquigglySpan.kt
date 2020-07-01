/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.leakcanary.internal

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.Path
import android.text.SpannableStringBuilder
import android.text.style.ReplacementSpan
import android.text.style.UnderlineSpan
import com.squareup.leakcanary.R
import kotlin.math.sin

/**
 * Inspired from https://github.com/flavienlaurent/spans and
 * https://github.com/andyxialm/WavyLineView
 */
internal class SquigglySpan(resources: Resources) : ReplacementSpan() {
    private val squigglyPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path: Path
    private val referenceColor: Int
    private val halfStrokeWidth: Float
    private val amplitude: Float
    private val halfWaveHeight: Float
    private val periodDegrees: Float
    private var width = 0
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int,
                         fm: FontMetricsInt?): Int {
        width = paint.measureText(text, start, end).toInt()
        return width
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int,
                      y: Int, bottom: Int, paint: Paint) {
        squigglyHorizontalPath(path,
                x + halfStrokeWidth,
                x + width - halfStrokeWidth,
                bottom - halfWaveHeight,
                amplitude, periodDegrees)
        canvas.drawPath(path, squigglyPaint)
        paint.color = referenceColor
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }

    companion object {
        @JvmStatic
        fun replaceUnderlineSpans(builder: SpannableStringBuilder, resources: Resources) {
            val underlineSpans = builder.getSpans(0, builder.length, UnderlineSpan::class.java)
            for (span in underlineSpans) {
                val start = builder.getSpanStart(span)
                val end = builder.getSpanEnd(span)
                builder.removeSpan(span)
                builder.setSpan(SquigglySpan(resources), start, end, 0)
            }
        }

        private fun squigglyHorizontalPath(path: Path, left: Float, right: Float, centerY: Float,
                                           amplitude: Float,
                                           periodDegrees: Float) {
            path.reset()
            var y: Float
            path.moveTo(left, centerY)
            val period = (2 * Math.PI / periodDegrees).toFloat()
            var x = 0f
            while (x <= right - left) {
                y = (amplitude * sin(40 + period * x.toDouble()) + centerY).toFloat()
                path.lineTo(left + x, y)
                x += 1f
            }
        }
    }

    init {
        squigglyPaint.style = Paint.Style.STROKE
        squigglyPaint.color = resources.getColor(R.color.leak_canary_leak)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.leak_canary_squiggly_span_stroke_width).toFloat()
        squigglyPaint.strokeWidth = strokeWidth
        halfStrokeWidth = strokeWidth / 2
        amplitude = resources.getDimensionPixelSize(R.dimen.leak_canary_squiggly_span_amplitude).toFloat()
        periodDegrees = resources.getDimensionPixelSize(R.dimen.leak_canary_squiggly_span_period_degrees).toFloat()
        path = Path()
        val waveHeight = 2 * amplitude + strokeWidth
        halfWaveHeight = waveHeight / 2
        referenceColor = resources.getColor(R.color.leak_canary_reference)
    }
}