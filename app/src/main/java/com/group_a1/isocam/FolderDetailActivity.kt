package com.group_a1.isocam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.group_a1.isocam.screens.CameraActivity
import java.io.File

class FolderDetailActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val folder = intent.getStringExtra(EXTRA_FOLDER_PATH)?.let { File(it) }
        if (folder == null || !folder.isDirectory) {
            finish()
            return
        }

        // Pressing system back button inside any folder always routes back home to MainActivity
        onBackPressedDispatcher.addCallback(this) {
            navigateHome()
        }

        setContent {
            MaterialTheme {
                FolderDetailScreen(
                    initialFolder = folder,
                    refreshTick = refreshTick,
                    onBack = { navigateHome() },
                    onCamera = { openCamera(it) },
                    onFolderSettings = { openFolderSettings(it) },
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

    private fun openCamera(folder: File) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra("folder_path", folder.absolutePath)
        }
        startActivity(intent)
    }

    private fun openFolderSettings(folder: File) {
        Toast.makeText(this, "Folder settings for ${folder.name}", Toast.LENGTH_SHORT).show()
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

private enum class FolderNavTab(val label: String, val icon: ImageVector) {
    Folders("Folders", Icons.Default.Home),
    Camera("Camera", Icons.Default.AddCircle),
    Settings("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDetailScreen(
    initialFolder: File,
    refreshTick: Int,
    onBack: () -> Unit,
    onCamera: (File) -> Unit,
    onFolderSettings: (File) -> Unit,
) {
    var currentFolder by remember { mutableStateOf(initialFolder) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember(currentFolder) { mutableStateOf(currentFolder.name) }
    var photos by remember { mutableStateOf(emptyList<File>()) }

    val context = LocalContext.current

    LaunchedEffect(currentFolder, refreshTick) {
        photos = FolderRepository.listPhotos(currentFolder)
    }

    fun commitRename() {
        val trimmed = editedName.trim()
        if (trimmed.isNotBlank() && trimmed != currentFolder.name) {
            when (val result = FolderRepository.renameFolder(context, currentFolder.name, trimmed)) {
                is CreateResult.Success -> {
                    currentFolder = result.folder
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

    Scaffold(
        topBar = {
            TopAppBar(
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
            )
        },
        bottomBar = {
            NavigationBar {
                FolderNavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = (tab == FolderNavTab.Folders),
                        onClick = {
                            when (tab) {
                                FolderNavTab.Folders -> onBack()
                                FolderNavTab.Camera -> onCamera(currentFolder)
                                FolderNavTab.Settings -> onFolderSettings(currentFolder)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos yet")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(photos, key = { it.absolutePath }) { file ->
                    AsyncImage(
                        model = file,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}