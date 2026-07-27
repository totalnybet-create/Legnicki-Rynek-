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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import pl.legnickirynek.app.ui.screens.ConversationScreen
import pl.legnickirynek.app.ui.screens.EditListingScreen
import pl.legnickirynek.app.ui.screens.HomeScreen
import pl.legnickirynek.app.ui.screens.ListingDetailScreen
import pl.legnickirynek.app.ui.screens.ListingsCollectionScreen
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
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val showBottomBar = destinations.any { it.route == currentRoute }

    LaunchedEffect(uiState.dataError) {
        uiState.dataError?.let { error ->
            snackbarHostState.showSnackbar(error)
            appViewModel.clearDataError()
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo("home") {
                saveState = true
            }
        }
    }

    fun openListing(id: String) {
        navController.navigate("listing/$id") {
            launchSingleTop = true
        }
    }

    fun openConversation(id: String) {
        navController.navigate("conversation/$id") {
            launchSingleTop = true
        }
    }

    fun openCreatedListing(id: String) {
        navController.navigate("listing/$id") {
            launchSingleTop = true
            popUpTo("add") {
                inclusive = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    destinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(destination.route) },
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
                    onOpenCategories = { navigateTopLevel("categories") },
                    onOpenProfile = { navigateTopLevel("profile") },
                    onOpenListing = ::openListing,
                    onToggleFavorite = appViewModel::toggleFavorite
                )
            }
            composable("categories") {
                CategoriesScreen(
                    listings = uiState.listings,
                    onOpenListing = ::openListing,
                    onToggleFavorite = appViewModel::toggleFavorite
                )
            }
            composable("add") {
                AddListingScreen(
                    onListingCreated = { listing ->
                        appViewModel.addListing(listing)
                        openCreatedListing(listing.id)
                    }
                )
            }
            composable("messages") {
                MessagesScreen(
                    conversations = uiState.conversations,
                    onOpenConversation = ::openConversation
                )
            }
            composable("profile") {
                ProfileScreen(
                    profile = uiState.profile,
                    listingCount = uiState.listings.count {
                        it.sellerName == uiState.profile.name && uiState.profile.loggedIn
                    },
                    favoriteCount = uiState.listings.count { it.isFavorite },
                    onOpenMyListings = {
                        navController.navigate("my-listings")
                    },
                    onOpenFavorites = {
                        navController.navigate("favorites")
                    },
                    onLogin = appViewModel::login,
                    onLogout = appViewModel::logout
                )
            }
            composable("favorites") {
                ListingsCollectionScreen(
                    title = "Ulubione",
                    listings = uiState.listings.filter { it.isFavorite },
                    emptyMessage = "Nie masz jeszcze ulubionych ogłoszeń.",
                    onBack = { navController.popBackStack() },
                    onOpenListing = ::openListing,
                    onToggleFavorite = appViewModel::toggleFavorite,
                    onEmptyAction = { navigateTopLevel("home") },
                    emptyActionLabel = "Przeglądaj ogłoszenia"
                )
            }
            composable("my-listings") {
                val myListings = if (uiState.profile.loggedIn) {
                    uiState.listings.filter { it.sellerName == uiState.profile.name }
                } else {
                    emptyList()
                }

                ListingsCollectionScreen(
                    title = "Moje ogłoszenia",
                    listings = myListings,
                    emptyMessage = if (uiState.profile.loggedIn) {
                        "Nie masz jeszcze własnych ogłoszeń."
                    } else {
                        "Zaloguj się, aby zobaczyć swoje ogłoszenia."
                    },
                    onBack = { navController.popBackStack() },
                    onOpenListing = ::openListing,
                    onToggleFavorite = appViewModel::toggleFavorite,
                    onEmptyAction = {
                        if (uiState.profile.loggedIn) {
                            navigateTopLevel("add")
                        } else {
                            navigateTopLevel("profile")
                        }
                    },
                    emptyActionLabel = if (uiState.profile.loggedIn) {
                        "Dodaj ogłoszenie"
                    } else {
                        "Przejdź do logowania"
                    }
                )
            }
            composable("listing/{listingId}") { entry ->
                val listingId = entry.arguments?.getString("listingId").orEmpty()
                val listing = uiState.listings.firstOrNull { it.id == listingId }
                val canManage = listing != null && (
                    listing.id.startsWith("listing-") ||
                        uiState.profile.loggedIn && listing.sellerName == uiState.profile.name
                    )

                ListingDetailScreen(
                    listing = listing,
                    canManage = canManage,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("edit/$listingId") },
                    onDelete = {
                        appViewModel.deleteListing(listingId)
                        navController.popBackStack()
                    },
                    onToggleFavorite = { appViewModel.toggleFavorite(listingId) },
                    onStatusChange = { status ->
                        appViewModel.updateListingStatus(listingId, status)
                    },
                    onMessageSeller = {
                        listing?.let { currentListing ->
                            val conversationId = appViewModel.ensureConversation(currentListing)
                            openConversation(conversationId)
                        }
                    }
                )
            }
            composable("edit/{listingId}") { entry ->
                val listingId = entry.arguments?.getString("listingId").orEmpty()
                val listing = uiState.listings.firstOrNull { it.id == listingId }

                if (listing == null) {
                    ListingDetailScreen(
                        listing = null,
                        canManage = false,
                        onBack = { navController.popBackStack() },
                        onEdit = {},
                        onDelete = {},
                        onToggleFavorite = {},
                        onStatusChange = {}
                    )
                } else {
                    EditListingScreen(
                        listing = listing,
                        onListingUpdated = { updatedListing ->
                            appViewModel.updateListing(updatedListing)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("conversation/{conversationId}") { entry ->
                val conversationId = entry.arguments?.getString("conversationId").orEmpty()
                val conversationFlow = remember(conversationId) {
                    appViewModel.observeConversation(conversationId)
                }
                val messagesFlow = remember(conversationId) {
                    appViewModel.observeMessages(conversationId)
                }
                val conversation by conversationFlow.collectAsStateWithLifecycle(
                    initialValue = uiState.conversations.firstOrNull { it.id == conversationId }
                )
                val messages by messagesFlow.collectAsStateWithLifecycle(
                    initialValue = emptyList()
                )

                LaunchedEffect(conversationId) {
                    appViewModel.markConversationRead(conversationId)
                }

                ConversationScreen(
                    conversation = conversation,
                    messages = messages,
                    onBack = { navController.popBackStack() },
                    onOpenListing = ::openListing,
                    onSendMessage = { body ->
                        appViewModel.sendMessage(conversationId, body)
                    },
                    onDeleteConversation = {
                        appViewModel.deleteConversation(conversationId)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
