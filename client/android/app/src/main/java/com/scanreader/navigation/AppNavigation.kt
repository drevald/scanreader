package com.scanreader.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanreader.model.Book
import com.scanreader.ui.screens.LibraryScreen
import com.scanreader.ui.screens.ReaderScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {

        composable("library") {
            LibraryScreen(
                onBookOpen = { book ->
                    val encoded = java.net.URLEncoder.encode(json.encodeToString(book), "UTF-8")
                    navController.navigate("reader/$encoded")
                }
            )
        }

        composable("reader/{bookJson}") { backStack ->
            val encoded = backStack.arguments?.getString("bookJson") ?: return@composable
            val book = json.decodeFromString<Book>(
                java.net.URLDecoder.decode(encoded, "UTF-8")
            )
            ReaderScreen(
                book = book,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
