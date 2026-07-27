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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.ui.theme.LegnicaBackground
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
            .background(LegnicaBackground),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LegnicaNavy)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Legnicki Rynek",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Lokalnie i blisko",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = onOpenProfile) {
                        Text("Zaloguj", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Znajdź to, czego potrzebujesz w Legnicy",
                    color = Color.White,
                    fontSize = 27.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Czego szukasz?") },
                    leadingIcon = { Text("⌕", fontSize = 22.sp) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Text(
                                text = "×",
                                fontSize = 24.sp,
                                modifier = Modifier.clickable { query = "" }
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = LegnicaCoral,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
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
                items(SampleData.categories) { category ->
                    Card(
                        modifier = Modifier
                            .width(126.dp)
                            .clickable(onClick = onOpenCategories),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(category.symbol, fontSize = 32.sp)
                            Spacer(Modifier.height(9.dp))
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
            SectionTitle(title = "Wydarzenia i aktualności")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SampleData.events) { event ->
                    Card(
                        modifier = Modifier.width(292.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(event.date, color = LegnicaCoral, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(6.dp))
                            Text(event.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(event.description, color = LegnicaMuted, maxLines = 2)
                            Spacer(Modifier.height(8.dp))
                            Text(event.location, color = LegnicaNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item { SectionTitle(title = "Polecane ogłoszenia") }

        if (filteredListings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nie znaleziono pasujących ogłoszeń.", color = LegnicaMuted)
                }
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
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(74.dp)
                    .background(LegnicaNavy.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val symbol = SampleData.categories.firstOrNull { it.id == listing.categoryId }?.symbol ?: "●"
                Text(symbol, fontSize = 31.sp)
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
                Spacer(Modifier.height(6.dp))
                Text(formattedPrice, color = LegnicaCoral, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(listing.location, color = LegnicaMuted, fontSize = 13.sp)
            }
            Text(
                text = if (listing.isFavorite) "♥" else "♡",
                color = LegnicaCoral,
                fontSize = 27.sp,
                modifier = Modifier.clickable(onClick = onToggleFavorite)
            )
        }
    }
}
