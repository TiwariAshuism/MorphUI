package com.app.sdui.di

import com.app.sdui.binding.DataBindingEngine
import com.app.sdui.cache.UICache
import com.app.sdui.data.remote.FirebaseService
import com.app.sdui.engine.MorphUIConfig
import com.app.sdui.engine.MorphUIEngine
import com.app.sdui.presentation.viewmodel.ScreenViewModel
import com.app.sdui.registry.ComponentRegistry
import com.google.firebase.Firebase
import com.google.firebase.database.database
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ── Firebase ──
    single { Firebase.database("https://morphui-fdfd5-default-rtdb.firebaseio.com/").reference }
    single { FirebaseService(get()) }

    // ── MorphUI Engine ──
    single { ComponentRegistry() }
    single { DataBindingEngine() }
    single { UICache(androidContext()) }
    single {
        MorphUIConfig(
            minSupportedVersion = 1,
            maxSupportedVersion = 10,
            validateSchema = true,
            strictValidation = false,
            debugMode = true,  // Set to false for production
        )
    }
    single {
        MorphUIEngine(
            registry = get(),
            cache = get(),
            bindingEngine = get(),
            config = get(),
        )
    }

    // ── Presentation ──
    viewModel { ScreenViewModel(get(), get(), androidContext()) }
}
