package com.app.sdui.di

import com.app.sdui.binding.DataBindingEngine
import com.app.sdui.cache.UICache
import com.app.sdui.analytics.ActionAnalytics
import com.app.sdui.analytics.AnalyticsEventQueue
import com.app.sdui.analytics.BffActionAnalytics
import com.app.sdui.data.remote.FirebaseService
import com.app.sdui.data.remote.bff.ApiActionExecutor
import com.app.sdui.data.remote.bff.BffClient
import com.app.sdui.data.repository.BffScreenRepository
import com.app.sdui.data.repository.FirebaseScreenRepository
import com.app.sdui.data.repository.ScreenRepository
import com.app.sdui.engine.MorphUIConfig
import com.app.sdui.engine.MorphUIEngine
import com.app.sdui.identity.UserIdentity
import com.app.sdui.presentation.viewmodel.ScreenViewModel
import com.app.sdui.registry.ComponentRegistry
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ── Firebase ──
    single { Firebase.database("https://morphui-fdfd5-default-rtdb.firebaseio.com/").reference }
    single { FirebaseService(get()) }

    // ── Go BFF (Phase 4) ──
    // Android emulator should use 10.0.2.2 to reach host localhost.
    single { "http://10.0.2.2:8080/".toHttpUrl() }
    single {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single { BffClient(baseUrl = get(), http = get(), json = get()) }
    single { ApiActionExecutor(baseUrl = get(), http = get(), json = get(), bff = get()) }
    single { AnalyticsEventQueue(androidContext(), get()) }
    single<ActionAnalytics> {
        BffActionAnalytics(
            baseUrl = get(),
            http = get(),
            json = get(),
            userIdProvider = { get<UserIdentity>().userId() },
            queue = get(),
        )
    }
    single { UserIdentity(androidContext()) }

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

    // ── Screen sources ──
    single { FirebaseScreenRepository(get()) }
    single {
        // Dev identity strategy (Phase 4): fixed user id to demonstrate personalization.
        // Replace with auth/session propagation in Phase 7.
        BffScreenRepository(
            bff = get(),
            userIdProvider = { get<UserIdentity>().userId() },
            acceptLanguageProvider = { "en-US" },
        )
    }
    single<ScreenRepository> {
        val bffRepo: BffScreenRepository = get()
        val fbRepo: FirebaseScreenRepository = get()

        // Prefer BFF for supported screens; fallback to Firebase for others.
        object : ScreenRepository {
            override fun observeScreen(screenId: String) =
                if (screenId == "home") bffRepo.observeScreen(screenId) else fbRepo.observeScreen(screenId)
        }
    }

    // ── Presentation ──
    viewModel {
        ScreenViewModel(
            engine = get(),
            screens = get(),
            context = androidContext(),
            apiExecutor = get(),
            analytics = get(),
            userIdProvider = { get<UserIdentity>().userId() },
            acceptLanguageProvider = { "en-US" },
        )
    }
}
