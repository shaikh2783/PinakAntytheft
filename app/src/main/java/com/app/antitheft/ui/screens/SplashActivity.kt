package com.app.antitheft.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.MainActivity
import kotlinx.coroutines.delay
import com.app.antitheft.R
import com.app.antitheft.ui.LoginActivity
import kotlinx.coroutines.flow.first

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Hide both bars; swipe up to reveal
            SideEffect {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }

            SplashScreen() // your composable
        }
    }
}

@Composable
fun SplashScreen() {
    val context = LocalContext.current // ✅ Get Activity context
    val dataStore = DataStoreManager.getInstance(context)

    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds

        val token = dataStore.getToken.first()

        if (!token.isNullOrEmpty()) {
            context.startActivity(Intent(context, MainActivity::class.java))
        } else {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }
        if (context is ComponentActivity) {
            context.finish() // ✅ close SplashActivity
        }
    }

    Box(
        modifier = Modifier.run {
            fillMaxSize()
                .background(color = colorResource(R.color.primary))
        },

        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(height = 192.dp, width = 192.dp),
                contentScale = ContentScale.Fit)

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "Pinak AntiTheft",
                style = TextStyle(
                    lineHeight = 24.sp,
                    fontSize = 34.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                ),
                textAlign = TextAlign.Center,
            )

        }
        Text(
            text = "Version 1.0.1",
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight(400),
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter) // ✅ stick to bottom
                .padding(bottom = 16.dp)
        )
    }
}
