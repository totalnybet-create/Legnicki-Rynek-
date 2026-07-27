package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.ui.theme.LegnicaBackground
import pl.legnickirynek.app.ui.theme.LegnicaCoral
import pl.legnickirynek.app.ui.theme.LegnicaMuted
import pl.legnickirynek.app.ui.theme.LegnicaNavy

@Composable
fun CategoriesScreen(
    listings: List<Listing>,
    onToggleFavorite: (String) -> Unit
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleListings = selectedCategoryId?.let { id ->
        listings.filter { it.categoryId == id }
    } ?: listings

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
                    .padding(20.dp)
            ) {
                Text("Kategorie", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Wybierz kategorię i zobacz lokalne oferty.",
                    color = Color.White.copy(alpha = 0.74f)
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategoryId = null },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCategoryId == null) LegnicaCoral else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▦", fontSize = 27.sp)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                "Wszystkie kategorie",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCategoryId == null) Color.White else Color.Unspecified
                            )
                            Text(
                                "${listings.size} ogłoszeń",
                                color = if (selectedCategoryId == null) Color.White.copy(alpha = 0.78f) else LegnicaMuted
                            )
                        }
                    }
                }

                SampleData.categories.forEach { category ->
                    val selected = selectedCategoryId == category.id
                    val count = listings.count { it.categoryId == category.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategoryId = category.id },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) LegnicaNavy else Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.symbol, fontSize = 29.sp)
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(
                                    category.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color.Unspecified
                                )
                                Text(
                                    "$count ogłoszeń",
                                    color = if (selected) Color.White.copy(alpha = 0.72f) else LegnicaMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = selectedCategoryId?.let { id ->
                    SampleData.categories.firstOrNull { it.id == id }?.name
                } ?: "Wszystkie ogłoszenia",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (visibleListings.isEmpty()) {
            item {
                Text(
                    "Brak ogłoszeń w tej kategorii.",
                    modifier = Modifier.padding(20.dp),
                    color = LegnicaMuted
                )
            }
        } else {
            items(visibleListings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    onToggleFavorite = { onToggleFavorite(listing.id) }
                )
            }
        }
    }
}
