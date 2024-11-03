package com.reginaldateya.healthlinker

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.reginaldateya.healthlinker.ui.theme.HealthLinkerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class PatientOtpVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        enableEdgeToEdge()
        setContent {
            HealthLinkerTheme {
                PatientOtpVerificationScreen(activity = this, phoneNumber = phoneNumber, navController = rememberNavController())
            }
        }
    }
}



class PatientOtpVerificationActivityProvider : PreviewParameterProvider<Activity> {
    override val values: Sequence<Activity> = sequenceOf(Activity())
}

@Composable
fun PatientOtpVerificationScreen(activity: Activity, phoneNumber: String, navController: NavController) {
    var otp by remember { mutableStateOf("") }
    var remainingTime by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var storedVerificationId by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Start OTP verification when the screen loads
    LaunchedEffect(Unit) {
        sendPatientOtp(phoneNumber, activity, navController) { verificationId ->
            storedVerificationId = verificationId
        }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
            isTimerRunning = false
        }
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage!!,
                duration = SnackbarDuration.Short
            )
            snackbarMessage = null // Reset the message after showing
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) } // Add SnackbarHost
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Verify your phone number", fontSize = 20.sp)
            Text("Enter the OTP sent to $phoneNumber", fontSize = 16.sp)

            OutlinedTextField(
                value = otp,
                onValueChange = {
                    otp = it
                    showError = false
                },
                label = { Text("OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = showError
            )

            if (showError) {
                Text(
                    text = "Invalid OTP",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button for resending OTP
            if (isTimerRunning) {
                Text(
                    text = "Time remaining: $remainingTime seconds",
                    color = if (remainingTime <= 10) Color.Red else Color.Black
                )
            } else {
                Text("Time expired! Resend OTP?", color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    isTimerRunning = true
                    remainingTime = 60
                    coroutineScope.launch {
                        sendPatientOtp(phoneNumber, activity, navController) { verificationId ->
                            storedVerificationId = verificationId
                        }
                    }
                }) {
                    Text("Resend OTP")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))


            Button(
                onClick = {
                    isLoading = true
                    if (otp.isDigitsOnly() && otp.length == 6) {
                        verifyPatientOtp(
                            otp = otp,
                            phoneNumber = phoneNumber,  // Pass phoneNumber here
                            navController = navController,
                            onVerificationSuccess = {
                                isLoading = false
                                navController.navigate("doctorHome")
                            },
                            onVerificationFailure = { exception ->
                                isLoading = false
                                snackbarMessage = "Invalid OTP"
                                Log.e("OTP Verification", "Verification failed", exception)
                            }
                        )
                    } else {
                        snackbarMessage = "Invalid OTP"
                        isLoading = false
                    }
                },
                enabled = otp.isDigitsOnly() && otp.length == 6 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Verify")
                }
            }
        }
    }

    LaunchedEffect(showError) {
        if (showError) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Invalid OTP",
                    duration = SnackbarDuration.Short
                )
            }
            showError = false // Reset showError after showing Snackbar
        }
    }
}

var patientStoredVerificationId: String? = null
var patientResendToken: PhoneAuthProvider.ForceResendingToken? = null


fun sendPatientOtp(phoneNumber: String, activity: Activity, navController: NavController, onCodeSent: (String) -> Unit) {
    val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Verification successful, sign in directly if possible
                signInPatientWithPhoneAuthCredential(
                    credential = credential,
                    phoneNumber = phoneNumber, // Pass phoneNumber here
                    navController = navController,
                    onVerificationSuccess = {
                        navController.navigate("doctorHome") {
                            popUpTo("doctorRegistration") { inclusive = true }
                        }
                    },
                    onVerificationFailure = { error -> Log.e("Auto Verification", "Failed", error) }
                )
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("OTP Error", "Verification failed", e)
                // Handle the error in the UI
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                patientStoredVerificationId = verificationId
                patientResendToken = token
                onCodeSent(verificationId) // Execute the callback
            }
        })
        .build()
    PhoneAuthProvider.verifyPhoneNumber(options)
}

fun verifyPatientOtp(
    otp: String,
    phoneNumber: String, // Added phoneNumber parameter
    navController: NavController,
    onVerificationSuccess: () -> Unit,
    onVerificationFailure: (Exception) -> Unit
) {
    patientStoredVerificationId?.let { verificationId ->
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInPatientWithPhoneAuthCredential(
            credential = credential,
            phoneNumber = phoneNumber, // Pass phoneNumber here
            navController = navController,
            onVerificationSuccess = {
                // Navigate to doctor home screen and clear back stack
                navController.navigate("doctorHome") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
                onVerificationSuccess() // Call original onVerificationSuccess if needed
            },
            onVerificationFailure = onVerificationFailure
        )
    } ?: run {
        onVerificationFailure(Exception("Verification ID is missing. Please request a new OTP."))
    }
}

fun signInPatientWithPhoneAuthCredential(
    credential: PhoneAuthCredential,
    phoneNumber: String, // Added phoneNumber parameter
    navController: NavController,
    onVerificationSuccess: () -> Unit,
    onVerificationFailure: (Exception) -> Unit
) {
    val auth = Firebase.auth
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d("TAG", "signInWithCredential:success")
                val user = auth.currentUser

                // Add user data to Firestore
                val db = Firebase.firestore
                val userRef = db.collection("users").document(user!!.uid)
                val userData = hashMapOf(
                    "phoneNumber" to phoneNumber // Use phoneNumber here
                    // Add other user data as needed
                )

                userRef.set(userData)
                    .addOnSuccessListener {
                        Log.d("TAG", "User data added to Firestore successfully")
                        // Navigate to doctor home screen and clear back stack
                        navController.navigate("patientHome") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                        onVerificationSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.w("TAG", "Error adding user data to Firestore", e)
                        onVerificationFailure(e) // Pass the exception to onVerificationFailure
                    }

            } else {
                // Sign in failed, display a message and update the UI
                Log.w("TAG", "signInWithCredential:failure", task.exception)
                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    onVerificationFailure(task.exception as Exception)
                }
            }
        }
}
@Preview(showBackground = true)
@Composable
fun PatientOtpVerificationScreenPreview() {
    HealthLinkerTheme {
        val phoneNumber = "+15551234567"
        val activity = ComponentActivity()
        PatientOtpVerificationScreen(activity = activity, phoneNumber= phoneNumber, navController = rememberNavController())
    }
}