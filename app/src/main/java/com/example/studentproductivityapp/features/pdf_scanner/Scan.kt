package com.example.studentproductivityapp.features.pdf_scanner

import android.net.Uri

data class ScanPage(val picture: Uri)

object ScanSession {
    val pages = mutableListOf<ScanPage>()
}