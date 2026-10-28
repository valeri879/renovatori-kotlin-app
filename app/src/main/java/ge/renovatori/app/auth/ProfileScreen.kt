package ge.renovatori.app.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ge.renovatori.app.R
import ge.renovatori.app.network.ApiConfig
import ge.renovatori.app.network.RemoteImage
import ge.renovatori.app.ui.formatPhone
import ge.renovatori.app.ui.phoneUri
import ge.renovatori.app.ui.theme.success
import kotlinx.coroutines.launch

/** "My profile" (web `/profile`): photo, name, e-mail with its verification state, phone, and logout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: User, onLoggedOut: () -> Unit, onEdit: () -> Unit, onChangePassword: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                AuthSession.refreshUser()
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedCard(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Text("პროფილი", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                // Web card header buttons.
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onChangePassword, modifier = Modifier.weight(1f)) { Text("პაროლის შეცვლა") }
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("რედაქტირება") }
                }
                HorizontalDivider()
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    val avatarModifier = Modifier.size(120.dp).clip(CircleShape)
                    // Users without a photo get the web's neutral avatar.
                    if (user.profileImage != null) {
                        RemoteImage(
                            url = ApiConfig.imageUrl("/uploads/small/${user.profileImage}"),
                            contentDescription = "${user.firstName} ${user.lastName}",
                            modifier = avatarModifier
                        )
                    } else {
                        Image(
                            painterResource(R.drawable.unisex_avatar),
                            contentDescription = "${user.firstName} ${user.lastName}",
                            contentScale = ContentScale.Crop,
                            modifier = avatarModifier
                        )
                    }
                }
                ProfileRow("👤 სახელი:", user.firstName)
                HorizontalDivider()
                ProfileRow("👥 გვარი:", user.lastName)
                HorizontalDivider()
                // Verification is shown in words as well as color (web: green / red e-mail).
                ProfileRow(
                    "✉️ ელ-ფოსტა:",
                    user.email,
                    valueColor = if (user.isVerified) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
                    supporting = if (user.isVerified) "ელ-ფოსტა დადასტურებულია" else "ელ-ფოსტა არ არის დადასტურებული",
                    onClick = { uriHandler.openUri("mailto:${user.email}") }
                )
                HorizontalDivider()
                val telUri = phoneUri(user.phoneNumber)
                ProfileRow(
                    "📱 ტელეფონი:",
                    formatPhone(user.phoneNumber),
                    onClick = telUri?.let { { uriHandler.openUri(it) } }
                )
            }
            Button(
                onClick = {
                    isLoggingOut = true
                    scope.launch {
                        AuthSession.logout()
                        onLoggedOut()
                    }
                },
                enabled = !isLoggingOut,
                // Web `btn-warning`.
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color(0xFF212529)),
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(top = 16.dp)
            ) { Text("გასვლა") }
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    supporting: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        overlineContent = { Text(label, fontWeight = FontWeight.Bold) },
        headlineContent = { Text(value.ifBlank { "-" }, color = valueColor) },
        supportingContent = supporting?.let { { Text(it) } },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
