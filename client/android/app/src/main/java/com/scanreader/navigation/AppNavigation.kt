package com.scanreader.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanreader.model.Book
import com.scanreader.ui.screens.LibraryScreen
import com.scanreader.ui.screens.OpenFileScreen
import com.scanreader.ui.screens.ReaderScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun AppNavigation(openUri: Uri? = null) {
    val navController = rememberNavController()
    val startDestination = if (openUri != null) "open" else "library"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("library") {
            LibraryScreen(
                onBookOpen = { book ->
                    val encoded = java.net.URLEncoder.encode(json.encodeToString(book), "UTF-8")
                    navController.navigate("reader/$encoded")
                }
            )
        }

        composable("open") {
            OpenFileScreen(uri = openUri!!, navController = navController)
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
