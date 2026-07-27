package pl.legnickirynek.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.legnickirynek.app.ui.screens.AddListingScreen
import pl.legnickirynek.app.ui.screens.CategoriesScreen
import pl.legnickirynek.app.ui.screens.HomeScreen
import pl.legnickirynek.app.ui.screens.MessagesScreen
import pl.legnickirynek.app.ui.screens.ProfileScreen

private data class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val destinations = listOf(
    AppDestination("home", "Główna", Icons.Default.Home),
    AppDestination("categories", "Kategorie", Icons.Default.Category),
    AppDestination("add", "Dodaj", Icons.Default.AddCircle),
    AppDestination("messages", "Wiadomości", Icons.Default.ChatBubble),
    AppDestination("profile", "Profil", Icons.Default.Person)
)

@Composable
fun LegnickiRynekApp(appViewModel: AppViewModel = viewModel()) {
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    fun navigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo("home") {
                saveState = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigate(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    listings = uiState.listings,
                    onOpenCategories = { navigate("categories") },
                    onOpenProfile = { navigate("profile") },
                    onToggleFavorite = appViewModel::toggleFavorite
                )
            }
            composable("categories") {
                CategoriesScreen(
                    listings = uiState.listings,
                    onToggleFavorite = appViewModel::toggleFavorite
                )
            }
            composable("add") {
                AddListingScreen(
                    onListingCreated = { listing ->
                        appViewModel.addListing(listing)
                        navigate("home")
                    }
                )
            }
            composable("messages") {
                MessagesScreen()
            }
            composable("profile") {
                ProfileScreen(
                    profile = uiState.profile,
                    listingCount = uiState.listings.count {
                        it.sellerName == uiState.profile.name && uiState.profile.loggedIn
                    },
                    favoriteCount = uiState.listings.count { it.isFavorite },
                    onLogin = appViewModel::login,
                    onLogout = appViewModel::logout
                )
            }
        }
    }
}
