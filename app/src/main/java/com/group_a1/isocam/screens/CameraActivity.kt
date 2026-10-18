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
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
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
import com.group_a1.isocam.CreateResult
import com.group_a1.isocam.FeedbackHelper
import com.group_a1.isocam.FolderDetailActivity
import com.group_a1.isocam.FolderRepository
import com.group_a1.isocam.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraActivity : ComponentActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    // the in-memory queue: holds raw image byte arrays strictly in RAM until explicitly sent
    private val photoQueue = mutableListOf<ByteArray>()

    // container views for UI elements
    private lateinit var previewContainer: LinearLayout
    private lateinit var rootLayout: FrameLayout
    private lateinit var sendButton: TextView
    private lateinit var zoomTextView: TextView
    
    private var isFrontCamera = false
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    // modern android way to handle runtime permissions safely
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            FeedbackHelper.show(this, "Camera permission is required.")
            finish()
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this)

        // set up camera preview view to fill the screen
        viewFinder = PreviewView(this)
        rootLayout.addView(viewFinder, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        })

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isFrontCamera = !isFrontCamera
                startCamera()
                return true
            }
        })

        viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        // 1. sleek back button with circular background for high contrast
        val backButton = TextView(this).apply {
            text = "‹"
            setTextColor(Color.WHITE)
            textSize = 36f
            gravity = Gravity.CENTER
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(120, 0, 0, 0))
            }
            setOnClickListener { discardAndExit() }
        }
        val backParams = FrameLayout.LayoutParams(110, 110).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 120
            leftMargin = 40
        }
        rootLayout.addView(backButton, backParams)

        // System back button/gesture discards RAM queue immediately and exits
        onBackPressedDispatcher.addCallback(this) {
            discardAndExit()
        }

        // 2. top-right queue thumbnail container
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

        // Flash Button
        val flashButton = TextView(this).apply {
            text = "⚡ Off"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(30, 20, 30, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 40f
                setColor(Color.argb(120, 0, 0, 0))
            }
            setOnClickListener {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                text = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> "⚡ Off"
                    ImageCapture.FLASH_MODE_AUTO -> "⚡ Auto"
                    else -> "⚡ On"
                }
                imageCapture?.flashMode = flashMode
            }
        }
        val flashParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 135
        }
        rootLayout.addView(flashButton, flashParams)

        // Camera Flip Button
        val flipButton = TextView(this).apply {
            text = "🔄"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(120, 0, 0, 0))
            }
            setOnClickListener {
                isFrontCamera = !isFrontCamera
                startCamera()
            }
        }
        val flipParams = FrameLayout.LayoutParams(110, 110).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            bottomMargin = 140
            leftMargin = 80
        }
        rootLayout.addView(flipButton, flipParams)

        // Zoom text view
        zoomTextView = TextView(this).apply {
            text = "1.0x"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 10)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.argb(120, 0, 0, 0))
            }
        }
        val zoomParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 350
        }
        rootLayout.addView(zoomTextView, zoomParams)

        // Dynamic Window Insets listener to position header controls safely below status bar/cutout on any device
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val topMarginPx = if (statusBarHeight > 0) statusBarHeight + 24 else 120

            (backButton.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topMarginPx }
            backButton.requestLayout()

            (previewContainer.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topMarginPx }
            previewContainer.requestLayout()
            
            (flashButton.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topMarginPx + 15 }
            flashButton.requestLayout()

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
            setOnClickListener { 
                if (isFrontCamera && flashMode == ImageCapture.FLASH_MODE_ON) {
                    val overlay = View(this@CameraActivity).apply {
                        setBackgroundColor(Color.WHITE)
                        alpha = 0f
                    }
                    rootLayout.addView(overlay, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, 
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                    overlay.animate().alpha(1f).setDuration(200).withEndAction {
                        takePhoto()
                        overlay.animate().alpha(0f).setDuration(200).withEndAction {
                            rootLayout.removeView(overlay)
                        }.start()
                    }.start()
                } else {
                    takePhoto() 
                }
            }
        }

        val buttonParams = FrameLayout.LayoutParams(200, 200).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 100
        }
        rootLayout.addView(captureButton, buttonParams)

        // 4. Send button (bottom right): saves RAM queue to target folder and navigates to folder
        sendButton = TextView(this).apply {
            text = "Send"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 40f
                setColor(Color.argb(255, 30, 136, 229))
            }
            setPadding(36, 20, 36, 20)
            setOnClickListener { sendPhotosToFolder() }
        }

        val sendParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 110
            rightMargin = 60
        }
        rootLayout.addView(sendButton, sendParams)

        setContentView(rootLayout)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun discardAndExit() {
        photoQueue.clear()
        finish()
    }

    private fun sendPhotosToFolder() {
        if (photoQueue.isEmpty()) return

        val targetFolder = getOrCreateTargetFolder() ?: run {
            FeedbackHelper.show(this, "Failed to determine target folder")
            return
        }

        // Write all queued RAM photos into target folder on disk
        photoQueue.forEach { photoBytes ->
            try {
                val photoFile = FolderRepository.newPhotoFile(targetFolder)
                val bitmap = getRotatedBitmap(photoBytes)
                FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            } catch (_: Exception) {
            }
        }

        photoQueue.clear()

        // Navigate directly into target folder view
        FolderDetailActivity.start(this, targetFolder.absolutePath)
        finish()
    }

    private fun getOrCreateTargetFolder(): File? {
        val existingFolderPath = intent.getStringExtra("folder_path")
        if (!existingFolderPath.isNullOrEmpty()) {
            val folder = File(existingFolderPath)
            if (folder.exists() && folder.isDirectory) {
                return folder
            }
        }

        // Home context: generate automatic folder name using current date/time
        val autoFolderName = SimpleDateFormat("MM-dd-yyyy HH:mm", Locale.US).format(Date())
        return when (val result = FolderRepository.createFolder(this, autoFolderName)) {
            is CreateResult.Success -> result.folder
            is CreateResult.Error -> {
                val fallbackName = SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.US).format(Date())
                (FolderRepository.createFolder(this, fallbackName) as? CreateResult.Success)?.folder
            }
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

            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()
                
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                
                camera?.cameraInfo?.zoomState?.observe(this) { state ->
                    val ratio = state.zoomRatio
                    zoomTextView.text = String.format(Locale.US, "%.1fx", ratio)
                }
            } catch (exc: Exception) {
                FeedbackHelper.show(this, "Camera binding failed: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    FeedbackHelper.show(baseContext as ComponentActivity, "Photo capture failed: ${exc.message}")
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    // Strictly RAM-first: append bytes to in-memory queue only
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
        // Adjust rotation based on front/back camera
        val rotation = if (isFrontCamera) -90f else 90f
        val matrix = Matrix().apply { 
            postRotate(rotation)
            if (isFrontCamera) {
                postScale(-1f, 1f) // Mirror for front camera
            }
        }
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }

    // renders clean thumbnails with tap-to-preview interaction
    @SuppressLint("SetTextI18n")
    private fun updateQueuePreview() {
        previewContainer.removeAllViews()

        val totalPhotos = photoQueue.size
        if (totalPhotos == 0) {
            sendButton.visibility = View.GONE
            return
        }

        sendButton.visibility = View.VISIBLE
        sendButton.text = "Send ($totalPhotos)"

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

    // full-screen preview with Google Photos style bottom action bar (Share, Move & Delete)
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
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            setPadding(40, 24, 40, 48)
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER
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

        val createActionBtn = { iconRes: Int, textStr: String, action: () -> Unit ->
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                clipChildren = false
                clipToPadding = false
                setPadding(20, 10, 20, 10)
                setOnClickListener { action() }
                
                val icon = ImageView(this@CameraActivity).apply {
                    if (iconRes != 0) {
                        setImageResource(iconRes)
                    } else {
                        // fallback for missing icon
                        val txt = TextView(context).apply {
                            text = "📁"
                            textSize = 24f
                        }
                        addView(txt, LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ))
                    }
                }
                if (iconRes != 0) addView(icon, LinearLayout.LayoutParams(64, 64))
                
                val textView = TextView(this@CameraActivity).apply {
                    text = textStr
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    setPadding(0, 6, 0, 0)
                }
                addView(textView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }
        }

        // 1. Share button
        val shareButton = createActionBtn(R.drawable.ic_share, getString(R.string.btn_share)) {
            sharePhoto(photoBytes)
        }
        
        // 2. Move button
        val moveButton = createActionBtn(0, "Move") {
            FeedbackHelper.show(this, "Photo is still in RAM. Send to save first.")
        }
        
        // 3. Delete button
        val deleteButton = createActionBtn(R.drawable.ic_delete, getString(R.string.btn_delete)) {
            AlertDialog.Builder(this@CameraActivity)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to remove this photo?")
                .setPositiveButton("Delete") { _, _ ->
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
        
        bottomBar.addView(shareButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bottomBar.addView(moveButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bottomBar.addView(deleteButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

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
            FeedbackHelper.show(this, "Sharing photo...")
        }
    }
}