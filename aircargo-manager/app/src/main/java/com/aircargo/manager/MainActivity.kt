package com.aircargo.manager

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.aircargo.manager.ui.AppNavHost
import com.aircargo.manager.ui.theme.AirCargoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35 Edge-to-Edge 강제 대응 (상태바 아이콘 흰색)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            AirCargoTheme {
                AppNavHost()
            }
        }
    }
}
