package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.domain.ListingSearch
import pl.legnickirynek.app.domain.ListingSearchCriteria
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus
import pl.legnickirynek.app.model.LocalEvent
import pl.legnickirynek.app.model.LocalNewsItem
import pl.legnickirynek.app.model.WeatherSnapshot

private data class LocalFeedCard(
    val id: String,
    val date: String,
    val title: String,
    val description: String,
    val locationOrSource: String,
    val sourceUrl: String
)

@Composable
fun HomeScreen(
    listings: List<Listing>,
    weather: WeatherSnapshot?,
    events: List<LocalEvent>,
    localNews: List<LocalNewsItem>,
    localDataLoading: Boolean,
    localDataError: String?,
    onRefreshLocalData: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var criteria by rememberSaveable { mutableStateOf(ListingSearchCriteria()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filteredListings = ListingSearch.apply(listings, criteria)
    val localFeed = remember(events, localNews) { buildLocalFeed(events, localNews) }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Legnicki Rynek",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = weather?.let {
                                "${it.temperatureC.roundToInt()}°C • ${it.description}"
                            } ?: if (localDataLoading) {
                                "Pobieranie danych lokalnych…"
                            } else {
                                "Lokalnie i blisko"
                            },
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.78f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onRefreshLocalData,
                        enabled = !localDataLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Odśwież pogodę, wydarzenia i aktualności",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    TextButton(onClick = onOpenProfile) {
                        Text(
                            text = "Profil",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Znajdź to, czego potrzebujesz w Legnicy",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 27.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = criteria.query,
                    onValueChange = { query ->
                        criteria = criteria.copy(query = query.take(120))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Czego szukasz?") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (criteria.query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    criteria = criteria.copy(query = "")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Wyczyść wyszukiwanie"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredListings.size} wyników",
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.82f),
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (criteria.activeFilterCount == 0) {
                                "Filtry"
                            } else {
                                "Filtry (${criteria.activeFilterCount})"
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "Kategorie",
                action = "Wszystkie →",
                onAction = onOpenCategories
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SampleData.categories, key = { it.id }) { category ->
                    val selected = criteria.categoryId == category.id
                    Card(
                        modifier = Modifier
                            .width(126.dp)
                            .clickable {
                                criteria = criteria.copy(
                                    categoryId = if (selected) null else category.id
                                )
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(category.symbol, fontSize = 32.sp)
                            Spacer(Modifier.height(9.dp))
                            Text(
                                category.name,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(title = "Wydarzenia i aktualności")
            if (!localDataError.isNullOrBlank()) {
                Text(
                    text = "Nie udało się odświeżyć części danych. Wyświetlam ostatnie dostępne informacje.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            if (localFeed.isEmpty()) {
                Text(
                    text = if (localDataLoading) "Pobieranie danych…" else "Brak aktualnych informacji.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(localFeed, key = { it.id }) { feedItem ->
                        Card(
                            modifier = Modifier
                                .width(292.dp)
                                .clickable(enabled = feedItem.sourceUrl.isNotBlank()) {
                                    runCatching { uriHandler.openUri(feedItem.sourceUrl) }
                                },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text(
                                    feedItem.date,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    feedItem.title,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    feedItem.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    feedItem.locationOrSource,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(title = "Ogłoszenia (${filteredListings.size})")
        }

        if (filteredListings.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Nie znaleziono ogłoszeń spełniających wybrane kryteria.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            criteria = ListingSearchCriteria()
                        }
                    ) {
                        Text("Wyczyść wyszukiwanie i filtry")
                    }
                }
            }
        } else {
            items(filteredListings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    onOpen = { onOpenListing(listing.id) },
                    onToggleFavorite = { onToggleFavorite(listing.id) }
                )
            }
        }
    }

    if (showFilters) {
        ListingFilterSheet(
            criteria = criteria,
            onApply = { updatedCriteria ->
                criteria = updatedCriteria
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
    }
}

private fun buildLocalFeed(
    events: List<LocalEvent>,
    news: List<LocalNewsItem>
): List<LocalFeedCard> = buildList {
    events.take(10).forEach { event ->
        add(
            LocalFeedCard(
                id = "event-${event.id}",
                date = event.date,
                title = event.title,
                description = event.description,
                locationOrSource = event.location,
                sourceUrl = event.sourceUrl
            )
        )
    }
    news.take(8).forEach { item ->
        add(
            LocalFeedCard(
                id = "news-${item.id}",
                date = formatNewsDate(item.publishedAt),
                title = item.title,
                description = item.description.ifBlank {
                    "Otwórz aktualność, aby przeczytać pełną informację."
                },
                locationOrSource = item.sourceName,
                sourceUrl = item.sourceUrl
            )
        )
    }
}

private fun formatNewsDate(value: String): String = runCatching {
    ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
        .withZoneSameInstant(ZoneId.of("Europe/Warsaw"))
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("pl", "PL")))
}.getOrElse {
    value.take(16).ifBlank { "Aktualność" }
}

@Composable
private fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 26.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(
                    action,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedPrice = NumberFormat
        .getIntegerInstance(Locale("pl", "PL"))
        .format(listing.price) + " zł"

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val thumbnail = listing.imageUris.firstOrNull()
                if (thumbnail == null) {
                    val symbol = SampleData.categories
                        .firstOrNull { it.id == listing.categoryId }
                        ?.symbol
                        ?: "●"
                    Text(symbol, fontSize = 31.sp)
                } else {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = "Miniatura ogłoszenia ${listing.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (listing.status != ListingStatus.ACTIVE) {
                    Text(
                        text = when (listing.status) {
                            ListingStatus.RESERVED -> "Zarezerwowane"
                            ListingStatus.SOLD -> "Sprzedane"
                            ListingStatus.EXPIRED -> "Wygasłe"
                            ListingStatus.ACTIVE -> ""
                        },
                        color = when (listing.status) {
                            ListingStatus.RESERVED -> MaterialTheme.colorScheme.tertiary
                            ListingStatus.SOLD,
                            ListingStatus.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
                            ListingStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    listing.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formattedPrice,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    listing.location,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
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
        }
    }
}
