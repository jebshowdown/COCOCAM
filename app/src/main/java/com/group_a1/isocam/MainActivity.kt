package com.group_a1.isocam

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
    var sortOption by remember { mutableStateOf(SortOption.NameAsc) }
    var folderMenuTarget by remember { mutableStateOf<FolderItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun reload() {
        folders = FolderRepository.listFolders(context)
        sortedFolders = when (sortOption) {
            SortOption.NameAsc -> folders.sortedBy { it.name.lowercase() }
            SortOption.NameDesc -> folders.sortedByDescending { it.name.lowercase() }
            SortOption.DateDesc -> folders.sortedByDescending { it.lastModified }
            SortOption.DateAsc -> folders.sortedBy { it.lastModified }
        }
        // clear selection of deleted items
        selectedFolders = selectedFolders.filter { s -> folders.any { it.path == s.path } }.toSet()
    }

    LaunchedEffect(refreshTick, sortOption) { reload() }

    val prefs = remember { context.getSharedPreferences("isocam_prefs", Context.MODE_PRIVATE) }
    val lastOpenedPath = prefs.getString("last_opened_folder_path", null)
    val lastOpenedFolder = folders.find { it.path == lastOpenedPath } ?: folders.firstOrNull()
    val recentFolders = folders.sortedByDescending { it.lastModified }

    val topPadding = paddingValues.calculateTopPadding()
    val bottomPadding = paddingValues.calculateBottomPadding()
    val rainbow = LocalIsoCamExtras.current.rainbow

    Box(modifier = Modifier.fillMaxSize()) {
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
                            val uris = ArrayList<Uri>()
                            // Order by sortOption
                            val sortedSelected = sortedFolders.filter { it in selectedFolders }
                            sortedSelected.forEach { folder ->
                                FolderRepository.listPhotos(File(folder.path)).forEach { photoFile ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    uris.add(uri)
                                }
                            }
                            if (uris.isNotEmpty()) {
                                val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/jpeg"
                                    if (uris.size == 1) {
                                        putExtra(Intent.EXTRA_STREAM, uris[0])
                                    } else {
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    }
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Photos"))
                            } else {
                                Toast.makeText(context, "No photos to share", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share selected")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = topPadding)
            )
        }

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
                                onLongClick = {
                                    selectedFolders = if (isSelected) selectedFolders - hero else selectedFolders + hero
                                },
                                onClick = {
                                    if (selectedFolders.isNotEmpty()) {
                                        selectedFolders = if (isSelected) selectedFolders - hero else selectedFolders + hero
                                    } else {
                                        openFolder(context, hero) { folderToUnlock = hero }
                                    }
                                },
                                onMenuClick = { folderMenuTarget = hero }
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
                                        onLongClick = {
                                            selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                        },
                                        onClick = {
                                            if (selectedFolders.isNotEmpty()) {
                                                selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                            } else {
                                                openFolder(context, folder) { folderToUnlock = folder }
                                            }
                                        },
                                        onMenuClick = { folderMenuTarget = folder }
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
                            onLongClick = {
                                selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                            },
                            onClick = {
                                if (selectedFolders.isNotEmpty()) {
                                    selectedFolders = if (isSelected) selectedFolders - folder else selectedFolders + folder
                                } else {
                                    openFolder(context, folder) { folderToUnlock = folder }
                                }
                            },
                            onMenuClick = { folderMenuTarget = folder }
                        )
                    }
                }
            }
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

        folderMenuTarget?.let { folder ->
            DropdownMenu(
                expanded = true,
                onDismissRequest = { folderMenuTarget = null },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        FolderDetailActivity.start(context, folder.path)
                        folderMenuTarget = null
                    }
                )
                if (folder.isLocked) {
                    DropdownMenuItem(
                        text = { Text("Update Passcode") },
                        onClick = {
                            FolderDetailActivity.start(context, folder.path)
                            folderMenuTarget = null
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Passcode") },
                        onClick = {
                            FolderRepository.setPasscode(File(folder.path), "")
                            reload()
                            folderMenuTarget = null
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Lock Folder") },
                        onClick = {
                            FolderDetailActivity.start(context, folder.path)
                            folderMenuTarget = null
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Export Photos") },
                    onClick = {
                        try {
                            val uris = ArrayList<Uri>()
                            FolderRepository.listPhotos(File(folder.path)).forEach { photoFile ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                uris.add(uri)
                            }
                            if (uris.isNotEmpty()) {
                                val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/jpeg"
                                    if (uris.size == 1) {
                                        putExtra(Intent.EXTRA_STREAM, uris[0])
                                    } else {
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    }
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Photos"))
                            } else {
                                Toast.makeText(context, "No photos to export", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to export", Toast.LENGTH_SHORT).show()
                        }
                        folderMenuTarget = null
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        selectedFolders = setOf(folder)
                        showDeleteConfirmDialog = true
                        folderMenuTarget = null
                    }
                )
            }
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
    if (folder.isLocked) {
        onLocked()
    } else {
        FolderDetailActivity.start(context, folder.path)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroFolderCard(folder: FolderItem, onLongClick: () -> Unit, onClick: () -> Unit, onMenuClick: () -> Unit, isSelected: Boolean) {
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
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
private fun RecentFolderCard(folder: FolderItem, onLongClick: () -> Unit, onClick: () -> Unit, onMenuClick: () -> Unit, isSelected: Boolean) {
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
                IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(20.dp))
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
private fun FolderRow(folder: FolderItem, onLongClick: () -> Unit, onClick: () -> Unit, onMenuClick: () -> Unit, isSelected: Boolean) {
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
                if (folder.expirationDate != null) {
                    val timeLeft = folder.expirationDate - System.currentTimeMillis()
                    if (timeLeft > 0) {
                        val hours = timeLeft / (1000 * 60 * 60)
                        val days = hours / 24
                        val displayTime = if (days > 0) "$days days left" else "$hours hours left"
                        Text(
                            text = "Deletes in $displayTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Expired",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
    var sortMenuExpanded by remember { mutableStateOf(false) }

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
                    Text("Sort by", style = MaterialTheme.typography.labelLarge)
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
private fun CenteredText(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
