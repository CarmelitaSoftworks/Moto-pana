package com.carmelitasoftworks.motointercom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme
import com.carmelitasoftworks.motointercom.navigation.MotoIntercomApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so force light (white) system bar icons.
        val darkBars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = darkBars, navigationBarStyle = darkBars)
        super.onCreate(savedInstanceState)
        setContent {
            MotoIntercomTheme {
                MotoIntercomApp()
            }
        }
    }
}
