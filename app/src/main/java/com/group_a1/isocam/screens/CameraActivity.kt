package com.group_a1.isocam.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

    // container views for UI elements
    private lateinit var previewContainer: LinearLayout
    private lateinit var rootLayout: FrameLayout

    // modern android way to handle runtime permissions safely
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this)

        // set up camera preview view to fill the screen
        viewFinder = PreviewView(this)
        rootLayout.addView(viewFinder, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // 1. sleek, thin '<' back button (shifted down to completely clear status bar)
        val backButton = Button(this).apply {
            text = "‹"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 40f
            setTypeface(null, android.graphics.Typeface.NORMAL)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        val backParams = FrameLayout.LayoutParams(140, 140).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 170
            leftMargin = 30
        }
        rootLayout.addView(backButton, backParams)

        // 2. top-right queue thumbnail container (stays rigidly pinned to the right)
        previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val previewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 170
            rightMargin = 50
        }
        rootLayout.addView(previewContainer, previewParams)

        // 3. snapchat-style circular ring drawable for capture button
        val ringDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke(10, android.graphics.Color.WHITE)
            alpha = 180
        }

        val captureButton = Button(this).apply {
            text = ""
            background = ringDrawable
            setOnClickListener { takePhoto() }
        }

        val buttonParams = FrameLayout.LayoutParams(200, 200).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 100
        }
        rootLayout.addView(captureButton, buttonParams)

        setContentView(rootLayout)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

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

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    photoQueue.add(bytes)

                    runOnUiThread {
                        updateQueuePreview()
                    }

                    image.close()
                }
            }
        )
    }

    private fun getRotatedBitmap(photoBytes: ByteArray): Bitmap {
        val rawBitmap = android.graphics.BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }

    // renders clean thumbnails with tap-to-preview interaction
    @SuppressLint("SetTextI18n")
    private fun updateQueuePreview() {
        previewContainer.removeAllViews()

        val totalPhotos = photoQueue.size
        if (totalPhotos == 0) return

        val displayCount = minOf(totalPhotos, 3)

        for (i in 0 until displayCount) {
            val targetIndex = totalPhotos - 1 - i
            val isBadgeSlot = (i == 2 && totalPhotos > 3)

            val thumbFrame = FrameLayout(this).apply {
                val params = LinearLayout.LayoutParams(120, 120).apply {
                    setMargins(10, 0, 10, 0)
                }
                layoutParams = params

                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 24f)
                    }
                }

                setOnClickListener {
                    if (!isBadgeSlot) {
                        showFullscreenPreview(photoQueue[targetIndex])
                    }
                }
            }

            val bitmap = getRotatedBitmap(photoQueue[targetIndex])
            val imageView = ImageView(this).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            thumbFrame.addView(imageView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            // add static uneditable badge counter if there are more than 3 photos
            if (isBadgeSlot) {
                val extraCount = totalPhotos - 3
                val badge = TextView(this).apply {
                    text = "+$extraCount"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setBackgroundColor(android.graphics.Color.argb(180, 0, 0, 0))
                }
                thumbFrame.addView(badge, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
            }

            previewContainer.addView(thumbFrame)
        }
    }

    // full-screen preview with Google Photos style bottom action bar (Share & Delete)
    private fun showFullscreenPreview(photoBytes: ByteArray) {
        val root = window.decorView.rootView as ViewGroup

        val previewOverlay = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            alpha = 0f
            scaleX = 0.95f
            scaleY = 0.95f
        }

        val bitmap = getRotatedBitmap(photoBytes)
        val fullImageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        previewOverlay.addView(fullImageView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // bottom action bar layout (resembling Google Photos footer)
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.argb(200, 0, 0, 0))
            setPadding(0, 20, 0, 40)
        }
        val bottomBarParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        // 1. Share button
        val shareButton = TextView(this).apply {
            text = "📤\nShare"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(60, 10, 60, 10)
            setOnClickListener {
                Toast.makeText(this@CameraActivity, "Share action triggered", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Delete button
        val deleteButton = TextView(this).apply {
            text = "🗑\nDelete"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(60, 10, 60, 10)
            setOnClickListener {
                AlertDialog.Builder(this@CameraActivity)
                    .setTitle("Delete Photo")
                    .setMessage("Are you sure you want to remove this photo?")
                    .setPositiveButton("Yes") { _, _ ->
                        val photoIndex = photoQueue.indexOf(photoBytes)
                        if (photoIndex != -1) {
                            photoQueue.removeAt(photoIndex)
                            updateQueuePreview()
                        }
                        previewOverlay.animate()
                            .alpha(0f)
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(200)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                root.removeView(previewOverlay)
                            }.start()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        bottomBar.addView(shareButton)
        bottomBar.addView(deleteButton)
        previewOverlay.addView(bottomBar, bottomBarParams)

        // tap background (excluding bottom bar) to close preview smoothly
        previewOverlay.setOnClickListener {
            previewOverlay.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    root.removeView(previewOverlay)
                }.start()
        }

        root.addView(previewOverlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // smooth scale-up and fade-in animation
        previewOverlay.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}