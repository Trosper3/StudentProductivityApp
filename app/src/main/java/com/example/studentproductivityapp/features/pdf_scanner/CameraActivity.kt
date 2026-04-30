package com.example.studentproductivityapp.features.pdf_scanner

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.assignments.AddAssignmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : androidx.activity.ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var textDetected: TextView

    private lateinit var btnSmartScan: Button
    private lateinit var btnReview: Button
    private lateinit var btnCreateAssignment: Button

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var noteParser: NoteParser

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.forEach { page ->
                val uri = page.imageUri
                processScannedPage(uri)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        textDetected = findViewById(R.id.textDetected)
        btnSmartScan = findViewById(R.id.btnSmartScan)
        btnReview = findViewById(R.id.btnReview)
        btnCreateAssignment = findViewById(R.id.btnCreateAssignment)

        cameraExecutor = Executors.newSingleThreadExecutor()
        noteParser = NoteParser(this)

        findViewById<Button>(R.id.btnReview).setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }

        btnCapture.setOnClickListener { takePhoto() }

        btnCreateAssignment.setOnClickListener {
            val intent = Intent(this, AddAssignmentActivity::class.java)
            // If we detected a date or title, pass them
            lastDetectedDate?.let {
                intent.putExtra("EXTRA_DUE_DATE", it)
            }
            lastDetectedTitle?.let {
                intent.putExtra("EXTRA_TITLE", it)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnSmartScan).setOnClickListener { startSmartScan() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraActivity", "Use case binding failed", e)
                Toast.makeText(this, "Camera start failed: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotBlank()) {
                        handleDetectedText(visionText.text)
                    } else {
                        runOnUiThread { textDetected.visibility = View.GONE }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CameraActivity", "Text recognition failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private var lastDetectedDate: Long? = null
    private var lastDetectedTitle: String? = null

    private fun handleDetectedText(text: String) {
        // Look for keywords
        val hasKeyword = text.contains("Assignment", ignoreCase = true) || 
                         text.contains("Due", ignoreCase = true) ||
                         text.contains("Exam", ignoreCase = true) ||
                         text.contains("Syllabus", ignoreCase = true)

        val detectedDate = noteParser.tryParseDate(text)
        if (detectedDate != null) {
            lastDetectedDate = detectedDate
        }

        val detectedTitle = noteParser.tryParseTitle(text)
        if (detectedTitle != null && detectedTitle.isNotBlank()) {
            lastDetectedTitle = detectedTitle
        }

        runOnUiThread {
            if (hasKeyword || detectedDate != null) {
                val message = if (detectedDate != null) {
                    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                    "Potential Assignment Detected!\nDue: ${sdf.format(java.util.Date(detectedDate))}"
                } else {
                    getString(R.string.ocr_keyword_detected)
                }
                textDetected.text = message
                textDetected.visibility = View.VISIBLE
                btnCreateAssignment.visibility = View.VISIBLE
            } else {
                textDetected.visibility = View.GONE
            }
        }
        Log.d("OCR", "Detected text: $text")
    }

    private fun startSmartScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setGalleryImportAllowed(true)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("CameraActivity", "Failed to start scanner", e)
                Toast.makeText(this, "Scanner failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun processScannedPage(uri: android.net.Uri) {
        noteParser.extractTextLinesFromImage(
            uri,
            onSuccess = { lines ->
                ScanSession.pages.add(ScanPage(uri, lines))
                runOnUiThread {
                    Toast.makeText(this, "Smart Scanned page added with OCR", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { e ->
                Log.e("CameraActivity", "OCR failed for smart scan", e)
                ScanSession.pages.add(ScanPage(uri))
                runOnUiThread {
                    Toast.makeText(this, "Smart Scanned page added (OCR failed)", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        // Save into Photos/Gallery (MediaStore)
        val name = "scan_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StudentScanner")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri

                    if (savedUri == null) {
                        Toast.makeText(this@CameraActivity, "Saved but URI was null", Toast.LENGTH_LONG).show()
                        return
                    }

                    // Perform OCR on the saved image for the final PDF
                    noteParser.extractTextLinesFromImage(
                        savedUri,
                        onSuccess = { lines ->
                            ScanSession.pages.add(ScanPage(savedUri, lines))
                            runOnUiThread {
                                Toast.makeText(
                                    this@CameraActivity,
                                    "Added page ${ScanSession.pages.size} with OCR",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onError = { e ->
                            Log.e("CameraActivity", "Final OCR failed", e)
                            ScanSession.pages.add(ScanPage(savedUri))
                            runOnUiThread {
                                Toast.makeText(
                                    this@CameraActivity,
                                    "Added page ${ScanSession.pages.size} (OCR failed)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
