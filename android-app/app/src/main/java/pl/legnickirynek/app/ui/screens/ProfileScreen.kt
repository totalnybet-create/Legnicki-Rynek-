package pl.legnickirynek.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.legnickirynek.app.model.AuthMode
import pl.legnickirynek.app.model.UserProfile
import pl.legnickirynek.app.ui.AuthViewModel

@Composable
fun ProfileScreen(
    profile: UserProfile,
    listingCount: Int,
    favoriteCount: Int,
    onOpenMyListings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable {
        mutableStateOf(if (authState.apiAvailable) AuthMode.LOGIN else AuthMode.LOCAL)
    }

    LaunchedEffect(authState.apiAvailable) {
        if (!authState.apiAvailable && mode != AuthMode.LOCAL) {
            mode = AuthMode.LOCAL
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Twój profil",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (profile.loggedIn) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            profile.name,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            profile.email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (profile.remoteSession) {
                                "Konto zsynchronizowane z serwerem"
                            } else {
                                "Profil lokalny na tym urządzeniu"
                            },
                            color = if (profile.remoteSession) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStat(
                        icon = Icons.Default.Inventory2,
                        value = listingCount.toString(),
                        label = "Moje ogłoszenia",
                        onClick = onOpenMyListings,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStat(
                        icon = Icons.Default.Favorite,
                        value = favoriteCount.toString(),
                        label = "Ulubione",
                        onClick = onOpenFavorites,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        authViewModel.logout(profile.remoteSession)
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !authState.inProgress
                ) {
                    if (authState.inProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Wyloguj")
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Konto użytkownika",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (authState.apiAvailable) {
                                "Zaloguj się lub utwórz konto, aby synchronizować ogłoszenia między urządzeniami."
                            } else {
                                "Serwer kont nie jest jeszcze skonfigurowany. Możesz używać profilu lokalnego."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (authState.apiAvailable) {
                                FilterChip(
                                    selected = mode == AuthMode.LOGIN,
                                    onClick = {
                                        mode = AuthMode.LOGIN
                                        authViewModel.clearError()
                                    },
                                    label = { Text("Logowanie") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = mode == AuthMode.REGISTER,
                                    onClick = {
                                        mode = AuthMode.REGISTER
                                        authViewModel.clearError()
                                    },
                                    label = { Text("Rejestracja") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            FilterChip(
                                selected = mode == AuthMode.LOCAL,
                                onClick = {
                                    mode = AuthMode.LOCAL
                                    authViewModel.clearError()
                                },
                                label = { Text("Lokalnie") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (mode != AuthMode.LOGIN) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it.take(80)
                                    authViewModel.clearError()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Imię") },
                                singleLine = true,
                                enabled = !authState.inProgress
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it.take(160)
                                authViewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-mail") },
                            singleLine = true,
                            enabled = !authState.inProgress,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            )
                        )

                        if (mode != AuthMode.LOCAL) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it.take(128)
                                    authViewModel.clearError()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Hasło") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                enabled = !authState.inProgress,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password
                                ),
                                supportingText = {
                                    Text("Minimum 8 znaków")
                                }
                            )
                        }

                        if (mode == AuthMode.LOCAL) {
                            Text(
                                text = "Profil lokalny nie synchronizuje sesji konta między urządzeniami.",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 13.sp
                            )
                        }

                        authState.error?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (mode == AuthMode.LOCAL) {
                                    onLogin(name, email)
                                }
                                authViewModel.authenticate(
                                    name = name,
                                    email = email,
                                    password = password,
                                    mode = mode
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !authState.inProgress
                        ) {
                            if (authState.inProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    when (mode) {
                                        AuthMode.LOGIN -> "Zaloguj"
                                        AuthMode.REGISTER -> "Utwórz konto"
                                        AuthMode.LOCAL -> "Używaj lokalnie"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
