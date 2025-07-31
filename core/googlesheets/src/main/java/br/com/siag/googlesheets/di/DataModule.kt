package br.com.siag.googlesheets.di

import br.com.siag.googlesheets.data.SpreadSheetRepository
import br.com.siag.googlesheets.data.SpreadSheetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun provideSpreadSheetRepository(impl: SpreadSheetRepositoryImpl): SpreadSheetRepository
}