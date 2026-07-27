package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listing: Listing?,
    canManage: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStatusChange: (ListingStatus) -> Unit,
    onMessageSeller: () -> Unit = {},
    onOpenMap: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły ogłoszenia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                },
                actions = {
                    if (listing != null) {
                        if (!canManage) {
                            IconButton(onClick = onMessageSeller) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Napisz do sprzedającego"
                                )
                            }
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (listing.isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = if (listing.isFavorite) {
                                    "Usuń z ulubionych"
                                } else {
                                    "Dodaj do ulubionych"
                                },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (canManage) {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edytuj ogłoszenie"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (listing == null) {
            MissingListing(
                modifier = Modifier.padding(innerPadding),
                onBack = onBack
            )
        } else {
            ListingDetails(
                listing = listing,
                canManage = canManage,
                onEdit = onEdit,
                onRequestDelete = { showDeleteDialog = true },
                onStatusChange = onStatusChange,
                onMessageSeller = onMessageSeller,
                onOpenMap = onOpenMap,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            title = { Text("Usunąć ogłoszenie?") },
            text = {
                Text("Tej operacji nie można cofnąć. Ogłoszenie zostanie usunięte z urządzenia.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun ListingDetails(
    listing: Listing,
    canManage: Boolean,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onStatusChange: (ListingStatus) -> Unit,
    onMessageSeller: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedPrice = NumberFormat
        .getIntegerInstance(Locale("pl", "PL"))
        .format(listing.price) + " zł"
    val formattedDate = DateFormat
        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("pl", "PL"))
        .format(Date(listing.updatedAt))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            if (listing.imageUris.isEmpty()) {
                ListingImagePlaceholder(listing = listing)
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listing.imageUris, key = { it }) { uri ->
                        Card(
                            modifier = Modifier
                                .width(320.dp)
                                .height(230.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Zdjęcie ogłoszenia ${listing.title}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusLabel(status = listing.status)
                Text(
                    text = listing.title,
                    fontSize = 27.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = formattedPrice,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = listing.location,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Zaktualizowano: $formattedDate",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        item {
            DetailCard(title = "Opis") {
                Text(
                    text = listing.description,
                    lineHeight = 23.sp
                )
            }
        }

        item {
            DetailCard(title = "Lokalizacja") {
                Text(
                    text = listing.location,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onOpenMap,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pokaż na mapie")
                }
                Text(
                    text = "Dane mapy © OpenStreetMap contributors",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        item {
            DetailCard(title = "Sprzedający") {
                Text(
                    text = listing.sellerName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (canManage) {
                        "To jest Twoje ogłoszenie."
                    } else {
                        "Skontaktuj się bezpiecznie przez wiadomości w aplikacji."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!canManage) {
                    Button(
                        onClick = onMessageSeller,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Napisz wiadomość")
                    }
                }
            }
        }

        if (canManage) {
            item {
                DetailCard(title = "Zarządzaj ogłoszeniem") {
                    FilledTonalButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edytuj ogłoszenie")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusAction(
                            label = "Aktywne",
                            selected = listing.status == ListingStatus.ACTIVE,
                            onClick = { onStatusChange(ListingStatus.ACTIVE) },
                            modifier = Modifier.weight(1f)
                        )
                        StatusAction(
                            label = "Rezerwacja",
                            selected = listing.status == ListingStatus.RESERVED,
                            onClick = { onStatusChange(ListingStatus.RESERVED) },
                            modifier = Modifier.weight(1f)
                        )
                        StatusAction(
                            label = "Sprzedane",
                            selected = listing.status == ListingStatus.SOLD,
                            onClick = { onStatusChange(ListingStatus.SOLD) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TextButton(
                        onClick = onRequestDelete,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Usuń ogłoszenie",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingImagePlaceholder(listing: Listing) {
    val symbol = SampleData.categories
        .firstOrNull { it.id == listing.categoryId }
        ?.symbol
        ?: "●"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 72.sp)
    }
}

@Composable
private fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            content()
        }
    }
}

@Composable
private fun StatusAction(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label, maxLines = 1)
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(label, maxLines = 1)
        }
    }
}

@Composable
private fun StatusLabel(status: ListingStatus) {
    val label = when (status) {
        ListingStatus.ACTIVE -> "Aktywne"
        ListingStatus.RESERVED -> "Zarezerwowane"
        ListingStatus.SOLD -> "Sprzedane"
        ListingStatus.EXPIRED -> "Wygasłe"
    }

    Text(
        text = label,
        color = when (status) {
            ListingStatus.ACTIVE -> MaterialTheme.colorScheme.primary
            ListingStatus.RESERVED -> MaterialTheme.colorScheme.tertiary
            ListingStatus.SOLD,
            ListingStatus.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun MissingListing(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Nie znaleziono ogłoszenia.",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Wróć")
        }
    }
}
