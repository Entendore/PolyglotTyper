package com.example.polyglottyper.di

import android.content.Context
import androidx.room.Room
import com.example.polyglottyper.data.local.GameResultDao
import com.example.polyglottyper.data.local.TypingDatabase
import com.example.polyglottyper.data.repository.AchievementsRepositoryImpl
import com.example.polyglottyper.data.repository.StatsRepositoryImpl
import com.example.polyglottyper.data.repository.UserPreferencesRepositoryImpl
import com.example.polyglottyper.data.repository.WordRepositoryImpl
import com.example.polyglottyper.domain.repository.AchievementsRepository
import com.example.polyglottyper.domain.repository.StatsRepository
import com.example.polyglottyper.domain.repository.UserPreferencesRepository
import com.example.polyglottyper.domain.repository.WordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Real migration chain — a v1 install keeps its history and gains the
     * layoutId column (attributed to QWERTY). No destructive fallback.
     */
    @Provides
    @Singleton
    fun provideTypingDatabase(@ApplicationContext context: Context): TypingDatabase =
        Room.databaseBuilder(context, TypingDatabase::class.java, "polyglot_typer.db")
            .addMigrations(TypingDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideGameResultDao(database: TypingDatabase): GameResultDao =
        database.gameResultDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindWordRepository(impl: WordRepositoryImpl): WordRepository

    @Binds @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds @Singleton
    abstract fun bindAchievementsRepository(impl: AchievementsRepositoryImpl): AchievementsRepository

    @Binds @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository
}
