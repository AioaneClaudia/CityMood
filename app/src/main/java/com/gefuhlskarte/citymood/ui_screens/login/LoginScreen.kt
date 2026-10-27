package com.gefuhlskarte.citymood.ui_screens.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gefuhlskarte.citymood.repository.AuthManager

@Composable
fun LoginScreen(
    authManager: AuthManager = remember { AuthManager() },
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegisterMode) "Creare Cont CityMood" else "Autentificare CityMood",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Parolă") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isRegisterMode) {
                    authManager.register(
                        email = email,
                        pass = password,
                        onSuccess = { onLoginSuccess() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                } else {
                    authManager.login(
                        email = email,
                        pass = password,
                        onSuccess = { onLoginSuccess() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRegisterMode) "Înregistrează-te" else "Conectează-te")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(
                if (isRegisterMode) "Ai deja un cont? Conectează-te"
                else "Nu ai cont? Creează cont nou"
            )
        }
    }
}