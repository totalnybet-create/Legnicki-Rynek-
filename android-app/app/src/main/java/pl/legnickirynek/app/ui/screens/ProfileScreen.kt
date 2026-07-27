package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.legnickirynek.app.domain.ProfileValidationError
import pl.legnickirynek.app.domain.ProfileValidator
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
    var messageIsError by rememberSaveable { mutableStateOf(false) }

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
                    "Profil",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Zarządzaj kontem i swoimi ogłoszeniami.",
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.74f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (profile.loggedIn) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.name.take(1).uppercase().ifBlank { "?" },
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            "Witaj, ${profile.name}",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            profile.email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Moje ogłoszenia: $listingCount",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Ulubione: $favoriteCount",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                onLogout()
                                message = "Wylogowano."
                                messageIsError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Wyloguj", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "Zaloguj się",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Na obecnym etapie dane konta są zapisywane lokalnie na telefonie.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it.take(60)
                                message = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Imię") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it.take(120)
                                message = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-mail") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Button(
                            onClick = {
                                when (ProfileValidator.validate(name, email)) {
                                    ProfileValidationError.NAME_REQUIRED -> {
                                        message = "Podaj imię."
                                        messageIsError = true
                                    }

                                    ProfileValidationError.EMAIL_INVALID -> {
                                        message = "Podaj prawidłowy adres e-mail."
                                        messageIsError = true
                                    }

                                    null -> {
                                        onLogin(name.trim(), email.trim())
                                        message = "Zalogowano."
                                        messageIsError = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Zaloguj", fontWeight = FontWeight.ExtraBold)
                        }
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
                }
            }
        }
    }
}
