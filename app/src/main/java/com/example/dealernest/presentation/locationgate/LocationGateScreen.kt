package com.example.dealernest.presentation.locationgate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.dealernest.core.location.APP_GATE_PERMISSIONS
import com.example.dealernest.core.location.hasCameraPermission
import com.example.dealernest.core.location.hasLocationPermission
import com.example.dealernest.presentation.components.PrimaryButton
import com.example.dealernest.ui.theme.ActionBlue

/**
 * Blocks all navigation until location AND camera are both granted, requested together as soon
 * as the app opens (per the SRS: "the application cannot be used unless location permission is
 * granted" - camera is required upfront too, for QR scanning). Sits before Routes.SPLASH.
 */
@Composable
fun LocationGateScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    var wasDenied by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    fun allGranted() = context.hasLocationPermission() && context.hasCameraPermission()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (allGranted()) {
            onGranted()
        } else {
            wasDenied = true
            val activity = context as? Activity
            permanentlyDenied = activity != null && APP_GATE_PERMISSIONS.none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (allGranted()) onGranted() else launcher.launch(APP_GATE_PERMISSIONS)
    }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ActionBlue.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = ActionBlue,
                )
            }
            Text(
                "Allow location & camera",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "Needed to continue using DealerNest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            if (permanentlyDenied) {
                PrimaryButton(
                    text = "Open Settings",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )
            } else {
                PrimaryButton(
                    text = if (wasDenied) "Try Again" else "Allow",
                    onClick = { launcher.launch(APP_GATE_PERMISSIONS) },
                )
            }
        }
    }
}
