package com.group_a1.isocam.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
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
import kotlin.math.abs

class CameraActivity : ComponentActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null

    // the in-memory queue: holds raw image byte arrays safely in RAM
    private val photoQueue = mutableListOf<ByteArray>()

    // container views for UI elements
    private lateinit var previewContainer: LinearLayout
    private lateinit var trashButton: Button
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

        // 1. back arrow button (top left): pops back to previous activity without saving
        val backButton = Button(this).apply {
            text = "←"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 24f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        val backParams = FrameLayout.LayoutParams(120, 120).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 140
            leftMargin = 40
        }
        rootLayout.addView(backButton, backParams)

        // 2. circular transparent trash can target (bottom left) for drag-to-delete
        val trashDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke(8, android.graphics.Color.WHITE)
            alpha = 180
        }
        trashButton = Button(this).apply {
            text = "🗑️"
            textSize = 22f
            background = trashDrawable
            alpha = 0.6f
        }
        val trashParams = FrameLayout.LayoutParams(140, 140).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            bottomMargin = 100
            leftMargin = 50
        }
        rootLayout.addView(trashButton, trashParams)

        // 3. top-right queue thumbnail container (stays rigidly pinned to the right)
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
            topMargin = 150
            rightMargin = 50
        }
        rootLayout.addView(previewContainer, previewParams)

        // 4. snapchat-style circular ring drawable for capture button
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

    // renders thumbnails with stable floating drag-to-trash and safe preview clicks
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
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
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 24f)
                    }
                }

                var startX = 0f
                var startY = 0f
                var floatingView: ImageView? = null

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - startX
                            val deltaY = event.rawY - startY

                            // create floating drag view on first significant move
                            if (floatingView == null && (abs(deltaX) > 10 || abs(deltaY) > 10)) {
                                v.alpha = 0f // hide original view so space is preserved as a placeholder

                                val loc = IntArray(2)
                                v.getLocationOnScreen(loc)

                                val bitmap = getRotatedBitmap(photoQueue[targetIndex])
                                floatingView = ImageView(context).apply {
                                    setImageBitmap(bitmap)
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                    clipToOutline = true
                                    outlineProvider = v.outlineProvider
                                }

                                val floatParams = FrameLayout.LayoutParams(v.width, v.height).apply {
                                    leftMargin = loc[0]
                                    topMargin = loc[1]
                                }
                                rootLayout.addView(floatingView, floatParams)
                            }

                            // update floating view position smoothly across the entire root layout
                            floatingView?.let {
                                it.translationX = event.rawX - startX
                                it.translationY = event.rawY - startY
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (floatingView != null) {
                                // check if dropped over trash can bounds
                                val trashLocation = IntArray(2)
                                trashButton.getLocationOnScreen(trashLocation)
                                val floatLocation = IntArray(2)
                                floatingView!!.getLocationOnScreen(floatLocation)

                                val floatCenterX = floatLocation[0] + floatingView!!.width / 2
                                val floatCenterY = floatLocation[1] + floatingView!!.height / 2

                                val isOverTrash = floatCenterX in trashLocation[0]..(trashLocation[0] + trashButton.width) &&
                                        floatCenterY in trashLocation[1]..(trashLocation[1] + trashButton.height)

                                rootLayout.removeView(floatingView)
                                floatingView = null

                                if (isOverTrash) {
                                    // remove from memory array and update preview stack
                                    photoQueue.removeAt(targetIndex)
                                    updateQueuePreview()
                                } else {
                                    // snap back: restore original view visibility
                                    v.alpha = 1f
                                }
                            } else {
                                // quick tap detection: show fullscreen preview (badge slot does nothing)
                                if (!isBadgeSlot) {
                                    showFullscreenPreview(photoQueue[targetIndex])
                                }
                            }
                            true
                        }
                        else -> false
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

    private fun showFullscreenPreview(photoBytes: ByteArray) {
        val root = window.decorView.rootView as ViewGroup

        val previewOverlay = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            alpha = 0f
            setOnClickListener {
                animate().alpha(0f).setDuration(200).withEndAction {
                    root.removeView(this)
                }.start()
            }
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

        root.addView(previewOverlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        previewOverlay.animate().alpha(1f).setDuration(200).start()
    }
}