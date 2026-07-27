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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import pl.legnickirynek.app.ui.theme.LegnicaBackground
import pl.legnickirynek.app.ui.theme.LegnicaCoral
import pl.legnickirynek.app.ui.theme.LegnicaMuted
import pl.legnickirynek.app.ui.theme.LegnicaNavy

@Composable
fun ProfileScreen() {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LegnicaBackground),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LegnicaNavy)
                    .padding(20.dp)
            ) {
                Text("Profil", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Zarządzaj kontem i swoimi ogłoszeniami.",
                    color = Color.White.copy(alpha = 0.74f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (loggedIn) {
                        Text("Witaj, $name", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        Text(email, color = LegnicaMuted)
                        Text("Moje ogłoszenia: 0", fontWeight = FontWeight.SemiBold)
                        Text("Ulubione: dostępne na ekranie głównym", color = LegnicaMuted)
                        Button(
                            onClick = {
                                loggedIn = false
                                message = "Wylogowano."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LegnicaNavy)
                        ) {
                            Text("Wyloguj", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Zaloguj się", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Logowanie jest na razie lokalną wersją demonstracyjną.",
                            color = LegnicaMuted
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Imię") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-mail") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Button(
                            onClick = {
                                if (name.trim().length < 2) {
                                    message = "Podaj imię."
                                } else if (!email.contains("@") || !email.contains(".")) {
                                    message = "Podaj prawidłowy adres e-mail."
                                } else {
                                    name = name.trim()
                                    email = email.trim()
                                    loggedIn = true
                                    message = "Zalogowano."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LegnicaCoral)
                        ) {
                            Text("Zaloguj", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            color = if (message == "Zalogowano." || message == "Wylogowano.") LegnicaNavy else LegnicaCoral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
