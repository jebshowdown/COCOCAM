package com.group_a1.isocam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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

        setContent {
            MaterialTheme {
                FolderDetailScreen(
                    folder = folder,
                    refreshTick = refreshTick,
                    onBack = { finish() },
                    onCamera = {openCamera(folder)},
                    onFolderSettings = {openFolderSettings(folder)},
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }
    private fun openCamera(folder: File) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra("folder_path", folder.absolutePath)
        }
        startActivity(intent)
    }

    private fun openFolderSettings(folder: File) {
        //var
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDetailScreen(
    folder: File,
    refreshTick: Int,
    onBack: () -> Unit,
    onCamera: () ->Unit,
    onFolderSettings: () -> Unit,
) {
    var photos by remember { mutableStateOf(emptyList<File>()) }
    var menuOpen by remember {mutableStateOf(false)}
    LaunchedEffect(refreshTick) { photos = FolderRepository.listPhotos(folder) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Folder actions")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ){
                    DropdownMenuItem(
                        text = { Text("Camera")} ,
                        onClick = {
                            menuOpen = false
                            onCamera()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings")},
                        onClick = {
                            menuOpen = false
                            onFolderSettings()
                        }
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
