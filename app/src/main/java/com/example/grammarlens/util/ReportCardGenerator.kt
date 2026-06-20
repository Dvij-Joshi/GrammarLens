package com.example.grammarlens.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.grammarlens.ui.dashboard.CategoryDetail
import com.example.grammarlens.ui.dashboard.DailyTrend
import java.io.File
import java.io.FileOutputStream

object ReportCardGenerator {
    fun generateAndShare(context: Context, trendData: List<DailyTrend>, categoryBreakdown: List<CategoryDetail>) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(Color.parseColor("#FFFBF5")) // Pastel cream

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Title
        textPaint.color = Color.parseColor("#4A403A")
        textPaint.textSize = 100f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("GrammarLens Report", width / 2f, 250f, textPaint)

        // Subtitle
        textPaint.textSize = 50f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#A1948B")
        canvas.drawText("Weekly Stats", width / 2f, 350f, textPaint)

        // Stats boxes
        paint.color = Color.parseColor("#E6F2FF") // CardBlue
        val rectF = RectF(100f, 450f, width - 100f, 750f)
        canvas.drawRoundRect(rectF, 60f, 60f, paint)

        val totalMistakes = trendData.sumOf { it.mistakes }
        val totalChecks = trendData.sumOf { it.checks }

        textPaint.color = Color.parseColor("#4A403A")
        textPaint.textSize = 80f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totalChecks Sentences Checked", width / 2f, 580f, textPaint)
        
        textPaint.textSize = 60f
        textPaint.color = Color.parseColor("#FF6B6B")
        canvas.drawText("$totalMistakes Mistakes Fixed", width / 2f, 680f, textPaint)

        // Categories
        textPaint.color = Color.parseColor("#4A403A")
        textPaint.textSize = 70f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Top Weaknesses:", 120f, 900f, textPaint)

        var yPos = 1050f
        textPaint.textSize = 55f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        categoryBreakdown.take(4).forEach { cat ->
            paint.color = Color.parseColor("#FFE6ED") // CardPink
            canvas.drawRoundRect(RectF(120f, yPos - 70f, width - 120f, yPos + 30f), 30f, 30f, paint)
            
            canvas.drawText("${cat.category}", 160f, yPos, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${cat.count} times", width - 160f, yPos, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            yPos += 140f
        }

        // Branding
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 40f
        textPaint.color = Color.parseColor("#A1948B")
        canvas.drawText("Made with GrammarLens", width / 2f, height - 150f, textPaint)

        // Save and share
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val imagePath = File(cachePath, "report_card.png")
            val fos = FileOutputStream(imagePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imagePath)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Check out my GrammarLens progress!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Report Card").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
