package com.example.flowmind.auth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the authentication lifecycle state for the sign-in screen.
 */
sealed class SignInState {
    /** No sign-in has been attempted yet. */
    object Idle    : SignInState()
    /** A sign-in attempt is currently in progress. */
    object Loading : SignInState()
    /** The user has been successfully authenticated with Firebase. */
    object Success : SignInState()
    /** Authentication failed with a human-readable [message]. */
    data class Error(val message: String) : SignInState()
}

/**
 * ViewModel for the authentication flow.
 *
 * Delegates all repository work to [AuthRepository] and exposes a single
 * [signInState] stream that the UI can collect to drive its render logic.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val googleClientId =
        "526825237012-sqlo61p2os4htv6uatn302ucm44rttsa.apps.googleusercontent.com"

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)

    /**
     * Stream of the current authentication state.
     * Collect this in the UI to react to sign-in progress, success, and failure.
     */
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    /**
     * Display name of the currently signed-in user, or `null` when no user is authenticated.
     * This is a convenience accessor; the source of truth is [AuthRepository].
     */
    val displayName: String?
        get() = authRepository.currentDisplayName()

    /**
     * Returns the [Intent] that launches the Google account chooser dialog.
     * Pass the resulting Intent to [android.app.Activity.startActivityForResult].
     */
    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /**
     * Processes the result Intent returned from the Google Sign-In activity.
     *
     * On success the state transitions to [SignInState.Success].
     * On any failure it transitions to [SignInState.Error] with an appropriate message.
     *
     * @param data The Intent delivered by `onActivityResult`.
     */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            try {
                val task    = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                    ?: throw IllegalStateException("Google did not return an ID token")

                val result = authRepository.signInWithGoogle(idToken)
                _signInState.value = if (result.isSuccess) {
                    SignInState.Success
                } else {
                    SignInState.Error(result.exceptionOrNull()?.message ?: "Firebase sign-in failed")
                }
            } catch (e: ApiException) {
                _signInState.value = SignInState.Error("Google Sign-In error (code ${e.statusCode})")
            } catch (e: Exception) {
                _signInState.value = SignInState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Returns `true` when a Firebase user session is currently active. */
    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    /**
     * Signs the current user out and resets [signInState] to [SignInState.Idle].
     * Safe to call even when no user is signed in.
     */
    fun signOut() {
        authRepository.signOut()
        _signInState.value = SignInState.Idle
    }

    /**
     * Resets [signInState] to [SignInState.Idle] without signing the user out.
     * Useful for clearing transient error messages in the UI.
     */
    fun resetState() {
        _signInState.value = SignInState.Idle
    }
}
