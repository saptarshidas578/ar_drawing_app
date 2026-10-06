package com.tracear.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * ProjectRepository — manages persistent project storage in app internal files.
 *
 * Folder structure:
 *   context.filesDir/projects/{projectId}/
 *     ├── project.json      (settings, fit mode, grid, lines, transform)
 *     ├── image.png         (reference image copied into app sandbox)
 *     └── thumb.png         (downscaled 300px thumbnail for fast list rendering)
 */
class ProjectRepository(private val context: Context) {

    companion object {
        private const val TAG = "TraceAR"
        private const val PROJECTS_DIR = "projects"
    }

    private val baseDir: File
        get() = File(context.filesDir, PROJECTS_DIR).apply { if (!exists()) mkdirs() }

    /**
     * Lists all saved projects, sorted newest to oldest.
     */
    suspend fun listProjects(): List<ProjectData> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ProjectData>()
        val projectDirs = baseDir.listFiles { f -> f.isDirectory } ?: return@withContext emptyList()

        for (dir in projectDirs) {
            val jsonFile = File(dir, "project.json")
            if (jsonFile.exists()) {
                try {
                    val jsonStr = jsonFile.readText()
                    val project = ProjectData.fromJson(jsonStr)
                    if (project != null) {
                        list.add(project)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed reading project in ${dir.name}: ${e.message}")
                }
            }
        }
        return@withContext list.sortedByDescending { it.lastModified }
    }

    /**
     * Gets a single project by ID.
     */
    suspend fun getProject(id: String): ProjectData? = withContext(Dispatchers.IO) {
        val dir = File(baseDir, id)
        val jsonFile = File(dir, "project.json")
        if (!jsonFile.exists()) return@withContext null
        return@withContext try {
            ProjectData.fromJson(jsonFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a new project by copying the image into internal sandbox and generating a thumbnail.
     */
    suspend fun createProject(name: String, sourceUri: Uri): ProjectData? = withContext(Dispatchers.IO) {
        try {
            val id = UUID.randomUUID().toString()
            val projectDir = File(baseDir, id).apply { mkdirs() }
            val imageFile = File(projectDir, "image.png")
            val thumbFile = File(projectDir, "thumb.png")

            // 1. Copy source image bytes into internal sandbox
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(imageFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            // 2. Generate and save downscaled 300px thumbnail
            generateThumbnail(imageFile, thumbFile, 300)

            // 3. Create ProjectData and save JSON
            val project = ProjectData(
                id = id,
                name = name.ifBlank { "Drawing Project" },
                lastModified = System.currentTimeMillis()
            )
            saveProject(project)
            return@withContext project

        } catch (e: Exception) {
            Log.e(TAG, "createProject error: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Saves project data atomically (writes to temp file, then renames).
     */
    suspend fun saveProject(project: ProjectData) = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(baseDir, project.id).apply { if (!exists()) mkdirs() }
            val jsonFile = File(projectDir, "project.json")
            val tmpFile = File(projectDir, "project.json.tmp")

            val updatedProject = project.copy(lastModified = System.currentTimeMillis())
            tmpFile.writeText(updatedProject.toJson())

            // Atomic rename
            if (tmpFile.renameTo(jsonFile) || (jsonFile.delete() && tmpFile.renameTo(jsonFile))) {
                Log.i(TAG, "Project saved: ${project.name} (${project.id})")
            } else {
                jsonFile.writeText(updatedProject.toJson())
                tmpFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveProject error: ${e.message}", e)
        }
    }

    /**
     * Renames a project.
     */
    suspend fun renameProject(id: String, newName: String) = withContext(Dispatchers.IO) {
        val project = getProject(id) ?: return@withContext
        saveProject(project.copy(name = newName.ifBlank { "Untitled Project" }))
    }

    /**
     * Deletes a project and all its files.
     */
    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        val projectDir = File(baseDir, id)
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        } else {
            false
        }
    }

    fun getProjectImageFile(project: ProjectData): File =
        File(File(baseDir, project.id), project.imageFileName)

    fun getProjectThumbFile(project: ProjectData): File =
        File(File(baseDir, project.id), project.thumbFileName)

    private fun generateThumbnail(srcFile: File, dstFile: File, maxDim: Int) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(srcFile.absolutePath, bounds)

            var sample = 1
            while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(srcFile.absolutePath, opts) ?: return

            val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height)
            val thumb = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true)
            } else {
                bmp
            }

            FileOutputStream(dstFile).use { out ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (thumb != bmp) thumb.recycle()
            bmp.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "Failed generating thumbnail: ${e.message}")
        }
    }
}
