package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.data.SampleData

@Composable
fun MessagesScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(20.dp)
            ) {
                Text(
                    "Wiadomości",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Rozmowy dotyczące Twoich ogłoszeń i zakupów.",
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.74f)
                )
            }
        }

        if (SampleData.conversations.isEmpty()) {
            item {
                Text(
                    "Nie masz jeszcze żadnych wiadomości.",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(SampleData.conversations, key = { it.id }) { conversation ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                conversation.person.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    conversation.person,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (conversation.unread) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.Bold
                                    }
                                )
                                Text(
                                    conversation.time,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                conversation.listingTitle,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                conversation.lastMessage,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (conversation.unread) {
                            Spacer(Modifier.width(9.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
