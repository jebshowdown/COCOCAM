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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    private lateinit var ringLightOverlay: View
    private var activePreviewOverlay: View? = null
    
    private var isFrontCamera = false
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var isTorchOn = false

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

        // Constant front-facing clean rounded soft-glow ring light overlay
        val metrics = resources.displayMetrics
        val radiusPx = minOf(metrics.widthPixels, metrics.heightPixels) * 0.8f

        ringLightOverlay = View(this).apply {
            val ringLightGradient = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 120f // Smooth rounded inner edges
                gradientType = GradientDrawable.RADIAL_GRADIENT
                colors = intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(40, 255, 255, 255),
                    Color.argb(180, 255, 255, 255),
                    Color.WHITE
                )
                setGradientCenter(0.5f, 0.5f)
                gradientRadius = radiusPx
            }
            background = ringLightGradient
            isClickable = false
            isFocusable = false
            visibility = View.GONE
        }
        rootLayout.addView(ringLightOverlay, FrameLayout.LayoutParams(
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
                updateFrontRingLight()
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

        // System back button/gesture closes active preview overlay or discards RAM queue and exits
        onBackPressedDispatcher.addCallback(this) {
            val overlay = activePreviewOverlay
            if (overlay != null) {
                val root = window.decorView.rootView as ViewGroup
                closePreviewOverlay(root, overlay)
            } else {
                discardAndExit()
            }
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
        val flashButton = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(30, 20, 30, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 40f
                setColor(Color.argb(120, 0, 0, 0))
            }
            
            val flashIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_flash)
                layoutParams = LinearLayout.LayoutParams(40, 40)
            }
            val flashText = TextView(context).apply {
                text = " Off"
                setTextColor(Color.WHITE)
                textSize = 14f
            }
            addView(flashIcon)
            addView(flashText)

            setOnClickListener {
                if (isTorchOn) {
                    isTorchOn = false
                    flashMode = ImageCapture.FLASH_MODE_OFF
                } else {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> {
                            isTorchOn = true
                            ImageCapture.FLASH_MODE_OFF // Use torch instead of flash
                        }
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                }
                
                flashText.text = if (isTorchOn) " Torch" else when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> " Off"
                    ImageCapture.FLASH_MODE_AUTO -> " Auto"
                    else -> " On"
                }
                
                imageCapture?.flashMode = flashMode
                camera?.cameraControl?.enableTorch(isTorchOn)
                updateFrontRingLight()
            }
        }
        val flashParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            bottomMargin = 240
            leftMargin = 40
        }
        rootLayout.addView(flashButton, flashParams)

        // Camera Flip Button
        val flipButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_flip_camera)
            setPadding(25, 25, 25, 25)
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
            bottomMargin = 100
            leftMargin = 40
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
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val topMarginPx = if (statusBarHeight > 0) statusBarHeight + 24 else 120

            (backButton.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topMarginPx }
            backButton.requestLayout()

            (previewContainer.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topMarginPx }
            previewContainer.requestLayout()
            
            (flashButton.layoutParams as FrameLayout.LayoutParams).apply { bottomMargin = navBarHeight + 240 }
            flashButton.requestLayout()
            (flipButton.layoutParams as FrameLayout.LayoutParams).apply { bottomMargin = navBarHeight + 100 }
            flipButton.requestLayout()

            insets
        }

        // 3. snapchat-style circular ring drawable for capture button
        val ringDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(160, 0, 0, 0)) // Translucent dark background matching flip/flash/back buttons
            setStroke(8, Color.WHITE) // Crisp outer white stroke
        }

        val captureButton = Button(this).apply {
            text = ""
            background = ringDrawable
            setOnClickListener { 
                val shouldFlash = isFrontCamera && !isTorchOn && (
                    flashMode == ImageCapture.FLASH_MODE_ON || 
                    (flashMode == ImageCapture.FLASH_MODE_AUTO && isLowLightEnvironment())
                )

                if (shouldFlash) {
                    ringLightOverlay.visibility = View.VISIBLE
                    ringLightOverlay.alpha = 0f
                    ringLightOverlay.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .withEndAction {
                            takePhoto()
                            ringLightOverlay.animate()
                                .alpha(0f)
                                .setDuration(350)
                                .withEndAction {
                                    if (!isTorchOn) ringLightOverlay.visibility = View.GONE
                                }
                                .start()
                        }
                        .start()
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
        captureButton.elevation = 30f
        captureButton.bringToFront()

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

    private fun isLowLightEnvironment(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour < 7 || hour >= 19
    }

    private fun updateFrontRingLight() {
        if (isFrontCamera && isTorchOn) {
            ringLightOverlay.alpha = 1f
            ringLightOverlay.visibility = View.VISIBLE
        } else {
            ringLightOverlay.visibility = View.GONE
        }
    }

    private fun startCamera() {
        updateFrontRingLight()
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
        var currentIndex = photoQueue.indexOf(photoBytes).coerceAtLeast(0)

        val previewOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
        }
        activePreviewOverlay = previewOverlay

        val fullImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        fun updateCurrentPhotoDisplay() {
            if (photoQueue.isEmpty()) {
                closePreviewOverlay(root, previewOverlay)
                return
            }
            currentIndex = currentIndex.coerceIn(0, photoQueue.size - 1)
            val currentBytes = photoQueue[currentIndex]
            val bitmap = getRotatedBitmap(currentBytes)
            fullImageView.setImageBitmap(bitmap)
        }

        updateCurrentPhotoDisplay()

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
            if (currentIndex in photoQueue.indices) {
                sharePhoto(photoQueue[currentIndex])
            }
        }
        
        // 2. Move button
        val moveButton = createActionBtn(R.drawable.ic_folder, "Move") {
            if (currentIndex in photoQueue.indices) {
                val targetBytes = photoQueue[currentIndex]
                showMovePhotoFolderDialog(targetBytes) {
                    if (photoQueue.isEmpty()) {
                        closePreviewOverlay(root, previewOverlay)
                    } else {
                        updateCurrentPhotoDisplay()
                    }
                }
            }
        }
        
        // 3. Delete button
        val deleteButton = createActionBtn(R.drawable.ic_delete, getString(R.string.btn_delete)) {
            if (currentIndex in photoQueue.indices) {
                val targetBytes = photoQueue[currentIndex]
                AlertDialog.Builder(this@CameraActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Delete Photo")
                    .setMessage("Are you sure you want to remove this photo?")
                    .setPositiveButton("Yes") { _, _ ->
                        photoQueue.remove(targetBytes)
                        updateQueuePreview()
                        if (photoQueue.isEmpty()) {
                            closePreviewOverlay(root, previewOverlay)
                        } else {
                            updateCurrentPhotoDisplay()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        
        bottomBar.addView(shareButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bottomBar.addView(moveButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bottomBar.addView(deleteButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        previewOverlay.addView(bottomBar, bottomBarParams)

        var downX = 0f
        var downY = 0f

        previewOverlay.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - downX
                    val deltaY = event.y - downY
                    val absX = Math.abs(deltaX)
                    val absY = Math.abs(deltaY)

                    if (absX > absY && absX > 80) {
                        if (deltaX < 0) {
                            // Swipe Left -> Next photo in RAM queue
                            if (currentIndex < photoQueue.size - 1) {
                                currentIndex++
                                updateCurrentPhotoDisplay()
                            }
                        } else {
                            // Swipe Right -> Previous photo in RAM queue
                            if (currentIndex > 0) {
                                currentIndex--
                                updateCurrentPhotoDisplay()
                            }
                        }
                    } else if (absX < 30 && absY < 30) {
                        // Tap -> dismiss preview
                        view.performClick()
                        closePreviewOverlay(root, previewOverlay)
                    }
                    true
                }
                else -> true
            }
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

    private fun showMovePhotoFolderDialog(photoBytes: ByteArray, onMoved: () -> Unit) {
        val folders = FolderRepository.listFolders(this)
        if (folders.isEmpty()) {
            FeedbackHelper.show(this, "No folders available to move photo.")
            return
        }

        val folderNames = folders.map { it.name }.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Move Photo to Folder")
            .setItems(folderNames) { dialog, which ->
                val targetFolderItem = folders[which]
                val targetFolder = File(targetFolderItem.path)
                try {
                    val photoFile = FolderRepository.newPhotoFile(targetFolder)
                    val bitmap = getRotatedBitmap(photoBytes)
                    FileOutputStream(photoFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    photoQueue.remove(photoBytes)
                    updateQueuePreview()
                    FeedbackHelper.show(this, "Moved photo to ${targetFolderItem.name}")
                    onMoved()
                } catch (_: Exception) {
                    FeedbackHelper.show(this, "Failed to move photo.")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun closePreviewOverlay(root: ViewGroup, previewOverlay: View) {
        if (activePreviewOverlay == previewOverlay) {
            activePreviewOverlay = null
        }
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