package com.group_a1.isocam.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
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
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.group_a1.isocam.R
import java.io.File

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

        // 1. sleek back button with circular background for high contrast
        val backButton = TextView(this).apply {
            text = "‹"
            setTextColor(Color.WHITE)
            textSize = 36f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setOnClickListener { finish() }
        }
        val backParams = FrameLayout.LayoutParams(110, 110).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 120
            leftMargin = 40
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
            topMargin = 120
            rightMargin = 40
        }
        rootLayout.addView(previewContainer, previewParams)

        // Dynamic Window Insets listener to position header controls safely below status bar/cutout on any device
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val topMarginPx = if (statusBarHeight > 0) statusBarHeight + 24 else 120

            (backButton.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = topMarginPx
            }
            backButton.requestLayout()

            (previewContainer.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = topMarginPx
            }
            previewContainer.requestLayout()

            insets
        }

        // 3. snapchat-style circular ring drawable for capture button
        val ringDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(10, Color.WHITE)
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
        val rawBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
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
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
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
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.argb(180, 0, 0, 0))
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
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
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

        // Google Photos style bottom action bar container
        val bottomBar = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            setPadding(40, 24, 40, 48)
            clipChildren = false
            clipToPadding = false
            // prevent clicks on bottom bar from dismissing the overlay
            setOnClickListener { }
        }
        val bottomBarParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        // Adjust padding dynamically for navigation bar insets
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomPaddingPx = if (navBarHeight > 0) navBarHeight + 24 else 48
            view.setPadding(40, 24, 40, bottomPaddingPx)
            insets
        }

        // 1. Share button (bottom left)
        val shareButton = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                sharePhoto(photoBytes)
            }
        }
        val shareIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_share)
        }
        val shareText = TextView(this).apply {
            text = getString(R.string.btn_share)
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            isSingleLine = true
            setPadding(0, 6, 0, 0)
        }
        shareButton.addView(shareIcon, LinearLayout.LayoutParams(64, 64))
        shareButton.addView(shareText, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val shareParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = 80
        }
        bottomBar.addView(shareButton, shareParams)

        // 2. Delete button (bottom right)
        val deleteButton = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            setPadding(20, 10, 20, 10)
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
                        closePreviewOverlay(root, previewOverlay)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        val deleteIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_delete)
        }
        val deleteText = TextView(this).apply {
            text = getString(R.string.btn_delete)
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            isSingleLine = true
            setPadding(0, 6, 0, 0)
        }
        deleteButton.addView(deleteIcon, LinearLayout.LayoutParams(64, 64))
        deleteButton.addView(deleteText, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val deleteParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = 80
        }
        bottomBar.addView(deleteButton, deleteParams)

        previewOverlay.addView(bottomBar, bottomBarParams)

        // tap background (excluding bottom bar) to close preview smoothly
        previewOverlay.setOnClickListener {
            closePreviewOverlay(root, previewOverlay)
        }

        root.addView(previewOverlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // hardware-accelerated smooth scale-up and fade-in animation
        previewOverlay.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withLayer()
            .start()
    }

    private fun closePreviewOverlay(root: ViewGroup, previewOverlay: View) {
        previewOverlay.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withLayer()
            .withEndAction {
                root.removeView(previewOverlay)
            }
            .start()
    }

    private fun sharePhoto(photoBytes: ByteArray) {
        try {
            val cacheFile = File(cacheDir, "shared_photo.jpg")
            cacheFile.writeBytes(photoBytes)
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                cacheFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Photo"))
        } catch (_: Exception) {
            Toast.makeText(this, "Sharing photo...", Toast.LENGTH_SHORT).show()
        }
    }
}