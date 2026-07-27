package pl.legnickirynek.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.domain.ListingValidationError
import pl.legnickirynek.app.domain.ListingValidator
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

@Composable
fun AddListingScreen(onListingCreated: (Listing) -> Unit) {
    ListingFormScreen(
        initialListing = null,
        onSave = onListingCreated
    )
}

@Composable
fun EditListingScreen(
    listing: Listing,
    onListingUpdated: (Listing) -> Unit,
    onBack: () -> Unit
) {
    ListingFormScreen(
        initialListing = listing,
        onSave = onListingUpdated,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListingFormScreen(
    initialListing: Listing?,
    onSave: (Listing) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val formKey = initialListing?.id ?: "new-listing"
    var title by rememberSaveable(formKey) { mutableStateOf(initialListing?.title.orEmpty()) }
    var price by rememberSaveable(formKey) {
        mutableStateOf(initialListing?.price?.toString().orEmpty())
    }
    var location by rememberSaveable(formKey) {
        mutableStateOf(initialListing?.location ?: "Legnica")
    }
    var description by rememberSaveable(formKey) {
        mutableStateOf(initialListing?.description.orEmpty())
    }
    var categoryId by rememberSaveable(formKey) {
        mutableStateOf(initialListing?.categoryId ?: SampleData.categories.first().id)
    }
    var imageUris by remember(formKey) {
        mutableStateOf(initialListing?.imageUris.orEmpty())
    }
    var message by rememberSaveable(formKey) { mutableStateOf("") }
    var messageIsError by rememberSaveable(formKey) { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { selectedUris ->
        selectedUris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        imageUris = (imageUris + selectedUris.map { it.toString() })
            .distinct()
            .take(8)
        message = ""
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Wróć",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            if (initialListing == null) "Dodaj ogłoszenie" else "Edytuj ogłoszenie",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (initialListing == null) {
                                "Uzupełnij dane i dodaj zdjęcia oferty."
                            } else {
                                "Zmień dane, zdjęcia lub opis ogłoszenia."
                            },
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.74f)
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Zdjęcia", fontWeight = FontWeight.Bold)
                Text(
                    "Dodaj do 8 zdjęć. Pierwsze zdjęcie będzie miniaturą ogłoszenia.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                FilledTonalButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    enabled = imageUris.size < 8
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (imageUris.isEmpty()) {
                            "Wybierz zdjęcia"
                        } else {
                            "Dodaj kolejne (${imageUris.size}/8)"
                        }
                    )
                }

                if (imageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(imageUris, key = { it }) { uri ->
                            Box {
                                Card(
                                    modifier = Modifier.size(112.dp),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Wybrane zdjęcie ogłoszenia",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        imageUris = imageUris.filterNot { it == uri }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                            RoundedCornerShape(50)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Usuń zdjęcie"
                                    )
                                }
                            }
                        }
                    }
                }

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
                                val now = System.currentTimeMillis()
                                val numericPrice = requireNotNull(price.toIntOrNull())
                                onSave(
                                    Listing(
                                        id = initialListing?.id ?: "listing-$now",
                                        title = title.trim(),
                                        price = numericPrice,
                                        location = location.trim(),
                                        categoryId = categoryId,
                                        description = description.trim(),
                                        imageUris = imageUris,
                                        sellerName = initialListing?.sellerName ?: "Użytkownik",
                                        createdAt = initialListing?.createdAt ?: now,
                                        updatedAt = now,
                                        status = initialListing?.status ?: ListingStatus.ACTIVE,
                                        isFavorite = initialListing?.isFavorite ?: false
                                    )
                                )
                                message = if (initialListing == null) {
                                    "Ogłoszenie zostało opublikowane."
                                } else {
                                    "Zmiany zostały zapisane."
                                }
                                messageIsError = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Text(
                        if (initialListing == null) "Opublikuj ogłoszenie" else "Zapisz zmiany",
                        fontWeight = FontWeight.ExtraBold
                    )
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
                    "Dane i wybrane zdjęcia są obecnie przechowywane lokalnie na tym urządzeniu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}
