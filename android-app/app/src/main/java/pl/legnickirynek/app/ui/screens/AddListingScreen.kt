package pl.legnickirynek.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing

@Composable
fun AddListingScreen(onListingCreated: (Listing) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("Legnica") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(SampleData.categories.first().id) }
    var message by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(22.dp)) {
                Text("Dodaj ogłoszenie", color = MaterialTheme.colorScheme.onPrimary, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Przejrzysty formularz lokalnej oferty", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("1. Wybierz kategorię", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SampleData.categories) { category ->
                            FilterChip(selected = categoryId == category.id, onClick = { categoryId = category.id }, label = { Text("${category.symbol} ${category.name}") })
                        }
                    }
                    Text("2. Podstawowe informacje", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Tytuł ogłoszenia") }, supportingText = { Text("Krótko i konkretnie") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(price, { price = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Cena") }, suffix = { Text("zł") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(location, { location = it }, Modifier.weight(1f), label = { Text("Lokalizacja") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    }
                    Text("3. Opisz ofertę", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Opis") }, minLines = 5, shape = RoundedCornerShape(16.dp))
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Zdjęcia", fontWeight = FontWeight.Bold)
                            Text("Miejsce na galerię zdjęć — interfejs demonstracyjny.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            val numericPrice = price.toIntOrNull()
                            if (title.trim().length < 4 || numericPrice == null || location.isBlank() || description.trim().length < 10) {
                                message = "Uzupełnij wymagane pola."
                            } else {
                                onListingCreated(Listing("listing-${System.currentTimeMillis()}", title.trim(), numericPrice, location.trim(), categoryId, description.trim()))
                                message = "Podgląd ogłoszenia jest gotowy."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("Zobacz podgląd", fontWeight = FontWeight.Black) }
                    AnimatedVisibility(message.isNotBlank()) {
                        Text(message, color = if (message.startsWith("Podgląd")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    Text("Ten ekran obejmuje wyłącznie warstwę UI formularza.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}
