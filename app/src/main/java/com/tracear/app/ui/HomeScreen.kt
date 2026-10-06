package com.tracear.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tracear.app.data.ProjectData
import com.tracear.app.data.ProjectRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.widget.Toast
import com.tracear.app.data.AppSettings

/**
 * HomeScreen — Project List & Launch Screen.
 *
 * Shows:
 *  - App header with Settings, Help/Tutorial, and "+ New Project" button
 *  - List of saved projects with thumbnails, names, dates, rename, delete, and corrupted image recovery
 *  - Seamless resume into ARScreen
 */
@Composable
fun HomeScreen(
    settings: AppSettings? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenTutorial: (() -> Unit)? = null,
    onOpenProject: (ProjectData) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ProjectRepository(context) }
    val scope = rememberCoroutineScope()

    val projects = remember { mutableStateListOf<ProjectData>() }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog state
    var projectToRename by remember { mutableStateOf<ProjectData?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<ProjectData?>(null) }
    var missingImageProject by remember { mutableStateOf<ProjectData?>(null) }
    var storageErrorMsg by remember { mutableStateOf<String?>(null) }
    var newProjectUri by remember { mutableStateOf<Uri?>(null) }
    var newProjectName by remember { mutableStateOf("") }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    fun refreshProjects() {
        scope.launch {
            isLoading = true
            projects.clear()
            projects.addAll(repository.listProjects())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshProjects()
    }

    // Photo picker launcher for new projects
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            newProjectUri = uri
            val defaultName = "Drawing " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
            newProjectName = defaultName
            showNewProjectDialog = true
        }
    }

    // Photo picker launcher for replacing missing project images
    val replaceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val p = missingImageProject
        if (uri != null && p != null) {
            scope.launch {
                val success = repository.replaceProjectImage(p, uri)
                missingImageProject = null
                if (success) {
                    refreshProjects()
                    onOpenProject(p)
                } else {
                    Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // --- Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "✏️ TraceAR",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "AR Drawing & Tracing Assistant",
                        fontSize = 12.sp,
                        color = Color(0xFF8B949E)
                    )
                }

                // Header Actions: Tutorial, Settings & New Project
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onOpenTutorial != null) {
                        IconButton(
                            onClick = onOpenTutorial,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF21262D))
                        ) {
                            Text("❓", fontSize = 16.sp)
                        }
                    }

                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF21262D))
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("+ New", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SAVED PROJECTS (${projects.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B949E),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Projects List ---
            if (projects.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33238636)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎨", fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Welcome to TraceAR!",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Place your phone on a cup or stand above paper, select an image, and trace directly with pencil or pen.",
                                color = Color(0xFF8B949E),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Pick an Image to Trace", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            }

                            if (onOpenTutorial != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = onOpenTutorial) {
                                    Text("📖 View Quick 5-Step Guide", color = Color(0xFF58A6FF), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        val thumbFile = repository.getProjectThumbFile(project)
                        ProjectCard(
                            project = project,
                            thumbFile = thumbFile,
                            onOpen = {
                                val imgFile = repository.getProjectImageFile(project)
                                if (!imgFile.exists() || imgFile.length() == 0L) {
                                    missingImageProject = project
                                } else {
                                    onOpenProject(project)
                                }
                            },
                            onRename = {
                                projectToRename = project
                                renameInput = project.name
                            },
                            onDelete = {
                                projectToDelete = project
                            }
                        )
                    }
                }
            }
        }
    }

    // --- New Project Dialog ---
    if (showNewProjectDialog && newProjectUri != null) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("New Project", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a name for this drawing project:", color = Color(0xFFC9D1D9), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF58A6FF),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = newProjectUri ?: return@Button
                        if (!repository.hasEnoughStorageSpace()) {
                            showNewProjectDialog = false
                            storageErrorMsg = "Device storage is low. Please free up space before saving new projects."
                            return@Button
                        }
                        showNewProjectDialog = false
                        scope.launch {
                            val created = repository.createProject(newProjectName, uri)
                            if (created != null) {
                                refreshProjects()
                                onOpenProject(created)
                            } else {
                                storageErrorMsg = "Unable to create project. Please verify file access and available device storage."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                ) {
                    Text("Start Tracing", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel", color = Color(0xFF58A6FF))
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- Missing Image Recovery Dialog ---
    missingImageProject?.let { project ->
        AlertDialog(
            onDismissRequest = { missingImageProject = null },
            title = { Text("⚠️ Image Not Found", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "The drawing image for \"${project.name}\" was removed or could not be found.\n\nChoose a new image to replace it, or remove this project from your list.",
                    color = Color(0xFFC9D1D9),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        replaceImagePickerLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                ) {
                    Text("Pick New Image", color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val p = missingImageProject ?: return@TextButton
                            missingImageProject = null
                            scope.launch {
                                repository.deleteProject(p.id)
                                refreshProjects()
                            }
                        }
                    ) {
                        Text("Delete", color = Color(0xFFDA3633))
                    }
                    TextButton(onClick = { missingImageProject = null }) {
                        Text("Cancel", color = Color(0xFF8B949E))
                    }
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- Storage Error Dialog ---
    storageErrorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { storageErrorMsg = null },
            title = { Text("💾 Storage Error", color = Color(0xFFDA3633), fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = Color(0xFFC9D1D9), fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { storageErrorMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D))
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }


    // --- Rename Dialog ---
    projectToRename?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = projectToRename ?: return@Button
                        projectToRename = null
                        scope.launch {
                            repository.renameProject(p.id, renameInput)
                            refreshProjects()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- Delete Confirmation Dialog ---
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${project.name}\"? This cannot be undone.",
                    color = Color(0xFFC9D1D9),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = projectToDelete ?: return@Button
                        projectToDelete = null
                        scope.launch {
                            repository.deleteProject(p.id)
                            refreshProjects()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDA3633))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = Color(0xFF58A6FF))
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ProjectCard(
    project: ProjectData,
    thumbFile: File,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.lastModified) {
        SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(project.lastModified))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2128)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D1117))
            ) {
                AsyncImage(
                    model = thumbFile,
                    contentDescription = project.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (project.isLinesOnly) {
                        Text(
                            text = "Lines",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3300E5FF))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (project.gridEnabled) {
                        Text(
                            text = "${project.gridCols}×${project.gridRows} Grid (${project.doneCells.size} done)",
                            color = Color(0xFF3FB950),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x333FB950))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quick Actions
            Row {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Text("✏️", fontSize = 14.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Text("🗑️", fontSize = 14.sp)
                }
            }
        }
    }
}
