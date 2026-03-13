package com.app.sdui.di

import com.app.sdui.data.cache.ScreenCache
import com.app.sdui.data.remote.FirebaseService
import com.app.sdui.data.repository.ScreenRepository
import com.app.sdui.domain.mapper.ComponentMapper
import com.app.sdui.domain.usecase.GetScreenUseCase
import com.app.sdui.presentation.viewmodel.ScreenViewModel
import com.google.firebase.Firebase
import com.google.firebase.database.database
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { Firebase.database("https://morphui-47df0-default-rtdb.asia-southeast1.firebasedatabase.app").reference }
    
    // Data Layer
    single { FirebaseService(get()) }
    single { ScreenCache() }
    single { ComponentMapper() }
    single { ScreenRepository(get(), get(), get()) }
    
    // Domain Layer
    single { GetScreenUseCase(get()) }
    
    // Presentation Layer
    viewModel { ScreenViewModel(get(), androidContext()) }
}
