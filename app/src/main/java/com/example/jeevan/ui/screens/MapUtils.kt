package com.example.jeevan.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Shared map marker utilities used by LiveTrackingScreen,
 * DriverUIScreen, and HospitalUIScreen.
 */

/**
 * Creates a circular emoji/text marker bitmap for OSMDroid.
 * @param context  Android context
 * @param emoji    Emoji or short text to draw in the circle
 * @param bgColor  ARGB background circle color
 */
fun createTextMarker(
    context: Context,
    emoji: String,
    bgColor: Int,
): android.graphics.drawable.Drawable {
    val size   = 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Circle background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, bgPaint)

    // White border ring
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = android.graphics.Color.WHITE
        style       = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, borderPaint)

    // Emoji / text
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 44f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(emoji, size / 2f, size / 2f + 16f, textPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
