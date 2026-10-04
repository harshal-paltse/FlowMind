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
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SignInState {
    object Idle    : SignInState()
    object Loading : SignInState()
    object Success : SignInState()
    data class Error(val message: String) : SignInState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val googleClientId =
        "526825237012-sqlo61p2os4htv6uatn302ucm44rttsa.apps.googleusercontent.com"

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState

    /** Returns the Intent that starts the Google account chooser. */
    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /** Call this with the Intent returned from the sign-in activity result. */
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

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun resetState() {
        _signInState.value = SignInState.Idle
    }
}
