package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.domain.ListingSearchCriteria
import pl.legnickirynek.app.domain.ListingSort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingFilterSheet(
    criteria: ListingSearchCriteria,
    onApply: (ListingSearchCriteria) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryId by remember(criteria) { mutableStateOf(criteria.categoryId) }
    var minimumPrice by remember(criteria) {
        mutableStateOf(criteria.minimumPrice?.toString().orEmpty())
    }
    var maximumPrice by remember(criteria) {
        mutableStateOf(criteria.maximumPrice?.toString().orEmpty())
    }
    var location by remember(criteria) { mutableStateOf(criteria.location) }
    var includeUnavailable by remember(criteria) {
        mutableStateOf(criteria.includeUnavailable)
    }
    var sort by remember(criteria) { mutableStateOf(criteria.sort) }

    val minimumValue = minimumPrice.toIntOrNull()
    val maximumValue = maximumPrice.toIntOrNull()
    val validPriceRange = minimumValue == null ||
        maximumValue == null ||
        minimumValue <= maximumValue

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Filtry i sortowanie",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text("Kategoria", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("Wszystkie") }
                    )
                }
                items(SampleData.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id },
                        label = { Text("${category.symbol} ${category.name}") }
                    )
                }
            }

            Text("Cena", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minimumPrice,
                    onValueChange = { minimumPrice = it.filter(Char::isDigit).take(9) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Od") },
                    suffix = { Text("zł") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maximumPrice,
                    onValueChange = { maximumPrice = it.filter(Char::isDigit).take(9) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Do") },
                    suffix = { Text("zł") },
                    singleLine = true,
                    isError = !validPriceRange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            if (!validPriceRange) {
                Text(
                    text = "Cena maksymalna nie może być niższa od minimalnej.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lokalizacja") },
                placeholder = { Text("np. Tarninów") },
                singleLine = true
            )

            Text("Sortowanie", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ListingSort.entries, key = { it.name }) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = {
                            Text(
                                when (option) {
                                    ListingSort.NEWEST -> "Najnowsze"
                                    ListingSort.OLDEST -> "Najstarsze"
                                    ListingSort.PRICE_ASCENDING -> "Cena rosnąco"
                                    ListingSort.PRICE_DESCENDING -> "Cena malejąco"
                                    ListingSort.TITLE_ASCENDING -> "Nazwa A–Z"
                                }
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pokaż niedostępne", fontWeight = FontWeight.Bold)
                    Text(
                        "Uwzględnij sprzedane i wygasłe oferty.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = includeUnavailable,
                    onCheckedChange = { includeUnavailable = it }
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    onApply(
                        criteria.copy(
                            categoryId = categoryId,
                            minimumPrice = minimumValue,
                            maximumPrice = maximumValue,
                            location = location.trim(),
                            includeUnavailable = includeUnavailable,
                            sort = sort
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = validPriceRange
            ) {
                Text("Zastosuj filtry")
            }

            OutlinedButton(
                onClick = {
                    onApply(ListingSearchCriteria(query = criteria.query))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wyczyść filtry")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
