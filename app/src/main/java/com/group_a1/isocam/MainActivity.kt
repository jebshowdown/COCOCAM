package com.group_a1.isocam


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    // Bumped every onResume so the folder list refreshes after returning from the detail screen.
    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                IsoCamApp(refreshTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }
}

private enum class HomeTab(val label: String, val icon: ImageVector) {
    Folders("Folders", Icons.Default.Home),
    Camera("Camera", Icons.Default.AddCircle),
    Settings("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsoCamApp(refreshTick: Int) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Folders) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(tab.label) }) },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (tab) {
            HomeTab.Folders -> FoldersScreen(refreshTick, content)
            // TODO(teammate): replace with the real camera screen / launch the camera activity
            HomeTab.Camera -> CenteredText("Camera screen goes here", content)
            HomeTab.Settings -> CenteredText("Settings", content)
        }
    }
}

@Composable
private fun FoldersScreen(refreshTick: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var folders by remember { mutableStateOf(emptyList<FolderItem>()) }
    var showDialog by remember { mutableStateOf(false) }

    fun reload() {
        folders = FolderRepository.listFolders(context)
    }

    LaunchedEffect(refreshTick) { reload() }

    Box(modifier.fillMaxSize()) {
        if (folders.isEmpty()) {
            CenteredText("No folders yet.\nTap + to create one.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(folders, key = { it.path }) { folder ->
                    FolderRow(folder) { FolderDetailActivity.start(context, folder.path) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
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
                            null // no error
                        }
                        is CreateResult.Error -> result.message
                    }
                },
            )
        }
    }
}

@Composable
private fun FolderRow(folder: FolderItem, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(folder.name, style = MaterialTheme.typography.titleMedium)
            val n = folder.photoCount
            Text(
                text = "$n photo${if (n == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** onCreate returns an error message to display, or null if the folder was created. */
@Composable
private fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> String?) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
        Text(text, textAlign = TextAlign.Center)
    }
}