package com.example.dealernest.presentation.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.dealernest.core.session.SessionState
import com.example.dealernest.domain.model.AdminProfile
import com.example.dealernest.presentation.components.InfoRow
import com.example.dealernest.presentation.components.SectionCard
import com.example.dealernest.presentation.components.StatusBadge
import com.example.dealernest.presentation.components.StatusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val state = sessionState) {
            is SessionState.LoggedIn -> ProfileContent(
                profile = state.profile,
                onLogoutClick = { viewModel.logout(onLoggedOut) },
            )
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun ProfileContent(profile: AdminProfile, onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.pictureUrl != null) {
                    AsyncImage(
                        model = profile.pictureUrl,
                        contentDescription = profile.name,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.padding(start = 12.dp))
                Column {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        profile.email ?: profile.phone.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusBadge(
                        text = profile.statusText,
                        tone = if (profile.status) StatusTone.SUCCESS else StatusTone.DANGER,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Contact", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            InfoRow(label = "Email", value = profile.email ?: "-")
            InfoRow(label = "Phone", value = profile.phone ?: "-")
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text("Role", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            InfoRow(label = "Roles", value = profile.roles.joinToString().ifBlank { "-" })
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            // The API doesn't return Employee ID/Address/Joining Date/Last Login - see
            // .claude/docs/auth-and-permissions.md "Profile fields gap". createdAt is shown as
            // a "member since" stand-in, not a real joining-date field.
            InfoRow(label = "Member since", value = profile.createdAt?.take(10) ?: "-")
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.padding(start = 8.dp))
            Text("Log Out")
        }
    }
}
