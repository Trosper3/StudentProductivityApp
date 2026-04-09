package com.example.studentproductivityapp.features.pdf_scanner

import android.graphics.Rect
import android.net.Uri

data class TextLine(val text: String, val boundingBox: Rect)

data class ScanPage(
    val picture: Uri,
    val textLines: List<TextLine> = emptyList()
)

object ScanSession {
    val pages = mutableListOf<ScanPage>()
}
