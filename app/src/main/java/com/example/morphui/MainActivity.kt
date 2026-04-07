package com.example.morphui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.sdui.di.appModule
import com.app.sdui.presentation.screen.DynamicScreen
import com.app.sdui.presentation.viewmodel.ScreenViewModel
import com.example.morphui.ui.theme.MorphUITheme
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
                                    navigateFromSdui(navController, route, params)
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
                                    navigateFromSdui(navController, route, params)
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = "details/{contentId}",
                            arguments = listOf(
                                navArgument("contentId") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                            ),
                        ) { entry ->
                            val id = entry.arguments?.getString("contentId").orEmpty()
                            SduiPlaceholderScreen(
                                title = "Details",
                                body = "contentId=$id",
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(
                            route = "play/{contentId}",
                            arguments = listOf(
                                navArgument("contentId") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                            ),
                        ) { entry ->
                            val id = entry.arguments?.getString("contentId").orEmpty()
                            SduiPlaceholderScreen(
                                title = "Play",
                                body = "contentId=$id",
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps SDUI [UIAction.Navigate] routes (often with a leading "/") to registered NavHost routes.
 */
private fun navigateFromSdui(
    navController: NavController,
    route: String,
    params: Map<String, String>?,
) {
    val key = route.trim().removePrefix("/").substringBefore("?").lowercase()
    val id = params?.get("id").orEmpty()

    when (key) {
        "home" -> navController.navigate("home") {
            launchSingleTop = true
        }

        "profile" -> navController.navigate("profile") {
            launchSingleTop = true
        }

        "details" -> {
            val segment = if (id.isNotEmpty()) encodePathSegment(id) else "unknown"
            navController.navigate("details/$segment")
        }

        "play" -> {
            val segment = if (id.isNotEmpty()) encodePathSegment(id) else "unknown"
            navController.navigate("play/$segment")
        }

        else -> {
            android.util.Log.w(
                "MainActivity",
                "SDUI navigate: no matching destination for route=$route params=$params",
            )
        }
    }
}

private fun encodePathSegment(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
}

@Composable
private fun SduiPlaceholderScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = body, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
