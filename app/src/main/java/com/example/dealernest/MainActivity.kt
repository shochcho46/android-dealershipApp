package com.example.dealernest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dealernest.presentation.navigation.DealerNestNavHost
import com.example.dealernest.ui.theme.DealerNestTheme
import dagger.hilt.android.AndroidEntryPoint

// See DealerNestApp.kt for why this extends Hilt_MainActivity manually instead of using
// @AndroidEntryPoint's usual plugin-driven bytecode transform. The (ComponentActivity::class)
// argument tells the Hilt annotation processor what Hilt_MainActivity should itself extend,
// since it can no longer infer that from a plugin-transformed supertype.
@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DealerNestTheme {
                DealerNestNavHost()
            }
        }
    }
}
