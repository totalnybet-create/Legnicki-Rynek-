package pl.legnickirynek.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing

@Composable
fun CategoriesScreen(
    listings: List<Listing>,
    onToggleFavorite: (String) -> Unit,
    onOpenListing: (String) -> Unit = {}
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleListings = selectedCategoryId?.let { id -> listings.filter { it.categoryId == id } } ?: listings

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(20.dp)
            ) {
                Text("Kategorie", color = MaterialTheme.colorScheme.onPrimary, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Wybierz dział i przeglądaj lokalne oferty", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f))
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 600.dp) 4 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxWidth().height(if (columns == 4) 260.dp else 430.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        CategoryTile("▦", "Wszystkie", listings.size, selectedCategoryId == null) { selectedCategoryId = null }
                    }
                    gridItems(SampleData.categories) { category ->
                        CategoryTile(
                            category.symbol,
                            category.name,
                            listings.count { it.categoryId == category.id },
                            selectedCategoryId == category.id
                        ) { selectedCategoryId = category.id }
                    }
                }
            }
        }
        item {
            AnimatedContent(targetState = selectedCategoryId, label = "categoryTitle") { id ->
                Text(
                    text = id?.let { selected -> SampleData.categories.firstOrNull { it.id == selected }?.name } ?: "Wszystkie ogłoszenia",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        if (visibleListings.isEmpty()) {
            item { Text("Brak ogłoszeń w tej kategorii.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(visibleListings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    onToggleFavorite = { onToggleFavorite(listing.id) },
                    onOpen = { onOpenListing(listing.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(symbol: String, name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).semantics { role = Role.Button },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 30.sp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(name, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text("$count ogłoszeń", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
