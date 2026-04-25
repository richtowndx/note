package com.videonote.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.videonote.data.local.database.converter.Converters
import com.videonote.data.local.database.dao.ModelDao
import com.videonote.data.local.database.dao.NoteDao
import com.videonote.data.local.database.dao.ProviderDao
import com.videonote.data.local.database.entity.ModelEntity
import com.videonote.data.local.database.entity.NoteEntity
import com.videonote.data.local.database.entity.ProviderEntity

/**
 * 数据库版本1到版本2的迁移
 * 添加TTS相关字段：ttsProgress, ttsSpeechRate, ttsPitch, ttsVoiceId
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE notes ADD COLUMN ttsProgress INTEGER DEFAULT NULL
        """)
        db.execSQL("""
            ALTER TABLE notes ADD COLUMN ttsSpeechRate REAL DEFAULT NULL
        """)
        db.execSQL("""
            ALTER TABLE notes ADD COLUMN ttsPitch REAL DEFAULT NULL
        """)
        db.execSQL("""
            ALTER TABLE notes ADD COLUMN ttsVoiceId TEXT DEFAULT NULL
        """)
    }
}

/**
 * 数据库版本2到版本3的迁移
 * 添加本地文件笔记支持：noteType, filePath, fileName
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 添加笔记类型字段，默认为 VIDEO
        db.execSQL("ALTER TABLE notes ADD COLUMN noteType TEXT DEFAULT 'VIDEO' NOT NULL")
        // 添加本地文件路径字段
        db.execSQL("ALTER TABLE notes ADD COLUMN filePath TEXT DEFAULT NULL")
        // 添加文件名字段
        db.execSQL("ALTER TABLE notes ADD COLUMN fileName TEXT DEFAULT NULL")
        // 为 fileName 字段创建索引，优化文件名查询性能
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_fileName ON notes(fileName)")
    }
}

@Database(
    entities = [NoteEntity::class, ProviderEntity::class, ModelEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VideoNoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao

    companion object {
        /**
         * 获取数据库实例
         * 使用时需要在Application中创建数据库，这里提供getDatabase方法作为参考
         */
        fun getDatabase(context: Context): VideoNoteDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VideoNoteDatabase::class.java,
                "videonote_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}