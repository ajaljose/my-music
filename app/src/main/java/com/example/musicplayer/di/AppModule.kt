package com.example.musicplayer.di

import android.content.Context
import androidx.room.Room
import com.example.musicplayer.data.local.HarmoniaDatabase
import com.example.musicplayer.data.local.dao.PlaybackHistoryDao
import com.example.musicplayer.data.local.dao.PlaylistDao
import com.example.musicplayer.data.local.dao.SongDao
import com.example.musicplayer.data.media.MediaStoreScanner
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.data.repository.MusicRepositoryImpl
import com.example.musicplayer.domain.repository.MusicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHarmoniaDatabase(
        @ApplicationContext context: Context
    ): HarmoniaDatabase {
        return Room.databaseBuilder(
            context,
            HarmoniaDatabase::class.java,
            HarmoniaDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSongDao(database: HarmoniaDatabase): SongDao = database.songDao

    @Provides
    fun providePlaylistDao(database: HarmoniaDatabase): PlaylistDao = database.playlistDao

    @Provides
    fun providePlaybackHistoryDao(database: HarmoniaDatabase): PlaybackHistoryDao = database.playbackHistoryDao

    @Provides
    @Singleton
    fun provideMusicRepository(
        songDao: SongDao,
        playlistDao: PlaylistDao,
        playbackHistoryDao: PlaybackHistoryDao,
        mediaStoreScanner: MediaStoreScanner
    ): MusicRepository {
        return MusicRepositoryImpl(
            songDao = songDao,
            playlistDao = playlistDao,
            playbackHistoryDao = playbackHistoryDao,
            mediaStoreScanner = mediaStoreScanner
        )
    }
}
