package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(onListingCreated: (Listing) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("Legnica") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(SampleData.categories.first().id) }
    var message by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LegnicaBackground),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LegnicaNavy)
                    .padding(20.dp)
            ) {
                Text("Dodaj ogłoszenie", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Uzupełnij dane oferty. Publikacja zajmuje mniej niż minutę.",
                    color = Color.White.copy(alpha = 0.74f)
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Kategoria", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SampleData.categories) { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text("${category.symbol} ${category.name}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tytuł ogłoszenia") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cena w zł") },
                    suffix = { Text("zł") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lokalizacja") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Opis") },
                    minLines = 4,
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = {
                        val numericPrice = price.toIntOrNull()
                        if (title.trim().length < 4) {
                            message = "Tytuł musi mieć co najmniej 4 znaki."
                        } else if (numericPrice == null) {
                            message = "Podaj prawidłową cenę."
                        } else if (location.isBlank()) {
                            message = "Podaj lokalizację."
                        } else if (description.trim().length < 10) {
                            message = "Opis musi mieć co najmniej 10 znaków."
                        } else {
                            onListingCreated(
                                Listing(
                                    id = "listing-${System.currentTimeMillis()}",
                                    title = title.trim(),
                                    price = numericPrice,
                                    location = location.trim(),
                                    categoryId = categoryId,
                                    description = description.trim()
                                )
                            )
                            title = ""
                            price = ""
                            location = "Legnica"
                            description = ""
                            message = "Ogłoszenie zostało opublikowane."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LegnicaCoral)
                ) {
                    Text("Opublikuj ogłoszenie", fontWeight = FontWeight.ExtraBold)
                }

                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        color = if (message.startsWith("Ogłoszenie")) LegnicaNavy else LegnicaCoral,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    "Na tym etapie dane są przechowywane w pamięci aplikacji. Po podłączeniu API publikacja trafi również na stronę internetową.",
                    color = LegnicaMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}
