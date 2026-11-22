package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface YodhaDao {

    @Query("SELECT * FROM video_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<VideoProjectEntity>>

    @Query("SELECT * FROM video_projects WHERE id = :projectId LIMIT 1")
    fun observeProjectById(projectId: Long): Flow<VideoProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity): Long

    @Query("DELETE FROM video_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("SELECT * FROM video_clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<VideoClipEntity>>

    @Query("SELECT * FROM video_clips WHERE projectId = :projectId ORDER BY startMs ASC")
    fun getClipsForProject(projectId: Long): Flow<List<VideoClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: VideoClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<VideoClipEntity>)

    @Update
    suspend fun updateClip(clip: VideoClipEntity)

    @Query("DELETE FROM video_clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: Long)

    @Query("SELECT * FROM exported_downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<ExportedDownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: ExportedDownloadEntity): Long

    @Query("DELETE FROM exported_downloads WHERE id = :downloadId")
    suspend fun deleteDownloadById(downloadId: Long)

    @Query("DELETE FROM exported_downloads")
    suspend fun clearAllDownloads()
}

