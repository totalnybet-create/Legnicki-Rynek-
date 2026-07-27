package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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

private data class NewsUi(val tag: String, val title: String, val excerpt: String, val date: String)

private val newsItems = listOf(
    NewsUi("Miasto", "Nowa przestrzeń rekreacyjna w centrum Legnicy", "Miejsce dla rodzin, spacerowiczów i lokalnych inicjatyw.", "Dzisiaj, 08:30"),
    NewsUi("Komunikacja", "Zmiany organizacji ruchu w weekend", "Sprawdź objazdy i czasowe wyłączenia ulic w centrum.", "Wczoraj, 17:10"),
    NewsUi("Kultura", "Letni program wydarzeń na rynku", "Koncerty, kino plenerowe i rodzinne warsztaty.", "25 lipca")
)

@Composable
fun NewsScreen() {
    LocalScreenHeader("Aktualności", "Najważniejsze informacje z Legnicy i okolic") {
        items(newsItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.tag, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(item.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(item.excerpt, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Czytaj więcej →", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EventsScreen() {
    var selected by rememberSaveable { mutableStateOf("Wszystkie") }
    val filters = listOf("Wszystkie", "Dzisiaj", "Weekend", "Rodzinne")
    LocalScreenHeader("Wydarzenia", "Zobacz, co dzieje się w Legnicy") {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(selected = selected == filter, onClick = { selected = filter }, label = { Text(filter) })
                }
            }
        }
        items(SampleData.events) { event ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(Modifier.padding(18.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(event.date, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(event.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(event.description)
                    }
                    Text("›", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun LocalScreenHeader(
    title: String,
    subtitle: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(20.dp)
            ) {
                Text(title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f))
            }
        }
        content()
    }
}
