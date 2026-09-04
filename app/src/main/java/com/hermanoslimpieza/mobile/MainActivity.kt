package com.hermanoslimpieza.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hermanoslimpieza.mobile.ui.HermanosRoot
import com.hermanoslimpieza.mobile.ui.theme.HermanosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HermanosApp
        setContent {
            HermanosTheme {
                HermanosRoot(app.apiClient.api, app.tokenStore, app.chatCache)
            }
        }
    }
}
