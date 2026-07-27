package pl.legnickirynek.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.ui.screens.AddListingScreen
import pl.legnickirynek.app.ui.screens.CategoriesScreen
import pl.legnickirynek.app.ui.screens.HomeScreen
import pl.legnickirynek.app.ui.screens.MessagesScreen
import pl.legnickirynek.app.ui.screens.ProfileScreen
import pl.legnickirynek.app.ui.theme.LegnicaCoral
import pl.legnickirynek.app.ui.theme.LegnicaNavy

private data class AppDestination(
    val route: String,
    val label: String,
    val symbol: String
)

private val destinations = listOf(
    AppDestination("home", "Główna", "⌂"),
    AppDestination("categories", "Kategorie", "▦"),
    AppDestination("add", "Dodaj", "+"),
    AppDestination("messages", "Wiadomości", "✉"),
    AppDestination("profile", "Profil", "♙")
)

@Composable
fun LegnickiRynekApp() {
    val navController = rememberNavController()
    val listings = remember {
        mutableStateListOf<Listing>().apply { addAll(SampleData.listings) }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    fun toggleFavorite(id: String) {
        val index = listings.indexOfFirst { it.id == id }
        if (index >= 0) {
            listings[index] = listings[index].copy(isFavorite = !listings[index].isFavorite)
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
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigate(destination.route) },
                        icon = {
                            Text(
                                text = destination.symbol,
                                fontSize = if (destination.route == "add") 27.sp else 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) LegnicaCoral else LegnicaNavy
                            )
                        },
                        label = { Text(destination.label, fontSize = 11.sp) }
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
                        navigate("home")
                    }
                )
            }
            composable("messages") {
                MessagesScreen()
            }
            composable("profile") {
                ProfileScreen()
            }
        }
    }
}
