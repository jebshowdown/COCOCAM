package com.group_a1.isocam

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.group_a1.isocam.screens.CameraActivity
import com.group_a1.isocam.ui.theme.DarkBackground
import com.group_a1.isocam.ui.theme.DarkPrimary
import com.group_a1.isocam.ui.theme.DarkSurface
import com.group_a1.isocam.ui.theme.DolphinBackground
import com.group_a1.isocam.ui.theme.DolphinPrimary
import com.group_a1.isocam.ui.theme.DolphinRainbow
import com.group_a1.isocam.ui.theme.DolphinSecondary
import com.group_a1.isocam.ui.theme.DolphinTertiary
import com.group_a1.isocam.ui.theme.IsoCamTheme
import com.group_a1.isocam.ui.theme.LightBackground
import com.group_a1.isocam.ui.theme.LightPrimary
import com.group_a1.isocam.ui.theme.LightSurface
import com.group_a1.isocam.ui.theme.LocalIsoCamExtras
import com.group_a1.isocam.ui.theme.SparkleBubbleOverlay
import com.group_a1.isocam.ui.theme.ThemeController
import com.group_a1.isocam.ui.theme.ThemeMode
import java.io.File

class MainActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeController.init(this)

        val initialTabStr = intent.getStringExtra("initial_tab")
        val initialTab = if (initialTabStr == "Settings") HomeTab.Settings else HomeTab.Folders

        setContent {
            IsoCamTheme {
                IsoCamApp(refreshTick, initialTab)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }
}

private enum class HomeTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    Folders("Folders", Icons.Filled.Folder, Icons.Outlined.Folder),
    Camera("Camera", Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsoCamApp(refreshTick: Int, initialTab: HomeTab = HomeTab.Folders) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val context = LocalContext.current
    val extras = LocalIsoCamExtras.current
    val sunsetHeader = extras.sunsetHeader
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        // Vertical gradient background fading from sunsetHeader into background over ~260dp
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
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    HomeTab.entries.forEach { t ->
                        val selected = t == tab
                        val glowColor = extras.glow
                        val selectedIconTint = if (selected && ThemeController.mode == ThemeMode.RainbowDolphins && glowColor != Color.Unspecified) {
                            glowColor
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (t == HomeTab.Camera) {
                                    val intent = Intent(context, CameraActivity::class.java)
                                    context.startActivity(intent)
                                } else {
                                    tab = t
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) t.filledIcon else t.outlinedIcon,
                                    contentDescription = t.label
                                )
                            },
                            label = { Text(t.label) },
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
        ) { paddingValues ->
            when (tab) {
                HomeTab.Folders -> FoldersScreen(refreshTick, paddingValues)
                HomeTab.Camera -> CenteredText("Launching camera...", Modifier.padding(paddingValues))
                HomeTab.Settings -> SettingsScreen(paddingValues)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoldersScreen(refreshTick: Int, paddingValues: PaddingValues) {
    val context = LocalContext.current
    var folders by remember { mutableStateOf(emptyList<FolderItem>()) }
    var sortedFolders by remember { mutableStateOf(emptyList<FolderItem>()) }
    var selectedFolders by remember { mutableStateOf(setOf<FolderItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var folderToUnlock by remember { mutableStateOf<FolderItem?>(null) }
    var folderToRename by remember { mutableStateOf<FolderItem?>(null) }
    var folderToLock by remember { mutableStateOf<FolderItem?>(null) }
    var folderToUpdatePasscode by remember { mutableStateOf<FolderItem?>(null) }
    var folderToRemovePasscode by remember { mutableStateOf<FolderItem?>(null) }
    var folderToTimedDeletion by remember { mutableStateOf<FolderItem?>(null) }
    var folderToAuthenticate by remember { mutableStateOf<FolderItem?>(null) }
    var pendingLockedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    var sortOption by remember { mutableStateOf(SortOption.NameAsc) }
    var folderMenuTarget by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun reload() {
        folders = FolderRepository.listFolders(context)
        sortedFolders = when (sortOption) {
            SortOption.NameAsc -> folders.sortedBy { it.name.lowercase() }
            SortOption.NameDesc -> folders.sortedByDescending { it.name.lowercase() }
            SortOption.DateDesc -> folders.sortedByDescending { it.lastModified }
            SortOption.DateAsc -> folders.sortedBy { it.lastModified }
        }
        // clear selection of deleted or locked items
        selectedFolders = selectedFolders.filter { s -> folders.any { it.path == s.path && !it.isLocked } }.toSet()
    }

    LaunchedEffect(refreshTick, sortOption) { reload() }

    val prefs = remember { context.getSharedPreferences("isocam_prefs", Context.MODE_PRIVATE) }
    val lastOpenedPath = prefs.getString("last_opened_folder_path", null)
    val lastOpenedFolder = folders.find { it.path == lastOpenedPath } ?: folders.firstOrNull()
    val recentFolders = folders.sortedByDescending { it.lastModified }

    val topPadding = paddingValues.calculateTopPadding()
    val bottomPadding = paddingValues.calculateBottomPadding()
    val rainbow = LocalIsoCamExtras.current.rainbow

    fun exportPhotosToPdf(folder: FolderItem) {
        val pdfFile = FolderRepository.exportFolderToPdf(context, File(folder.path))
        if (pdfFile != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export PDF"))
        } else {
            Toast.makeText(context, "No photos to export into PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun executeWithPasscodeProtection(folder: FolderItem, action: () -> Unit) {
        if (FolderRepository.isLocked(File(folder.path))) {
            folderToAuthenticate = folder
            pendingLockedAction = action
        } else {
            action()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding)
            ) {
                ScreenHeader(title = "Folders")
                if (ThemeController.mode == ThemeMode.RainbowDolphins) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Text("✨ 🫧 🐬 🌊 ✨", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No folders yet.\nTap + to create one.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    CenteredText("No folders yet.\nTap + to create one.", Modifier.weight(1f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = if (selectedFolders.isNotEmpty()) 64.dp else 0.dp),
                contentPadding = PaddingValues(
                    top = topPadding + 4.dp,
                    bottom = bottomPadding + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ScreenHeader(title = "Folders")
                }

                // Hero Card
                lastOpenedFolder?.let { hero ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader(
                                icon = Icons.Filled.History,
                                title = "Last opened folder"
                            )
                            Spacer(Modifier.height(8.dp))
                            val isSelected = hero in selectedFolders
                            HeroFolderCard(
                                folder = hero,
                                isSelected = isSelected,
                                isMenuExpanded = (folderMenuTarget == "hero_${hero.path}"),
                                onLongClick = {
                                    if (hero.isLocked) {
                                        Toast.makeText(context, "Locked folder cannot be selected", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedFolders = if (isSelected) selectedFolders - hero else selectedFolders + hero
                                    }
                                },
                                onClick = {
                                    if (selectedFolders.isNotEmpty()) {
                                        if (!hero.isLocked) {
                                            selectedFolders = if (isSelected) selectedFolders - hero else selectedFolders + hero
                                        }
                                    } else {
                                        openFolder(context, hero) { folderToUnlock = hero }
                                    }
                                },
                                onMenuClick = { folderMenuTarget = "hero_${hero.path}" },
                                onDismissMenu = { folderMenuTarget = null },
                                onRename = {
                                    folderMenuTarget = null
                                    executeWithPasscodeProtection(hero) { folderToRename = hero }
                                },
                                onLock = {
                                    folderMenuTarget = null
                                    folderToLock = hero
                                },
                                onUpdatePasscode = {
                                    folderMenuTarget = null
                                    folderToUpdatePasscode = hero
                                },
                                onRemovePasscode = {
                                    folderMenuTarget = null
                                    folderToRemovePasscode = hero
                                },
                                onTimedDeletion = {
                                    folderMenuTarget = null
                                    executeWithPasscodeProtection(hero) { folderToTimedDeletion = hero }
                                },
                                onExport = {
                                    folderMenuTarget = null
                                    executeWithPasscodeProtection(hero) { exportPhotosToPdf(hero) }
                                },
                                onDelete = {
                                    folderMenuTarget = null
                                    executeWithPasscodeProtection(hero) {
                                        selectedFolders = setOf(hero)
                                        showDeleteConfirmDialog = true
                                    }
                                }
                            )
                        }
                    }
                }

                // Recent Folders horizontal LazyRow
                if (recentFolders.size > 1) {
                    item {
                        Column {
                            SectionHeader(
                                icon = Icons.Filled.Folder,
                                title = "Recent folders"
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recentFolders, key = { "recent_${it.path}" }) { folder ->
                                    val isSelected = folder in selectedFolders
                                    RecentFolderCard(
                                        folder = folder,
                                        isSelected = isSelected,
                                        isMenuExpanded = (folderMenuTarget == "recent_${folder.path}"),
                                        onLongClick = {
                                            if (folder.isLocked) {
                                                Toast.makeText(context, "Locked folder cannot be selected", Toast.LENGTH_SHORT).show()
                                            } else {
                                                selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                            }
                                        },
                                        onClick = {
                                            if (selectedFolders.isNotEmpty()) {
                                                if (!folder.isLocked) {
                                                    selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                                }
                                            } else {
                                                openFolder(context, folder) { folderToUnlock = folder }
                                            }
                                        },
                                        onMenuClick = { folderMenuTarget = "recent_${folder.path}" },
                                        onDismissMenu = { folderMenuTarget = null },
                                        onRename = {
                                            folderMenuTarget = null
                                            executeWithPasscodeProtection(folder) { folderToRename = folder }
                                        },
                                        onLock = {
                                            folderMenuTarget = null
                                            folderToLock = folder
                                        },
                                        onUpdatePasscode = {
                                            folderMenuTarget = null
                                            folderToUpdatePasscode = folder
                                        },
                                        onRemovePasscode = {
                                            folderMenuTarget = null
                                            folderToRemovePasscode = folder
                                        },
                                        onTimedDeletion = {
                                            folderMenuTarget = null
                                            executeWithPasscodeProtection(folder) { folderToTimedDeletion = folder }
                                        },
                                        onExport = {
                                            folderMenuTarget = null
                                            executeWithPasscodeProtection(folder) { exportPhotosToPdf(folder) }
                                        },
                                        onDelete = {
                                            folderMenuTarget = null
                                            executeWithPasscodeProtection(folder) {
                                                selectedFolders = setOf(folder)
                                                showDeleteConfirmDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // All Folders section
                item {
                    SectionHeader(
                        icon = Icons.Outlined.Folder,
                        title = "All folders",
                        sortOption = sortOption,
                        onSortChanged = { sortOption = it }
                    )
                }

                items(sortedFolders, key = { it.path }) { folder ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        val isSelected = folder in selectedFolders
                        FolderRow(
                            folder = folder,
                            isSelected = isSelected,
                            isMenuExpanded = (folderMenuTarget == "all_${folder.path}"),
                            onLongClick = {
                                if (folder.isLocked) {
                                    Toast.makeText(context, "Locked folder cannot be selected", Toast.LENGTH_SHORT).show()
                                } else {
                                    selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                }
                            },
                            onClick = {
                                if (selectedFolders.isNotEmpty()) {
                                    if (!folder.isLocked) {
                                        selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                    }
                                } else {
                                    openFolder(context, folder) { folderToUnlock = folder }
                                }
                            },
                            onMenuClick = { folderMenuTarget = "all_${folder.path}" },
                            onDismissMenu = { folderMenuTarget = null },
                            onRename = {
                                folderMenuTarget = null
                                executeWithPasscodeProtection(folder) { folderToRename = folder }
                            },
                            onLock = {
                                folderMenuTarget = null
                                folderToLock = folder
                            },
                            onUpdatePasscode = {
                                folderMenuTarget = null
                                folderToUpdatePasscode = folder
                            },
                            onRemovePasscode = {
                                folderMenuTarget = null
                                folderToRemovePasscode = folder
                            },
                            onTimedDeletion = {
                                folderMenuTarget = null
                                executeWithPasscodeProtection(folder) { folderToTimedDeletion = folder }
                            },
                            onExport = {
                                folderMenuTarget = null
                                executeWithPasscodeProtection(folder) { exportPhotosToPdf(folder) }
                            },
                            onDelete = {
                                folderMenuTarget = null
                                executeWithPasscodeProtection(folder) {
                                    selectedFolders = setOf(folder)
                                    showDeleteConfirmDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }

        // Batch Action Contextual Top Bar (Positions right at top without gap)
        if (selectedFolders.isNotEmpty()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                navigationIcon = {
                    IconButton(onClick = { selectedFolders = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear selection")
                    }
                },
                title = {
                    Text("${selectedFolders.size} selected", style = MaterialTheme.typography.titleMedium)
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            val sortedSelected = sortedFolders.filter { it in selectedFolders }
                            sortedSelected.forEach { folder ->
                                exportPhotosToPdf(folder)
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share selected")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
            )
        }

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
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomPadding + 16.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(fabBrush)
                    .clickable { showDialog = true }
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New folder", tint = Color.White)
            }
        } else {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomPadding + 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New folder")
            }
        }

        if (showDialog) {
            NewFolderDialog(
                onDismiss = { showDialog = false },
                onCreate = { raw ->
                    when (val result = FolderRepository.createFolder(context, raw)) {
                        is CreateResult.Success -> {
                            reload()
                            showDialog = false
                            null
                        }
                        is CreateResult.Error -> result.message
                    }
                }
            )
        }

        folderToRename?.let { folder ->
            RenameFolderDialog(
                folderName = folder.name,
                onDismiss = { folderToRename = null },
                onRename = { newName ->
                    when (val result = FolderRepository.renameFolder(context, folder.name, newName)) {
                        is CreateResult.Success -> {
                            reload()
                            folderToRename = null
                            null
                        }
                        is CreateResult.Error -> result.message
                    }
                }
            )
        }

        folderToLock?.let { folder ->
            SetLockPasscodeDialog(
                folderName = folder.name,
                onDismiss = { folderToLock = null },
                onSetPasscode = { passcode ->
                    FolderRepository.setPasscode(File(folder.path), passcode)
                    Toast.makeText(context, "Folder locked", Toast.LENGTH_SHORT).show()
                    reload()
                    folderToLock = null
                }
            )
        }

        folderToUpdatePasscode?.let { folder ->
            UpdatePasscodeDialog(
                folderName = folder.name,
                onDismiss = { folderToUpdatePasscode = null },
                onUpdatePasscode = { oldCode, newCode ->
                    val folderFile = File(folder.path)
                    if (!FolderRepository.verifyPasscode(folderFile, oldCode)) {
                        false
                    } else {
                        FolderRepository.setPasscode(folderFile, newCode)
                        Toast.makeText(context, "Passcode updated", Toast.LENGTH_SHORT).show()
                        reload()
                        folderToUpdatePasscode = null
                        true
                    }
                }
            )
        }

        folderToRemovePasscode?.let { folder ->
            RemovePasscodeDialog(
                folderName = folder.name,
                onDismiss = { folderToRemovePasscode = null },
                onRemovePasscode = { passcode ->
                    val folderFile = File(folder.path)
                    if (!FolderRepository.verifyPasscode(folderFile, passcode)) {
                        false
                    } else {
                        FolderRepository.setPasscode(folderFile, "")
                        Toast.makeText(context, "Passcode removed", Toast.LENGTH_SHORT).show()
                        reload()
                        folderToRemovePasscode = null
                        true
                    }
                }
            )
        }

        folderToTimedDeletion?.let { folder ->
            TimedDeletionDialog(
                folder = folder,
                onDismiss = { folderToTimedDeletion = null },
                onSetTimedDeletion = { expDate ->
                    val folderFile = File(folder.path)
                    val settings = FolderRepository.loadSettings(folderFile)
                    FolderRepository.saveSettings(folderFile, settings.copy(expirationDate = expDate))
                    reload()
                }
            )
        }

        folderToAuthenticate?.let { folder ->
            UnlockFolderDialog(
                folderName = folder.name,
                onDismiss = {
                    folderToAuthenticate = null
                    pendingLockedAction = null
                },
                onUnlock = { passcode ->
                    if (FolderRepository.verifyPasscode(File(folder.path), passcode)) {
                        val action = pendingLockedAction
                        folderToAuthenticate = null
                        pendingLockedAction = null
                        action?.invoke()
                        true
                    } else {
                        FeedbackHelper.show(context as Activity, "Incorrect passcode")
                        false
                    }
                }
            )
        }

        folderToUnlock?.let { folder ->
            UnlockFolderDialog(
                folderName = folder.name,
                onDismiss = { folderToUnlock = null },
                onUnlock = { passcode ->
                    if (FolderRepository.verifyPasscode(File(folder.path), passcode)) {
                        folderToUnlock = null
                        FolderDetailActivity.start(context, folder.path)
                        true
                    } else {
                        FeedbackHelper.show(context as Activity, "Incorrect passcode")
                        false
                    }
                }
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Folders") },
                text = { Text("Are you sure you want to delete ${selectedFolders.size} folder(s)?") },
                confirmButton = {
                    TextButton(onClick = {
                        selectedFolders.forEach { folder ->
                            File(folder.path).deleteRecursively()
                        }
                        selectedFolders = emptySet()
                        showDeleteConfirmDialog = false
                        reload()
                    }) {
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
    }
}

private fun openFolder(context: Context, folder: FolderItem, onLocked: () -> Unit) {
    if (FolderRepository.isLocked(File(folder.path))) {
        onLocked()
    } else {
        FolderDetailActivity.start(context, folder.path)
    }
}

@Composable
private fun FolderOptionsMenu(
    folder: FolderItem,
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onLock: () -> Unit,
    onUpdatePasscode: () -> Unit,
    onRemovePasscode: () -> Unit,
    onTimedDeletion: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = onRename
        )
        if (folder.isLocked) {
            DropdownMenuItem(
                text = { Text("Update Passcode") },
                onClick = onUpdatePasscode
            )
            DropdownMenuItem(
                text = { Text("Remove Passcode") },
                onClick = onRemovePasscode
            )
        } else {
            DropdownMenuItem(
                text = { Text("Lock Folder") },
                onClick = onLock
            )
        }
        DropdownMenuItem(
            text = { Text("Timed Deletion") },
            onClick = onTimedDeletion
        )
        DropdownMenuItem(
            text = { Text("Export Photos as PDF") },
            onClick = onExport
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = onDelete
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroFolderCard(
    folder: FolderItem,
    isSelected: Boolean,
    isMenuExpanded: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onLock: () -> Unit,
    onUpdatePasscode: () -> Unit,
    onRemovePasscode: () -> Unit,
    onTimedDeletion: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val glowColor = LocalIsoCamExtras.current.glow
    val rainbow = LocalIsoCamExtras.current.rainbow

    Box(modifier = Modifier.fillMaxWidth()) {
        if (glowColor != Color.Unspecified) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
        }

        PressableCard(
            onClick = onClick,
            onLongClick = onLongClick,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (folder.isLocked) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked folder",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        val n = folder.photoCount
                        Text(
                            text = "$n photo${if (n == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        FolderOptionsMenu(
                            folder = folder,
                            isExpanded = isMenuExpanded,
                            onDismiss = onDismissMenu,
                            onRename = onRename,
                            onLock = onLock,
                            onUpdatePasscode = onUpdatePasscode,
                            onRemovePasscode = onRemovePasscode,
                            onTimedDeletion = onTimedDeletion,
                            onExport = onExport,
                            onDelete = onDelete
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val progress = if (folder.photoCount > 0) (folder.photoCount / 50f).coerceIn(0.1f, 1f) else 0f
                if (rainbow.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(rainbow))
                        )
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentFolderCard(
    folder: FolderItem,
    isSelected: Boolean,
    isMenuExpanded: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onLock: () -> Unit,
    onUpdatePasscode: () -> Unit,
    onRemovePasscode: () -> Unit,
    onTimedDeletion: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    PressableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = MaterialTheme.shapes.medium,
        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(160.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary)
                if (folder.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(20.dp))
                    }
                    FolderOptionsMenu(
                        folder = folder,
                        isExpanded = isMenuExpanded,
                        onDismiss = onDismissMenu,
                        onRename = onRename,
                        onLock = onLock,
                        onUpdatePasscode = onUpdatePasscode,
                        onRemovePasscode = onRemovePasscode,
                        onTimedDeletion = onTimedDeletion,
                        onExport = onExport,
                        onDelete = onDelete
                    )
                }
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val n = folder.photoCount
            Text(
                text = "$n photo${if (n == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    folder: FolderItem,
    isSelected: Boolean,
    isMenuExpanded: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onLock: () -> Unit,
    onUpdatePasscode: () -> Unit,
    onRemovePasscode: () -> Unit,
    onTimedDeletion: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    PressableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = MaterialTheme.shapes.medium,
        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (folder.isLocked) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked folder",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                val n = folder.photoCount
                Text(
                    text = "$n photo${if (n == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val timeText = FolderRepository.formatTimedDeletion(folder.expirationDate)
                if (timeText != null) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                FolderOptionsMenu(
                    folder = folder,
                    isExpanded = isMenuExpanded,
                    onDismiss = onDismissMenu,
                    onRename = onRename,
                    onLock = onLock,
                    onUpdatePasscode = onUpdatePasscode,
                    onRemovePasscode = onRemovePasscode,
                    onTimedDeletion = onTimedDeletion,
                    onExport = onExport,
                    onDelete = onDelete
                )
            }
        }
    }
}

enum class SortOption(val label: String) {
    NameAsc("Name (A-Z)"), 
    NameDesc("Name (Z-A)"), 
    DateDesc("Recently Edited Date"), 
    DateAsc("Recently Edited Date (Reverse)")
}

@Composable
private fun ScreenHeader(title: String) {
    val rainbow = LocalIsoCamExtras.current.rainbow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (rainbow.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(rainbow))
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    sortOption: SortOption? = null,
    onSortChanged: ((SortOption) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (sortOption != null && onSortChanged != null) {
            Box {
                TextButton(onClick = { sortMenuExpanded = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sort by", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Sort options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSortChanged(option)
                                sortMenuExpanded = false
                            },
                            trailingIcon = if (option == sortOption) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val currentMode = ThemeController.mode

    val topPadding = paddingValues.calculateTopPadding()
    val bottomPadding = paddingValues.calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding + 4.dp,
            bottom = bottomPadding + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(title = "Settings")
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance"
                )
                Spacer(Modifier.height(12.dp))

                ThemeMode.entries.forEach { mode ->
                    ThemePreviewCard(
                        mode = mode,
                        isSelected = mode == currentMode,
                        onSelect = { ThemeController.set(context, mode) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    mode: ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val title = when (mode) {
        ThemeMode.System -> "System Default"
        ThemeMode.LightCoconut -> "Light Coconut"
        ThemeMode.DarkCoconut -> "Dark Coconut"
        ThemeMode.RainbowDolphins -> "Rainbow Dolphins"
    }

    val swatches = when (mode) {
        ThemeMode.System -> listOf(LightBackground, LightPrimary, LightSurface)
        ThemeMode.LightCoconut -> listOf(LightBackground, LightPrimary, LightSurface)
        ThemeMode.DarkCoconut -> listOf(DarkBackground, DarkPrimary, DarkSurface)
        ThemeMode.RainbowDolphins -> listOf(DolphinBackground, DolphinPrimary, DolphinSecondary, DolphinTertiary)
    }

    PressableCard(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    swatches.forEach { color ->
                        SwatchBox(color)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (mode == ThemeMode.RainbowDolphins) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(DolphinRainbow))
                )
            }
        }
    }
}

@Composable
private fun SwatchBox(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PressableCard(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = containerColor,
        border = border,
        content = { Column(content = content) }
    )
}

@Composable
private fun RenameFolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> String?
) {
    var name by remember { mutableStateOf(folderName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Rename Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Folder Name") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { msg -> { Text(msg) } },
            )
        },
        confirmButton = { TextButton(onClick = { error = onRename(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun UpdatePasscodeDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onUpdatePasscode: (String, String) -> Boolean
) {
    var oldPasscode by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Update Passcode for: $folderName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPasscode,
                    onValueChange = { oldPasscode = it; error = null },
                    label = { Text("Old Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPasscode,
                    onValueChange = { newPasscode = it; error = null },
                    label = { Text("New Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { confirmPasscode = it; error = null },
                    label = { Text("Confirm New Passcode") },
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
                    if (oldPasscode.isBlank()) {
                        error = "Old passcode required"
                    } else if (newPasscode.isBlank()) {
                        error = "New passcode cannot be empty"
                    } else if (newPasscode != confirmPasscode) {
                        error = "New passcodes do not match"
                    } else if (!onUpdatePasscode(oldPasscode, newPasscode)) {
                        error = "Incorrect old passcode"
                    }
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
private fun RemovePasscodeDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onRemovePasscode: (String) -> Boolean
) {
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Remove Passcode for: $folderName") },
        text = {
            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it; error = null },
                label = { Text("Current Passcode") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = error != null,
                supportingText = error?.let { msg -> { Text(msg) } }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (passcode.isBlank()) {
                        error = "Passcode required"
                    } else if (!onRemovePasscode(passcode)) {
                        error = "Incorrect passcode"
                    }
                }
            ) {
                Text("Verify & Clear")
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

@Composable
private fun SetLockPasscodeDialog(
    folderName: String,
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
        title = { Text("Lock Folder: $folderName") },
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
private fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> String?) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Folder name") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { msg -> { Text(msg) } },
            )
        },
        confirmButton = { TextButton(onClick = { error = onCreate(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimedDeletionDialog(
    folder: FolderItem,
    onDismiss: () -> Unit,
    onSetTimedDeletion: (Long?) -> Unit
) {
    var selectedOption by remember { mutableStateOf("1 hour") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Timed Deletion for: ${folder.name}") },
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
                                            onSetTimedDeletion(c.timeInMillis)
                                            Toast.makeText(context, "Folder will delete on ${c.time}", Toast.LENGTH_SHORT).show()
                                            onDismiss()
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
                            onClick = null
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
                    onSetTimedDeletion(System.currentTimeMillis() + offset)
                    Toast.makeText(context, "Timed deletion set", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSetTimedDeletion(null)
                Toast.makeText(context, "Timed deletion cleared", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) {
                Text("Clear")
            }
        }
    )
}

@Composable
private fun CenteredText(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
