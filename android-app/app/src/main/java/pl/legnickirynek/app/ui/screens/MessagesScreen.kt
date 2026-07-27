package pl.legnickirynek.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData

@Composable
fun MessagesScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val conversations = SampleData.conversations.filter {
        query.isBlank() || it.person.contains(query, true) || it.listingTitle.contains(query, true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(20.dp)) {
                Text("Wiadomości", color = MaterialTheme.colorScheme.onPrimary, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Rozmowy dotyczące ogłoszeń i zakupów", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    placeholder = { Text("Szukaj rozmowy") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
        if (conversations.isEmpty()) {
            item { Text("Nie znaleziono rozmów.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(conversations, key = { it.id }) { conversation ->
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp).fillMaxWidth().clickable { selectedId = if (selectedId == conversation.id) null else conversation.id }.semantics { role = Role.Button },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Text(conversation.person.take(1).uppercase(), fontSize = 21.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(conversation.person, Modifier.weight(1f), fontWeight = if (conversation.unread) FontWeight.Black else FontWeight.Bold)
                                    Text(conversation.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Text(conversation.listingTitle, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(conversation.lastMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (conversation.unread) Box(Modifier.padding(start = 9.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                        AnimatedVisibility(selectedId == conversation.id) {
                            Text(
                                "Podgląd rozmowy — interfejs demonstracyjny bez wysyłania i synchronizacji.",
                                modifier = Modifier.padding(top = 14.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
