package com.daveboostz.app.di

import android.content.Context
import androidx.room.Room
import com.daveboostz.app.data.DaveBoostzDatabase
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
    fun provideDaveBoostzDatabase(@ApplicationContext context: Context): DaveBoostzDatabase {
        return Room.databaseBuilder(
            context,
            DaveBoostzDatabase::class.java,
            "daveboostz.db"
        ).fallbackToDestructiveMigration().build()
    }
}
