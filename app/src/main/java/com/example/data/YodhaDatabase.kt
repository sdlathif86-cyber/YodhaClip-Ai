package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VideoProjectEntity::class,
        VideoClipEntity::class,
        ExportedDownloadEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class YodhaDatabase : RoomDatabase() {
    abstract fun yodhaDao(): YodhaDao

    companion object {
        @Volatile
        private var INSTANCE: YodhaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exported_downloads` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `contentUri` TEXT NOT NULL,
                        `locationLabel` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `sourceModule` TEXT NOT NULL,
                        `aspectRatio` TEXT NOT NULL,
                        `convertedTo9x16` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): YodhaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YodhaDatabase::class.java,
                    "yodhaclip_studio.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
