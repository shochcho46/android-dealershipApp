package com.example.dealernest

import android.app.Application
import com.example.dealernest.core.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Extends the generated Hilt_DealerNestApp (not Application directly) because this project
// doesn't apply the com.google.dagger.hilt.android Gradle plugin - it's incompatible with
// AGP 9 as of Hilt 2.57.1 (https://github.com/google/dagger/issues/4944). The plugin's only
// job for a single-module app is a bytecode transform that inserts this same superclass
// automatically; doing it by hand here is the documented fallback (dagger.dev/hilt/gradle-setup.html).
// The explicit Application::class argument is required without the plugin - Hilt can no
// longer infer the base class from a plugin-transformed supertype.
@HiltAndroidApp(Application::class)
class DealerNestApp : Hilt_DealerNestApp() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
    }
}

