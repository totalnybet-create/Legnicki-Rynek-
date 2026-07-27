package pl.legnickirynek.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.ui.theme.LegnicaCoral
import pl.legnickirynek.app.ui.theme.LegnicaMuted
import pl.legnickirynek.app.ui.theme.LegnicaNavy

@Composable
fun HomeScreen(
    listings: List<Listing>,
    onOpenCategories: () -> Unit,
    onOpenProfile: () -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredListings = listings.filter {
        query.isBlank() ||
            it.title.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            LegnicaHero(
                query = query,
                onQueryChange = { query = it },
                onOpenProfile = onOpenProfile
            )
        }

        item {
            LocalStatusStrip()
        }

        item {
            SectionTitle(
                title = "Kategorie",
                action = "Wszystkie",
                onAction = onOpenCategories
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SampleData.categories) { category ->
                    Card(
                        modifier = Modifier
                            .width(132.dp)
                            .clickable(onClick = onOpenCategories)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Kategoria ${category.name}"
                            },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = LegnicaCoral.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(category.symbol, fontSize = 28.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                category.name,
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
            SectionTitle(title = "Dzieje się w Legnicy", action = "Zobacz więcej")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SampleData.events) { event ->
                    Card(
                        modifier = Modifier.width(300.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                event.date.uppercase(Locale("pl", "PL")),
                                color = LegnicaCoral,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                event.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                event.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "📍 ${event.location}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = if (query.isBlank()) "Polecane ogłoszenia" else "Wyniki wyszukiwania",
                action = if (query.isBlank()) "Najnowsze" else "${filteredListings.size} wyników"
            )
        }

        if (filteredListings.isEmpty()) {
            item {
                EmptySearchState(query = query, onClear = { query = "" })
            }
        } else {
            items(filteredListings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    onToggleFavorite = { onToggleFavorite(listing.id) }
                )
            }
        }
    }
}

@Composable
private fun LegnicaHero(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(LegnicaNavy, Color(0xFF183B63), Color(0xFF2D587C))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Legnicki Rynek",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Wszystko lokalne w jednym miejscu",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp
                )
            }
            TextButton(onClick = onOpenProfile) {
                Text("Profil", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text(
                text = "Panorama Legnicy",
                color = LegnicaCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Znajdź, sprzedaj i odkrywaj blisko siebie",
                color = Color.White,
                fontSize = 29.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(18.dp))
            LegnicaSkyline()
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Wyszukiwarka ogłoszeń" },
                placeholder = { Text("Czego szukasz w Legnicy i okolicy?") },
                leadingIcon = { Text("⌕", fontSize = 22.sp) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Text(
                            text = "×",
                            fontSize = 25.sp,
                            modifier = Modifier
                                .clickable { onQueryChange("") }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Wyczyść wyszukiwanie"
                                }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.96f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.90f),
                    focusedTextColor = LegnicaNavy,
                    unfocusedTextColor = LegnicaNavy,
                    focusedBorderColor = LegnicaCoral,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@Composable
private fun LegnicaSkyline() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(30, 46, 34, 58, 40, 52, 32, 44).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(if (index == 3 || index == 5) 18.dp else 24.dp)
                    .height(height.dp)
                    .background(
                        Color.White.copy(alpha = if (index % 2 == 0) 0.26f else 0.38f),
                        RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                    )
            )
        }
    }
}

@Composable
private fun LocalStatusStrip() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusItem("Dzisiaj", "Legnica")
            StatusItem("Lokalnie", "Ogłoszenia")
            StatusItem("Blisko", "Wydarzenia")
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun EmptySearchState(query: String, onClear: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Brak wyników", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (query.isBlank()) "Nie ma jeszcze ogłoszeń." else "Nie znaleziono ogłoszeń dla „$query”.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (query.isNotBlank()) {
                TextButton(onClick = onClear) { Text("Wyczyść wyszukiwanie") }
            }
        }
    }
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
            .padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(action, color = LegnicaCoral, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: Listing,
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
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LegnicaNavy.copy(alpha = 0.16f),
                                LegnicaCoral.copy(alpha = 0.12f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val symbol = SampleData.categories
                    .firstOrNull { it.id == listing.categoryId }
                    ?.symbol ?: "●"
                Text(symbol, fontSize = 32.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listing.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    formattedPrice,
                    color = LegnicaCoral,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "📍 ${listing.location}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Text(
                text = if (listing.isFavorite) "♥" else "♡",
                color = LegnicaCoral,
                fontSize = 29.sp,
                modifier = Modifier
                    .clickable(onClick = onToggleFavorite)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (listing.isFavorite) {
                            "Usuń ${listing.title} z ulubionych"
                        } else {
                            "Dodaj ${listing.title} do ulubionych"
                        }
                    }
            )
        }
    }
}
