package com.gefuhlskarte.citymood.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isUserLoggedIn(): Boolean = currentUser != null

    fun login(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Completează toate câmpurile!")
            return
        }

        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && auth.currentUser != null) {
                    onSuccess(auth.currentUser!!)
                } else {
                    onError(task.exception?.localizedMessage ?: "Autentificare eșuată.")
                }
            }
    }

    fun register(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Completează toate câmpurile!")
            return
        }
        if (pass.length < 6) {
            onError("Parola trebuie să conțină cel puțin 6 caractere.")
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && auth.currentUser != null) {
                    onSuccess(auth.currentUser!!)
                } else {
                    onError(task.exception?.localizedMessage ?: "Înregistrare eșuată.")
                }
            }
    }

    fun logout() {
        auth.signOut()
    }
}