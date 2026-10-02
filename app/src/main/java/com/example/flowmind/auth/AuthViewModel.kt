package com.example.flowmind.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    val auth = FirebaseAuth.getInstance()
    
    // Configured Google Client ID
    val googleClientId = "526825237012-sqlo61p2os4htv6uatn302ucm44rttsa.apps.googleusercontent.com"
}
