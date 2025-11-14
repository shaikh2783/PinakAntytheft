package com.app.antitheft.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.antitheft.R
import com.app.antitheft.viewmodel.LoginUiState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch


@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val systemUiController = rememberSystemUiController()
    val primaryColor = colorResource(R.color.primary)

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = primaryColor,
            darkIcons = false             // white status bar icons
        )
    }
    // Show error popup (snackbar)
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = primaryColor)
            .padding(horizontal = 18.dp)
    ) {

        // Snackbar on top layer
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "Log in",
                fontSize = 34.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(45.dp))

            Text("Your Email", color = Color.White)
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                hint = "Email ID",
                icon = Icons.Outlined.Email
            )

            Text("Your Password", color = Color.White)
            AuthTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                hint = "Password",
                icon = Icons.Outlined.VisibilityOff,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            CenterContinueButton(onSubmit = onSubmit)

            // Add spacing for center feel
            Spacer(modifier = Modifier.height(40.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp)
        ) {
            BottomSafetySection()
        }
    }
}


@Composable
fun CenterContinueButton(onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5271FF)),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Continue", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(20.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(!isPassword) }

    Box(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .border(
                width = 1.dp,
                color = Color(0x80CACACA),
                shape = RoundedCornerShape(25.dp)
            )
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(hint, color = Color(0x80CACACA)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White
                ),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.weight(1f),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation()
            )

            if (isPassword) {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFF2F2F2)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFF2F2F2)
                )
            }
        }
    }
}

@Composable
fun BottomSafetySection() {

    Column(
        modifier = Modifier.padding(vertical = 25.dp)
    ) {

        Text("About Data Safety",
            color = Color(0xFF5271FF),
            fontSize = 16.sp
        )

        Text("This Email will be use below purpose",
            color = Color.White,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Icon(Icons.Default.Security, contentDescription = null,
                tint = Color(0xFF5271FF), modifier = Modifier.size(50.dp))

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Row { DotText("Data Breaches") }
                Row { DotText("We will protect your data") }
                Row { DotText("will never share your data") }
            }
        }
    }
}

@Composable
fun DotText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Circle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}
