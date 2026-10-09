package com.example.documentscanner

sealed class Screen(val route: String) {
    object DocumentScanner : Screen("document_scanner")
    object Translation : Screen("translation")
    object SelfieSegmentation : Screen("selfie_segmentation")
}
