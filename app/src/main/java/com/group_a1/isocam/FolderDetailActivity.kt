package com.group_a1.isocam

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.group_a1.isocam.screens.CameraActivity
import com.group_a1.isocam.ui.theme.IsoCamTheme
import com.group_a1.isocam.ui.theme.LocalIsoCamExtras
import com.group_a1.isocam.ui.theme.ThemeController
import java.io.File
import java.io.FileOutputStream

class FolderDetailActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)
    private var currentFolderFile: File? = null

    private val pickMultiplePhotosLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
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

        // Save as last opened folder
        getSharedPreferences("isocam_prefs", MODE_PRIVATE)
            .edit()
            .putString("last_opened_folder_path", folder.absolutePath)
            .apply()

        onBackPressedDispatcher.addCallback(this) {
            navigateHome()
        }

        setContent {
            IsoCamTheme {
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
            Toast.makeText(this, "Imported $importedCount photo${if (importedCount == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Failed to share photos", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_FOLDER_PATH = "folder_path"

        fun start(context: Context, folderPath: String) {
            context.startActivity(
                Intent(context, FolderDetailActivity::class.java)
                    .putExtra(EXTRA_FOLDER_PATH, folderPath)
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

    val context = LocalContext.current
    val headerTint = LocalIsoCamExtras.current.headerTint
    val backgroundColor = MaterialTheme.colorScheme.background

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
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            isEditingName = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        // Top vertical gradient header fading from headerTint into background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(headerTint, backgroundColor)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
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
                                    DropdownMenuItem(
                                        text = { Text("Lock Folder") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showLockDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Folder Settings") },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showFolderSettingsDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    FolderNavTab.entries.forEach { tab ->
                        val selected = tab == FolderNavTab.Folders
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
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedPhotos.isEmpty()) {
                    FloatingActionButton(
                        onClick = onImportPhotos,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Import photos")
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (photos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No photos yet.\nTap + to import existing photos.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = ripple(),
                                        onClick = {
                                            if (selectedPhotos.isNotEmpty()) {
                                                selectedPhotos = if (isSelected) selectedPhotos - file else selectedPhotos + file
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
                        Toast.makeText(context, "Folder locked with passcode", Toast.LENGTH_SHORT).show()
                        showLockDialog = false
                    }
                )
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmDialog) {
                val count = selectedPhotos.size
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    title = { Text("Delete $count photo${if (count == 1) "" else "s"}") },
                    text = { Text("Are you sure you want to remove these photos? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                var deletedCount = 0
                                selectedPhotos.forEach { file ->
                                    if (file.delete()) deletedCount++
                                }
                                Toast.makeText(context, "Deleted $deletedCount photo${if (deletedCount == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
                                selectedPhotos = emptySet()
                                photos = FolderRepository.listPhotos(currentFolder)
                                showDeleteConfirmDialog = false
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
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
                    onDismiss = { showMoveDialog = false },
                    onMoveToFolder = { destFolderItem ->
                        val destFolder = File(destFolderItem.path)
                        var movedCount = 0
                        selectedPhotos.forEach { file ->
                            val destFile = File(destFolder, file.name)
                            if (file.renameTo(destFile)) {
                                movedCount++
                            }
                        }
                        Toast.makeText(context, "Moved $movedCount photo${if (movedCount == 1) "" else "s"} to ${destFolderItem.name}", Toast.LENGTH_SHORT).show()
                        selectedPhotos = emptySet()
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode (leave empty for none)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = settings.copy(passcode = passcode.ifBlank { null })
                    FolderRepository.saveSettings(folder, updated)
                    Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
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
