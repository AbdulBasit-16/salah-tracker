package com.salah.tracker.ui.profile

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    // Web Client ID from Google Cloud Console
    private val WEB_CLIENT_ID = "425162514862-mq2v68l7t02sje70i016pr0dct3k3a0f.apps.googleusercontent.com"

    suspend fun signIn(): GoogleAuthResult {
        try {
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return GoogleAuthResult.Success(
                    id = googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id, // id is typically the email address
                    displayName = googleIdTokenCredential.displayName ?: "Google User",
                    profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                )
            } else {
                return GoogleAuthResult.Error("Unexpected credential type")
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthManager", "Sign-in failed", e)
            return GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Sign-in error", e)
            return GoogleAuthResult.Error(e.message ?: "An error occurred")
        }
    }
}

sealed class GoogleAuthResult {
    data class Success(
        val id: String,
        val email: String,
        val displayName: String,
        val profilePictureUri: String?
    ) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
}
