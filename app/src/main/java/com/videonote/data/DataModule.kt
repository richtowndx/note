package com.videonote.data

import android.content.Context
import androidx.room.Room
import com.videonote.data.local.database.MIGRATION_1_2
import com.videonote.data.local.database.MIGRATION_2_3
import com.videonote.data.local.database.VideoNoteDatabase
import com.videonote.data.local.database.dao.NoteDao
import com.videonote.data.repository.VideoNoteRepositoryImpl
import com.videonote.domain.repository.VideoNoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindVideoNoteRepository(
        videoNoteRepositoryImpl: VideoNoteRepositoryImpl
    ): VideoNoteRepository

    companion object {
        @Provides
        @Singleton
        fun provideVideoNoteDatabase(
            @ApplicationContext context: Context
        ): VideoNoteDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VideoNoteDatabase::class.java,
                "videonote_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideNoteDao(database: VideoNoteDatabase): NoteDao {
            return database.noteDao()
        }
    }
}
