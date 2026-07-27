package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.model.UserProfile

@Composable
fun ProfileScreen(
    profile: UserProfile,
    listingCount: Int,
    favoriteCount: Int,
    onLogin: (name: String, email: String) -> Unit,
    onLogout: () -> Unit
) {
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var email by rememberSaveable(profile.email) { mutableStateOf(profile.email) }
    var message by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(22.dp)) {
                Text("Profil", color = MaterialTheme.colorScheme.onPrimary, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Twoje konto i aktywność lokalna", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Text((profile.name.ifBlank { "LR" }).take(2).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(if (profile.loggedIn) profile.name else "Gość", fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text(if (profile.loggedIn) profile.email else "Uzupełnij dane profilu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Ogłoszenia", listingCount.toString(), Modifier.weight(1f))
                        StatCard("Ulubione", favoriteCount.toString(), Modifier.weight(1f))
                    }
                    if (profile.loggedIn) {
                        ProfileOption("Moje ogłoszenia", "Zarządzaj opublikowanymi ofertami")
                        ProfileOption("Ulubione", "Wróć do zapisanych ogłoszeń")
                        ProfileOption("Ustawienia", "Wygląd, prywatność i powiadomienia")
                        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Wyloguj") }
                    } else {
                        Text("Podgląd formularza profilu", fontWeight = FontWeight.Bold)
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Imię") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("E-mail") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        Button(
                            onClick = {
                                if (name.trim().length >= 2 && email.contains("@")) onLogin(name.trim(), email.trim()) else message = "Uzupełnij poprawnie pola."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Zapisz profil", fontWeight = FontWeight.Bold) }
                        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileOption(title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
