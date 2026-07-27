package pl.legnickirynek.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing

@Composable
fun ListingDetailsScreen(
    listing: Listing,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var showContactHint by remember { mutableStateOf(false) }
    val price = NumberFormat.getIntegerInstance(Locale("pl", "PL")).format(listing.price) + " zł"
    val category = SampleData.categories.firstOrNull { it.id == listing.categoryId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(category?.symbol ?: "●", fontSize = 76.sp)
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) { Text("← Wróć") }
            FilledTonalButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = RoundedCornerShape(50)
            ) { Text(if (listing.isFavorite) "♥ Zapisane" else "♡ Zapisz") }
        }

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(listing.title, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(price, color = MaterialTheme.colorScheme.primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(category?.name ?: "Ogłoszenie")
                InfoPill(listing.location)
            }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Opis", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(listing.description, lineHeight = 23.sp)
                }
            }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("LR", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black) }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("Lokalny sprzedający", fontWeight = FontWeight.Bold)
                        Text("Odpowiada zwykle w ciągu godziny", fontSize = 13.sp)
                    }
                }
            }
            Button(
                onClick = { showContactHint = !showContactHint },
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { role = Role.Button },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Napisz wiadomość", fontWeight = FontWeight.Bold) }
            AnimatedVisibility(showContactHint) {
                Text(
                    "To jest demonstracyjny element UI. Obsługa wysyłania wiadomości nie jest częścią tej gałęzi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Box(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
