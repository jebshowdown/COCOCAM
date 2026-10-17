package com.group_a1.isocam.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class CameraActivity : ComponentActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null

    // the in-memory queue: holds raw image byte arrays safely in RAM
    private val photoQueue = mutableListOf<ByteArray>()

    // container view for displaying the top-right thumbnail stack
    private lateinit var previewContainer: android.widget.LinearLayout

    // modern android way to handle runtime permissions safely
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            finish() // closes activity if user denies permission
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // root layout container holding camera preview, capture button, and queue preview
        val rootLayout = android.widget.FrameLayout(this)

        // set up camera preview view to fill the screen
        viewFinder = PreviewView(this)
        rootLayout.addView(viewFinder, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // set up top-right queue thumbnail container (pushed down to avoid notification bar)
        previewContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        val previewParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
            topMargin = 150 // increased top margin to clear status bar nicely
            rightMargin = 50
        }
        rootLayout.addView(previewContainer, previewParams)

        // snapchat-style circular ring drawable for capture button
        val ringDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke(10, android.graphics.Color.WHITE)
            alpha = 180
        }

        // create capture button and apply the ring background
        val captureButton = android.widget.Button(this).apply {
            text = ""
            background = ringDrawable
            setOnClickListener { takePhoto() }
        }

        // position button at bottom center
        val buttonParams = android.widget.FrameLayout.LayoutParams(200, 200).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            bottomMargin = 100
        }
        rootLayout.addView(captureButton, buttonParams)

        setContentView(rootLayout)

        // check permissions on launch
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // helper function to verify camera permission status
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // initialize camera-x to stream the camera feed to the viewfinder
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = viewFinder.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // capture image into memory buffer without writing straight to disk
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    // extract raw bytes from image-proxy buffer
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    // add to RAM queue silently
                    photoQueue.add(bytes)

                    // update visual preview stack on the main thread
                    runOnUiThread {
                        updateQueuePreview()
                    }

                    // always close the image proxy
                    image.close()
                }
            }
        )
    }

    // renders up to 3 thumbnails with rounded corners and a badge if more exist
    @SuppressLint("SetTextI18n")
    private fun updateQueuePreview() {
        previewContainer.removeAllViews()

        val totalPhotos = photoQueue.size
        if (totalPhotos == 0) return

        val displayCount = minOf(totalPhotos, 3)

        for (i in 0 until displayCount) {
            val thumbFrame = android.widget.FrameLayout(this).apply {
                val params = android.widget.LinearLayout.LayoutParams(120, 120).apply {
                    setMargins(10, 0, 10, 0)
                }
                layoutParams = params

                // add smooth rounded corners to each thumbnail frame
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 24f) // 24f sets corner radius
                    }
                }
            }

            // decode byte array into a bitmap thumbnail
            val photoBytes = photoQueue[totalPhotos - 1 - i]
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)

            val imageView = android.widget.ImageView(this).apply {
                setImageBitmap(bitmap)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
            thumbFrame.addView(imageView, android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            // add a badge counter if there are more than 3 photos in the queue
            if (i == 2 && totalPhotos > 3) {
                val extraCount = totalPhotos - 3
                val badge = android.widget.TextView(this).apply {
                    text = "+$extraCount"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    setBackgroundColor(android.graphics.Color.argb(180, 0, 0, 0))
                }
                thumbFrame.addView(badge, android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
            }

            previewContainer.addView(thumbFrame)
        }
    }
}