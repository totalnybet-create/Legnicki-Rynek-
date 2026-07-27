package pl.legnickirynek.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import pl.legnickirynek.app.presentation.AppViewModel
import pl.legnickirynek.app.ui.screens.AddListingScreen
import pl.legnickirynek.app.ui.screens.CategoriesScreen
import pl.legnickirynek.app.ui.screens.EventsScreen
import pl.legnickirynek.app.ui.screens.HomeScreen
import pl.legnickirynek.app.ui.screens.MessagesScreen
import pl.legnickirynek.app.ui.screens.NewsScreen
import pl.legnickirynek.app.ui.screens.ProfileScreen
import pl.legnickirynek.app.ui.screens.StartScreen

private data class AppDestination(val route: String, val label: String, val symbol: String)

private val destinations = listOf(
    AppDestination("home", "Główna", "⌂"),
    AppDestination("categories", "Kategorie", "▦"),
    AppDestination("add", "Dodaj", "+"),
    AppDestination("messages", "Wiadomości", "✉"),
    AppDestination("profile", "Profil", "♙")
)

@Composable
fun LegnickiRynekApp(viewModel: AppViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    var showStart by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1100)
        showStart = false
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionError()
        }
    }

    fun navigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo("home") { saveState = true }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    destinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigate(destination.route) },
                            modifier = Modifier.semantics { contentDescription = destination.label },
                            icon = {
                                Text(
                                    destination.symbol,
                                    fontSize = if (destination.route == "add") 27.sp else 21.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(destination.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() }
                ) {
                    composable("home") {
                        HomeScreen(
                            listings = uiState.listings,
                            onOpenCategories = { navigate("categories") },
                            onOpenProfile = { navigate("profile") },
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }
                    composable("categories") { CategoriesScreen(uiState.listings, viewModel::toggleFavorite) }
                    composable("add") {
                        AddListingScreen { listing ->
                            viewModel.addListing(listing)
                            navigate("home")
                        }
                    }
                    composable("messages") { MessagesScreen() }
                    composable("profile") {
                        ProfileScreen(
                            profile = uiState.profile,
                            listingCount = uiState.listings.count { it.id.startsWith("listing-") },
                            favoriteCount = uiState.listings.count { it.isFavorite },
                            onLogin = viewModel::login,
                            onLogout = viewModel::logout
                        )
                    }
                    composable("news") { NewsScreen() }
                    composable("events") { EventsScreen() }
                }
            }
        }
        AnimatedVisibility(visible = showStart, enter = fadeIn(), exit = fadeOut()) { StartScreen() }
    }
}
