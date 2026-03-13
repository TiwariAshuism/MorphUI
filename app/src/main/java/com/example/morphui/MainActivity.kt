package com.example.morphui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.sdui.di.appModule
import com.app.sdui.presentation.screen.DynamicScreen
import com.app.sdui.presentation.renderer.RendererRegistry
import com.app.sdui.presentation.viewmodel.ScreenViewModel
import com.example.morphui.ui.theme.MorphUITheme
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Koin
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        // Initialize SDUI renderers
        RendererRegistry.init()

        setContent {
            MorphUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: ScreenViewModel = koinViewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            DynamicScreen(
                                screenId = "home",
                                viewModel = viewModel,
                                onNavigate = { route, params ->
                                    navController.navigate(route)
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("profile") {
                            DynamicScreen(
                                screenId = "profile",
                                viewModel = viewModel,
                                onNavigate = { route, params ->
                                    navController.navigate(route)
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
