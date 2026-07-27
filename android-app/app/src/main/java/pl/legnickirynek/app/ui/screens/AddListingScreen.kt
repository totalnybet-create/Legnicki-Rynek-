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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.domain.ListingValidationError
import pl.legnickirynek.app.domain.ListingValidator
import pl.legnickirynek.app.model.Listing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(onListingCreated: (Listing) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("Legnica") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(SampleData.categories.first().id) }
    var message by rememberSaveable { mutableStateOf("") }
    var messageIsError by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(20.dp)
            ) {
                Text(
                    "Dodaj ogłoszenie",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Uzupełnij dane oferty. Publikacja zajmuje mniej niż minutę.",
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.74f)
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
                    items(SampleData.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text("${category.symbol} ${category.name}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(80)
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tytuł ogłoszenia") },
                    supportingText = { Text("${title.length}/80") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it.filter(Char::isDigit).take(9)
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cena w zł") },
                    suffix = { Text("zł") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it.take(80)
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lokalizacja") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(2000)
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Opis") },
                    supportingText = { Text("${description.length}/2000") },
                    minLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = {
                        when (
                            ListingValidator.validate(
                                title = title,
                                price = price,
                                location = location,
                                description = description
                            )
                        ) {
                            ListingValidationError.TITLE_TOO_SHORT -> {
                                message = "Tytuł musi mieć co najmniej 4 znaki."
                                messageIsError = true
                            }

                            ListingValidationError.PRICE_INVALID -> {
                                message = "Podaj prawidłową cenę."
                                messageIsError = true
                            }

                            ListingValidationError.LOCATION_REQUIRED -> {
                                message = "Podaj lokalizację."
                                messageIsError = true
                            }

                            ListingValidationError.DESCRIPTION_TOO_SHORT -> {
                                message = "Opis musi mieć co najmniej 10 znaków."
                                messageIsError = true
                            }

                            null -> {
                                val numericPrice = requireNotNull(price.toIntOrNull())
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
                                messageIsError = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Text("Opublikuj ogłoszenie", fontWeight = FontWeight.ExtraBold)
                }

                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        color = if (messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    "Ogłoszenie zostanie zapisane lokalnie na tym urządzeniu. Synchronizacja internetowa zostanie podłączona w kolejnym etapie.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}
