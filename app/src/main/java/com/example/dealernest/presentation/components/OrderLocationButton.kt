package com.example.dealernest.presentation.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dealernest.ui.theme.ActionBlue
import com.example.dealernest.ui.theme.OnActionBlue

// Shared by the Order list card and the Invoice (Order details) screen - both surfaces need the
// same "tap to open directions" affordance for Order.googleMapsUrl. Distance and the maps URL
// are both server-computed (OrderLocationDto) - this just displays and opens them, no distance
// math or URL-building here. Solid-fill button styling (not just a tinted pill) is deliberate -
// this needs to read as an obvious, high-contrast call-to-action, spotted and tapped at a
// glance rather than something a user has to notice is tappable.
@Composable
fun OrderLocationButton(distanceText: String?, mapsUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ActionBlue)
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                }.onFailure { if (it !is ActivityNotFoundException) throw it }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(OnActionBlue.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = OnActionBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Get Directions",
                style = MaterialTheme.typography.labelSmall,
                color = OnActionBlue.copy(alpha = 0.85f),
            )
            Text(
                distanceText ?: "View on map",
                style = MaterialTheme.typography.titleSmall,
                color = OnActionBlue,
                fontWeight = FontWeight.Bold,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open in Maps",
            tint = OnActionBlue,
            modifier = Modifier.size(20.dp),
        )
    }
}
