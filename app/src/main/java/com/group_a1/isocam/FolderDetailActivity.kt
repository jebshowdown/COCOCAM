package com.group_a1.isocam

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.group_a1.isocam.screens.CameraActivity
import com.group_a1.isocam.ui.theme.IsoCamTheme
import com.group_a1.isocam.ui.theme.LocalIsoCamExtras
import com.group_a1.isocam.ui.theme.SparkleBubbleOverlay
import com.group_a1.isocam.ui.theme.ThemeController
import com.group_a1.isocam.ui.theme.ThemeMode
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class FolderDetailActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)
    private var currentFolderFile: File? = null
    
    // Using a callback to handle URI cleanup after import
    private var pendingImportUris: List<Uri>? = null

    private val pickMultiplePhotosLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
            importPhotosFromUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeController.init(this)

        val folder = intent.getStringExtra(EXTRA_FOLDER_PATH)?.let { File(it) }
        if (folder == null || !folder.isDirectory) {
            finish()
            return
        }
        currentFolderFile = folder
        FolderRepository.markFolderOpened(folder)

        val alreadyAuthenticated = intent.getBooleanExtra(EXTRA_IS_AUTHENTICATED, false)

        getSharedPreferences("isocam_prefs", MODE_PRIVATE)
            .edit()
            .putString("last_opened_folder_path", folder.absolutePath)
            .apply()

        onBackPressedDispatcher.addCallback(this) {
            navigateHome()
        }

        setContent {
            IsoCamTheme {
                var isUnlocked by remember { mutableStateOf(alreadyAuthenticated || !FolderRepository.isLocked(folder)) }
                var showCleanupDialog by remember { mutableStateOf(false) }

                LaunchedEffect(pendingImportUris) {
                    if (pendingImportUris != null) {
                        showCleanupDialog = true
                    }
                }

                if (!isUnlocked) {
                    UnlockFolderDialog(
                        folderName = folder.name,
                        onDismiss = { finish() },
                        onUnlock = { passcode ->
                            if (FolderRepository.verifyPasscode(folder, passcode)) {
                                isUnlocked = true
                                true
                            } else {
                                FeedbackHelper.show(this@FolderDetailActivity, "Incorrect passcode")
                                false
                            }
                        }
                    )
                } else {
                    if (showCleanupDialog) {
                        AlertDialog(
                            onDismissRequest = { 
                                showCleanupDialog = false
                                pendingImportUris = null
                            },
                            title = { Text("Delete Original Photos?") },
                            text = { Text("The photos have been imported. Do you want to delete the originals from your gallery to save space?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    pendingImportUris?.let { uris ->
                                        try {
                                            var deletedCount = 0
                                            uris.forEach { uri ->
                                                contentResolver.delete(uri, null, null)
                                                deletedCount++
                                            }
                                            FeedbackHelper.show(this@FolderDetailActivity, "Deleted $deletedCount original photo${if (deletedCount == 1) "" else "s"}")
                                        } catch (_: SecurityException) {
                                            FeedbackHelper.show(this@FolderDetailActivity, "Failed to delete: Permission denied")
                                        } catch (_: Exception) {
                                            FeedbackHelper.show(this@FolderDetailActivity, "Failed to delete originals")
                                        }
                                    }
                                    showCleanupDialog = false
                                    pendingImportUris = null
                                }) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { 
                                    showCleanupDialog = false 
                                    pendingImportUris = null
                                }) {
                                    Text("Keep")
                                }
                            }
                        )
                    }

                    FolderDetailScreen(
                        initialFolder = folder,
                        refreshTick = refreshTick,
                        onBack = { navigateHome() },
                        onCamera = { openCamera(it) },
                        onOpenSettings = { openAppSettings() },
                        onImportPhotos = {
                            pickMultiplePhotosLauncher.launch(
                                PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                            )
                        },
                        onSharePhotos = { sharePhotos(it) },
                        onFolderRenamed = { currentFolderFile = it }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }

    private fun navigateHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun openAppSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("initial_tab", "Settings")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun openCamera(folder: File) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra("folder_path", folder.absolutePath)
        }
        startActivity(intent)
    }

    private fun importPhotosFromUris(uris: List<Uri>) {
        val folder = currentFolderFile ?: return
        var importedCount = 0
        uris.forEach { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val photoFile = FolderRepository.newPhotoFile(folder)
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                    importedCount++
                }
            } catch (_: Exception) {
            }
        }
        if (importedCount > 0) {
            FeedbackHelper.show(this, "Imported $importedCount photo${if (importedCount == 1) "" else "s"}")
            refreshTick++
        }
    }

    private fun sharePhotos(files: List<File>) {
        if (files.isEmpty()) return
        try {
            val uris = ArrayList<Uri>()
            files.forEach { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    file
                )
                uris.add(uri)
            }
            val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                if (uris.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                } else {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Photos"))
        } catch (_: Exception) {
            FeedbackHelper.show(this, "Failed to share photos")
        }
    }

    companion object {
        private const val EXTRA_FOLDER_PATH = "folder_path"
        private const val EXTRA_IS_AUTHENTICATED = "is_authenticated"

        fun start(context: Context, folderPath: String, isAuthenticated: Boolean = false) {
            context.startActivity(
                Intent(context, FolderDetailActivity::class.java)
                    .putExtra(EXTRA_FOLDER_PATH, folderPath)
                    .putExtra(EXTRA_IS_AUTHENTICATED, isAuthenticated)
            )
        }
    }
}

private enum class FolderNavTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    Folders("Folders", Icons.Filled.Folder, Icons.Outlined.Folder),
    Camera("Camera", Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FolderDetailScreen(
    initialFolder: File,
    refreshTick: Int,
    onBack: () -> Unit,
    onCamera: (File) -> Unit,
    onOpenSettings: () -> Unit,
    onImportPhotos: () -> Unit,
    onSharePhotos: (List<File>) -> Unit,
    onFolderRenamed: (File) -> Unit,
) {
    var currentFolder by remember { mutableStateOf(initialFolder) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember(currentFolder) { mutableStateOf(currentFolder.name) }
    var photos by remember { mutableStateOf(emptyList<File>()) }

    var selectedPhotos by remember { mutableStateOf(setOf<File>()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showFolderSettingsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showTimedDeletionDialog by remember { mutableStateOf(false) }
    var previewPhoto by remember { mutableStateOf<File?>(null) }

    BackHandler(enabled = selectedPhotos.isNotEmpty() || previewPhoto != null) {
        if (selectedPhotos.isNotEmpty()) {
            selectedPhotos = emptySet()
        } else if (previewPhoto != null) {
            previewPhoto = null
        }
    }

    val context = LocalContext.current
    val extras = LocalIsoCamExtras.current
    val sunsetHeader = extras.sunsetHeader
    val backgroundColor = MaterialTheme.colorScheme.background
    val rainbow = extras.rainbow

    LaunchedEffect(currentFolder, refreshTick) {
        photos = FolderRepository.listPhotos(currentFolder)
    }

    fun commitRename() {
        val trimmed = editedName.trim()
        if (trimmed.isNotBlank() && trimmed != currentFolder.name) {
            when (val result = FolderRepository.renameFolder(context, currentFolder.name, trimmed)) {
                is CreateResult.Success -> {
                    currentFolder = result.folder
                    onFolderRenamed(result.folder)
                    isEditingName = false
                }
                is CreateResult.Error -> {
                    FeedbackHelper.show(context as Activity, result.message)
                }
            }
        } else {
            isEditingName = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (sunsetHeader.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(sunsetHeader)
                    )
            ) {
                if (ThemeController.mode == ThemeMode.RainbowDolphins) {
                    SparkleBubbleOverlay(modifier = Modifier.fillMaxSize())
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (previewPhoto == null) {
                    if (selectedPhotos.isNotEmpty()) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            navigationIcon = {
                                IconButton(onClick = { selectedPhotos = emptySet() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                                }
                            },
                            title = {
                                Text("${selectedPhotos.size} selected", style = MaterialTheme.typography.titleMedium)
                            },
                            actions = {
                                IconButton(onClick = { onSharePhotos(selectedPhotos.toList()) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share selected")
                                }
                                IconButton(onClick = { showMoveDialog = true }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move selected")
                                }
                                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            title = {
                                if (isEditingName) {
                                    val focusRequester = remember { FocusRequester() }
    
                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }
    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editedName,
                                            onValueChange = { editedName = it },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(focusRequester),
                                            textStyle = MaterialTheme.typography.titleMedium,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { commitRename() })
                                        )
                                        IconButton(onClick = { commitRename() }) {
                                            Icon(Icons.Default.Check, contentDescription = "Save name")
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable {
                                                editedName = currentFolder.name
                                                isEditingName = true
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = currentFolder.name,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename folder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Folder options")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        shape = MaterialTheme.shapes.extraLarge,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                editedName = currentFolder.name
                                                isEditingName = true
                                            }
                                        )
                                        if (FolderRepository.isLocked(currentFolder)) {
                                            DropdownMenuItem(
                                                text = { Text("Update Passcode") },
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    showLockDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Remove Passcode") },
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    FolderRepository.setPasscode(currentFolder, "")
                                                    FeedbackHelper.show(context as Activity, "Passcode removed")
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Lock Folder") },
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    showLockDialog = true
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Timed Deletion") },
                                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                showTimedDeletionDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export Photos as PDF") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                val pdfFile = FolderRepository.exportFolderToPdf(context, currentFolder)
                                                if (pdfFile != null) {
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Export PDF"))
                                                } else {
                                                    FeedbackHelper.show(context as Activity, "No photos to export into PDF")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                menuExpanded = false
                                                showDeleteConfirmDialog = true
                                                selectedPhotos = emptySet()
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            },
            bottomBar = {
                if (previewPhoto == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        FolderNavTab.entries.forEach { tab ->
                            val selected = tab == FolderNavTab.Folders
                            val glowColor = extras.glow
                            val selectedIconTint = if (selected && ThemeController.mode == ThemeMode.RainbowDolphins && glowColor != Color.Unspecified) {
                                glowColor
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
    
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    when (tab) {
                                        FolderNavTab.Folders -> onBack()
                                        FolderNavTab.Camera -> onCamera(currentFolder)
                                        FolderNavTab.Settings -> onOpenSettings()
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = selectedIconTint,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (previewPhoto == null && selectedPhotos.isEmpty()) {
                    val fabBrush = if (rainbow.isNotEmpty()) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    } else null

                    if (fabBrush != null) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(fabBrush)
                                .clickable { onImportPhotos() }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import photos", tint = Color.White)
                        }
                    } else {
                        FloatingActionButton(
                            onClick = onImportPhotos,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import photos")
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (photos.isEmpty()) {
                    if (ThemeController.mode == ThemeMode.RainbowDolphins) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("✨ 🐬 ✨", style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No photos yet.\nTap + to import existing photos.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No photos yet.\nTap + to import existing photos.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(photos, key = { it.absolutePath }) { file ->
                            val isSelected = file in selectedPhotos
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.97f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "tileScale"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(MaterialTheme.shapes.medium)
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = ripple(),
                                        onClick = {
                                            if (selectedPhotos.isNotEmpty()) {
                                                selectedPhotos = if (isSelected) selectedPhotos - file else selectedPhotos + file
                                            } else {
                                                previewPhoto = file
                                            }
                                        },
                                        onLongClick = {
                                            selectedPhotos = if (isSelected) selectedPhotos - file else selectedPhotos + file
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = file.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Full screen preview overlay
            if (previewPhoto != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .zIndex(100f)
                        .clickable { previewPhoto = null }
                ) {
                    AsyncImage(
                        model = previewPhoto,
                        contentDescription = "Full Screen Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().align(Alignment.Center)
                    )
                    
                    // Bottom Action Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 40.dp, vertical = 24.dp)
                            .clickable(enabled = false) { }, // prevent clicks from dismissing
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { previewPhoto?.let { onSharePhotos(listOf(it)) } }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Share", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }

                        // Move
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedPhotos = setOf(previewPhoto!!)
                                showMoveDialog = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_folder),
                                contentDescription = "Move",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Move", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }

                        // Delete
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedPhotos = setOf(previewPhoto!!)
                                showDeleteConfirmDialog = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Delete", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Timed Deletion Dialog
            if (showTimedDeletionDialog) {
                var selectedOption by remember { mutableStateOf("1 hour") }
                
                AlertDialog(
                    onDismissRequest = { showTimedDeletionDialog = false },
                    title = { Text("Timed Deletion") },
                    text = {
                        Column {
                            listOf("1 hour", "1 day", "1 week", "Custom").forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            if (option == "Custom") {
                                                val cal = Calendar.getInstance()
                                                DatePickerDialog(
                                                    context,
                                                    { _, year, month, dayOfMonth ->
                                                        val c = Calendar.getInstance()
                                                        c.set(year, month, dayOfMonth)
                                                        val settings = FolderRepository.loadSettings(currentFolder)
                                                        FolderRepository.saveSettings(currentFolder, settings.copy(expirationDate = c.timeInMillis))
                                                        FeedbackHelper.show(context as Activity, "Folder will delete on ${c.time}")
                                                        showTimedDeletionDialog = false
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).apply {
                                                    datePicker.minDate = System.currentTimeMillis()
                                                }.show()
                                            } else {
                                                selectedOption = option
                                            }
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    RadioButton(
                                        selected = (selectedOption == option),
                                        onClick = null // let the row handle it
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(option)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (selectedOption != "Custom") {
                                val offset = when (selectedOption) {
                                    "1 hour" -> 1000L * 60 * 60
                                    "1 day" -> 1000L * 60 * 60 * 24
                                    "1 week" -> 1000L * 60 * 60 * 24 * 7
                                    else -> 0L
                                }
                                val settings = FolderRepository.loadSettings(currentFolder)
                                FolderRepository.saveSettings(currentFolder, settings.copy(expirationDate = System.currentTimeMillis() + offset))
                                FeedbackHelper.show(context as Activity, "Timed deletion set")
                            }
                            showTimedDeletionDialog = false
                        }) {
                            Text("Set")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            val settings = FolderRepository.loadSettings(currentFolder)
                            if (settings.expirationDate != null) {
                                FolderRepository.saveSettings(currentFolder, settings.copy(expirationDate = null))
                                FeedbackHelper.show(context as Activity, "Timed deletion cleared")
                            }
                            showTimedDeletionDialog = false 
                        }) {
                            Text("Clear")
                        }
                    }
                )
            }

            // Folder Settings Dialog
            if (showFolderSettingsDialog) {
                FolderSettingsDialog(
                    folder = currentFolder,
                    onDismiss = { showFolderSettingsDialog = false },
                    onSave = { photos = FolderRepository.listPhotos(currentFolder) }
                )
            }

            // Lock Folder Passcode Dialog
            if (showLockDialog) {
                SetLockPasscodeDialog(
                    onDismiss = { showLockDialog = false },
                    onSetPasscode = { passcode ->
                        FolderRepository.setPasscode(currentFolder, passcode)
                        FeedbackHelper.show(context as Activity, "Folder locked with passcode")
                        showLockDialog = false
                    }
                )
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmDialog) {
                val count = selectedPhotos.size
                AlertDialog(
                    onDismissRequest = { 
                        showDeleteConfirmDialog = false
                        selectedPhotos = emptySet()
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    title = { Text("Delete Photo") },
                    text = { Text("Are you sure you want to remove ${if (count == 1) "this photo" else "these photos"}?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                var deletedCount = 0
                                selectedPhotos.forEach { file ->
                                    if (file.delete()) deletedCount++
                                }
                                FeedbackHelper.show(context as Activity, "Deleted $deletedCount photo${if (deletedCount == 1) "" else "s"}")
                                selectedPhotos = emptySet()
                                previewPhoto = null
                                photos = FolderRepository.listPhotos(currentFolder)
                                showDeleteConfirmDialog = false
                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showDeleteConfirmDialog = false 
                            selectedPhotos = emptySet()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Move Photos Dialog
            if (showMoveDialog) {
                val availableFolders = remember {
                    FolderRepository.listFolders(context).filter { it.path != currentFolder.absolutePath }
                }
                MovePhotosDialog(
                    availableFolders = availableFolders,
                    onDismiss = { 
                        showMoveDialog = false 
                        selectedPhotos = emptySet()
                    },
                    onMoveToFolder = { destFolderItem ->
                        val destFolder = File(destFolderItem.path)
                        var movedCount = 0
                        selectedPhotos.forEach { file ->
                            val destFile = File(destFolder, file.name)
                            if (file.renameTo(destFile)) {
                                movedCount++
                            }
                        }
                        FeedbackHelper.show(context as Activity, "Moved $movedCount photo${if (movedCount == 1) "" else "s"} to ${destFolderItem.name}")
                        selectedPhotos = emptySet()
                        previewPhoto = null
                        photos = FolderRepository.listPhotos(currentFolder)
                        showMoveDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun FolderSettingsDialog(
    folder: File,
    onDismiss: () -> Unit,
    onSave: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember { FolderRepository.loadSettings(folder) }
    var passcode by remember { mutableStateOf(settings.passcode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Folder Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode (leave empty for none)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                
                if (settings.passcode != null) {
                    TextButton(onClick = { 
                        passcode = ""
                        val updated = settings.copy(passcode = null)
                        FolderRepository.saveSettings(folder, updated)
                        FeedbackHelper.show(context as Activity, "Passcode removed")
                        onDismiss()
                    }) {
                        Text("Remove Passcode", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = settings.copy(passcode = passcode.ifBlank { null })
                    FolderRepository.saveSettings(folder, updated)
                    FeedbackHelper.show(context as Activity, "Settings saved")
                    onSave()
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SetLockPasscodeDialog(
    onDismiss: () -> Unit,
    onSetPasscode: (String) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Lock Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it; error = null },
                    label = { Text("Set Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { confirmPasscode = it; error = null },
                    label = { Text("Confirm Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (passcode.isBlank()) {
                        error = "Passcode cannot be empty"
                    } else if (passcode != confirmPasscode) {
                        error = "Passcodes do not match"
                    } else {
                        onSetPasscode(passcode)
                    }
                }
            ) {
                Text("Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MovePhotosDialog(
    availableFolders: List<FolderItem>,
    onDismiss: () -> Unit,
    onMoveToFolder: (FolderItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Move Photos") },
        text = {
            if (availableFolders.isEmpty()) {
                Text("No other folders available to move photos to.")
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select destination folder:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    availableFolders.forEach { folderItem ->
                        Card(
                            onClick = { onMoveToFolder(folderItem) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(folderItem.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (folderItem.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UnlockFolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Boolean
) {
    var passcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Enter passcode for: $folderName") },
        text = {
            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text("Enter Passcode") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onUnlock(passcode) }) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}