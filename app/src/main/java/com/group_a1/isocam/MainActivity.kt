package com.group_a1.isocam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
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
import com.group_a1.isocam.screens.CameraActivity
import com.group_a1.isocam.ui.theme.DarkBackground
import com.group_a1.isocam.ui.theme.DarkPrimary
import com.group_a1.isocam.ui.theme.DarkSurface
import com.group_a1.isocam.ui.theme.IsoCamTheme
import com.group_a1.isocam.ui.theme.LightBackground
import com.group_a1.isocam.ui.theme.LightPrimary
import com.group_a1.isocam.ui.theme.LightSurface
import com.group_a1.isocam.ui.theme.LocalIsoCamExtras
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
    val headerTint = LocalIsoCamExtras.current.headerTint
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        // Vertical gradient background fading from headerTint to background
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
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    HomeTab.entries.forEach { t ->
                        val selected = t == tab
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
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
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

@Composable
private fun FoldersScreen(refreshTick: Int, paddingValues: PaddingValues) {
    val context = LocalContext.current
    var folders by remember { mutableStateOf(emptyList<FolderItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var folderToUnlock by remember { mutableStateOf<FolderItem?>(null) }

    fun reload() {
        folders = FolderRepository.listFolders(context)
    }

    LaunchedEffect(refreshTick) { reload() }

    val prefs = remember { context.getSharedPreferences("isocam_prefs", Context.MODE_PRIVATE) }
    val lastOpenedPath = prefs.getString("last_opened_folder_path", null)
    val lastOpenedFolder = folders.find { it.path == lastOpenedPath } ?: folders.firstOrNull()

    val topPadding = paddingValues.calculateTopPadding()
    val bottomPadding = paddingValues.calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding)
            ) {
                ScreenHeader(title = "Folders")
                CenteredText("No folders yet.\nTap + to create one.", Modifier.weight(1f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                            HeroFolderCard(folder = hero) {
                                openFolder(context, hero) { folderToUnlock = hero }
                            }
                        }
                    }
                }

                // Recent Folders horizontal LazyRow
                if (folders.size > 1) {
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
                                items(folders, key = { "recent_${it.path}" }) { folder ->
                                    RecentFolderCard(folder = folder) {
                                        openFolder(context, folder) { folderToUnlock = folder }
                                    }
                                }
                            }
                        }
                    }
                }

                // All Folders section
                item {
                    SectionHeader(
                        icon = Icons.Outlined.Folder,
                        title = "All folders"
                    )
                }

                items(folders, key = { it.path }) { folder ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FolderRow(folder) {
                            openFolder(context, folder) { folderToUnlock = folder }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomPadding + 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New folder")
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
                        Toast.makeText(context, "Incorrect passcode", Toast.LENGTH_SHORT).show()
                        false
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

@Composable
private fun HeroFolderCard(folder: FolderItem, onClick: () -> Unit) {
    PressableCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                        Text("📁", style = MaterialTheme.typography.titleLarge)
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
            }
            Spacer(Modifier.height(16.dp))
            val progress = if (folder.photoCount > 0) (folder.photoCount / 50f).coerceIn(0.1f, 1f) else 0f
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

@Composable
private fun RecentFolderCard(folder: FolderItem, onClick: () -> Unit) {
    PressableCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                Text("📁", style = MaterialTheme.typography.headlineMedium)
                if (folder.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
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

@Composable
private fun FolderRow(folder: FolderItem, onClick: () -> Unit) {
    PressableCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📁", style = MaterialTheme.typography.titleLarge)
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
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScreenHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
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
    }

    val (bg, surface, primary) = when (mode) {
        ThemeMode.System -> Triple(LightBackground, LightSurface, LightPrimary)
        ThemeMode.LightCoconut -> Triple(LightBackground, LightSurface, LightPrimary)
        ThemeMode.DarkCoconut -> Triple(DarkBackground, DarkSurface, DarkPrimary)
    }

    PressableCard(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SwatchBox(bg)
                SwatchBox(surface)
                SwatchBox(primary)
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

@Composable
private fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
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
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        color = containerColor,
        interactionSource = interactionSource,
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
        title = { Text("Locked Folder: $folderName") },
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
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
