package com.example.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow

class YodhaRepository(
    private val dao: YodhaDao,
    private val settingsStore: SettingsDataStore,
    private val mediaInspector: VideoMediaInspector,
    private val storageManager: StudioStorageManager
) {
    val allProjects: Flow<List<VideoProjectEntity>> = dao.getAllProjects()
    val allClips: Flow<List<VideoClipEntity>> = dao.getAllClips()
    val allDownloads: Flow<List<ExportedDownloadEntity>> = dao.getAllDownloads()
    val settings: Flow<StudioSettings> = settingsStore.settingsFlow

    fun observeProject(projectId: Long): Flow<VideoProjectEntity?> =
        dao.observeProjectById(projectId)

    fun observeClipsForProject(projectId: Long): Flow<List<VideoClipEntity>> =
        dao.getClipsForProject(projectId)

    suspend fun inspectVideo(uri: Uri, takePersistablePermission: Boolean): InspectedVideoInfo =
        mediaInspector.inspectVideo(uri, takePersistablePermission)

    suspend fun extractFrameThumbnail(videoUri: String, timeMs: Long): String? =
        mediaInspector.extractFrameThumbnail(videoUri, timeMs)

    suspend fun saveProjectWithClips(
        project: VideoProjectEntity,
        clips: List<VideoClipEntity>
    ): Long {
        val projectId = dao.insertProject(project)
        if (clips.isNotEmpty()) {
            val linkedClips = clips.map { clip ->
                clip.copy(projectId = projectId, projectTitle = project.title)
            }
            dao.insertClips(linkedClips)
        }
        return projectId
    }

    suspend fun addClip(clip: VideoClipEntity): Long = dao.insertClip(clip)

    suspend fun updateClip(clip: VideoClipEntity) = dao.updateClip(clip)

    suspend fun deleteClip(clipId: Long) = dao.deleteClipById(clipId)

    suspend fun deleteProject(projectId: Long) = dao.deleteProjectById(projectId)

    suspend fun recordDownload(download: ExportedDownloadEntity): Long =
        dao.insertDownload(download)

    suspend fun deleteDownload(downloadId: Long) = dao.deleteDownloadById(downloadId)

    suspend fun clearAllDownloads() = dao.clearAllDownloads()

    suspend fun setExportQuality(option: ExportQualityOption) =
        settingsStore.setExportQuality(option)

    suspend fun setDefaultAspectRatio(option: AspectRatioOption) =
        settingsStore.setDefaultAspectRatio(option)

    suspend fun setSmartClipSeconds(seconds: Int) =
        settingsStore.setSmartClipSeconds(seconds)

    suspend fun setDefaultCaptionStyle(option: CaptionStyleOption) =
        settingsStore.setDefaultCaptionStyle(option)

    suspend fun setDefaultLanguageCode(code: String) =
        settingsStore.setDefaultLanguageCode(code)

    suspend fun setAutoConvert9x16Export(enabled: Boolean) =
        settingsStore.setAutoConvert9x16Export(enabled)

    suspend fun setShowBeginnerTips(enabled: Boolean) =
        settingsStore.setShowBeginnerTips(enabled)

    suspend fun setLoopClipPreviews(enabled: Boolean) =
        settingsStore.setLoopClipPreviews(enabled)

    suspend fun resetSettings() = settingsStore.resetDefaults()

    suspend fun getCacheSizeBytes(): Long = mediaInspector.getCacheSizeBytes()

    suspend fun clearThumbnailCache(): Long = mediaInspector.clearThumbnailCache()

    suspend fun inspectStorageBreakdown(recordedDownloadsBytes: Long): StudioStorageBreakdown =
        storageManager.inspectStorage(recordedDownloadsBytes)

    suspend fun clearLiveBufferStorage(): Long = storageManager.clearLiveBuffer()

    suspend fun clearExportTempStorage(): Long = storageManager.clearExportTemp()

    suspend fun clearAllStudioCaches(): Long = storageManager.clearAllStudioCaches()
}
