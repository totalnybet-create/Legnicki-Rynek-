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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile
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
fun LegnickiRynekApp() {
    val context = LocalContext.current.applicationContext
    val navController = rememberNavController()
    val listings = remember(context) {
        val savedListings = LocalStore.loadListings(context)
        mutableStateListOf<Listing>().apply {
            addAll(savedListings.ifEmpty { SampleData.listings })
        }
    }
    var profile by remember(context) {
        mutableStateOf(LocalStore.loadProfile(context))
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    fun persistListings() {
        LocalStore.saveListings(context, listings)
    }

    fun toggleFavorite(id: String) {
        val index = listings.indexOfFirst { it.id == id }
        if (index >= 0) {
            listings[index] = listings[index].copy(isFavorite = !listings[index].isFavorite)
            persistListings()
        }
    }

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
                    listings = listings,
                    onOpenCategories = { navigate("categories") },
                    onOpenProfile = { navigate("profile") },
                    onToggleFavorite = ::toggleFavorite
                )
            }
            composable("categories") {
                CategoriesScreen(
                    listings = listings,
                    onToggleFavorite = ::toggleFavorite
                )
            }
            composable("add") {
                AddListingScreen(
                    onListingCreated = { listing ->
                        listings.add(0, listing)
                        persistListings()
                        navigate("home")
                    }
                )
            }
            composable("messages") {
                MessagesScreen()
            }
            composable("profile") {
                ProfileScreen(
                    profile = profile,
                    listingCount = listings.count { it.id.startsWith("listing-") },
                    favoriteCount = listings.count { it.isFavorite },
                    onLogin = { name, email ->
                        profile = UserProfile(name = name, email = email, loggedIn = true)
                        LocalStore.saveProfile(context, profile)
                    },
                    onLogout = {
                        profile = UserProfile()
                        LocalStore.saveProfile(context, profile)
                    }
                )
            }
        }
    }
}
