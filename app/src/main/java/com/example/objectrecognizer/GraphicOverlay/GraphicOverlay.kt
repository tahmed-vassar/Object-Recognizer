package com.example.objectrecognizer.GraphicOverlay

import android.graphics.*
import com.example.objectrecognizer.data.BoxWithText

class GraphicOverlay {

    //draw rectangle and text
    fun drawDetectionResults(bitmap: Bitmap, boxes: List<BoxWithText>): Bitmap {

        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val paintBox = Paint()
        val paintText = Paint()

        paintBox.textAlign = Paint.Align.LEFT
        paintBox.color = Color.CYAN
        paintBox.strokeWidth = 8F
        paintBox.style = Paint.Style.STROKE

        paintText.style = Paint.Style.FILL_AND_STROKE
        paintText.color = Color.CYAN
        paintText.strokeWidth = 2F


        for (box in boxes) {
            canvas.drawRect(box.boundingBox, paintBox)

            val textBound = Rect(0, 0, 0, 0)

            paintText.textSize = 96F
            paintText.getTextBounds(box.results, 0, box.results.length, textBound)
            val fontSize = paintText.textSize * box.boundingBox.width() / textBound.width()

            if (fontSize < paintText.textSize) paintText.textSize = fontSize

            var margin = (box.boundingBox.width() - textBound.width()) / 2.0F

            if (margin < 0F) margin = 0F
            canvas.drawText(
                box.results, box.boundingBox.left + margin,
                box.boundingBox.top + textBound.height().times(1F), paintText
            )

        }

        return outputBitmap
    }

}