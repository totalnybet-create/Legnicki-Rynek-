package pl.legnickirynek.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF062A55)
private val Coral = Color(0xFFFF6B5E)
private val Background = Color(0xFFF5F6F8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Coral, secondary = Navy)) {
                LegnickiRynekApp()
            }
        }
    }
}

data class Category(val name: String, val symbol: String)
data class Listing(val title: String, val price: String, val location: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegnickiRynekApp() {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val categories = listOf(
        Category("Motoryzacja", "🚗"),
        Category("Nieruchomości", "🏠"),
        Category("Praca", "💼"),
        Category("Usługi", "🛠"),
        Category("Dom i ogród", "🪴"),
        Category("Elektronika", "📱")
    )

    val listings = listOf(
        Listing("Rower miejski w dobrym stanie", "850 zł", "Legnica"),
        Listing("Mieszkanie 2 pokoje", "2 300 zł", "Tarninów"),
        Listing("Laptop Lenovo ThinkPad", "1 450 zł", "Piekary"),
        Listing("Komplet opon zimowych", "700 zł", "Legnica")
    ).filter { it.title.contains(query, ignoreCase = true) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Główna", "Kategorie", "Dodaj", "Wiadomości", "Profil").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(if (index == 2) "+" else "•", fontSize = 22.sp) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Legnicki Rynek", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        TextButton(onClick = {}) { Text("Zaloguj", color = Color.White) }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Wszystko lokalnie. Wszystko blisko.", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Czego szukasz?") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Coral,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            item {
                Text("Kategorie", modifier = Modifier.padding(20.dp, 22.dp, 20.dp, 10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories) { category ->
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Column(
                                modifier = Modifier.width(120.dp).padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(category.symbol, fontSize = 30.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(category.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Text("Wydarzenia i aktualności", modifier = Modifier.padding(20.dp, 26.dp, 20.dp, 10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Legnica", color = Coral, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Weekend pełen lokalnych wydarzeń", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Sprawdź koncerty, targi i wydarzenia rodzinne w mieście.")
                    }
                }
            }

            item {
                Text("Polecane ogłoszenia", modifier = Modifier.padding(20.dp, 26.dp, 20.dp, 10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            items(listings) { listing ->
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(listing.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(listing.price, color = Coral, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(listing.location, color = Color.Gray)
                    }
                }
            }
        }
    }
}
