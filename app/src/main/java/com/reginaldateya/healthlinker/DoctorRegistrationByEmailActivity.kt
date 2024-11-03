package com.reginaldateya.healthlinker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.reginaldateya.healthlinker.ui.theme.HealthLinkerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

interface NavigationCallbackDoctor {
    fun navigateToDoctorHome()
}

class DoctorRegistrationByEmailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthLinkerTheme {
                DoctorRegistrationNavigation(this)
            }
        }

    }
}
@Composable
fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    onCountryCodeChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: DoctorRegistrationViewModel
) {
    val countryCodes = listOf("+254", "+1", "+44", "+91" /* ... other country codes */)
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.CenterVertically) { // Align items vertically
        // Dropdown for country codes
        OutlinedTextField(
            value = selectedCountryCode,
            onValueChange = { }, // Prevent direct input
            label = { Text("Code") },
            readOnly = true, // Prevent editing
            modifier = Modifier
                .weight(1f) // Occupy 1/3 of the width
                .padding(end = 8.dp), // Add spacing to the right
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowDropDown, // Change icon based on expanded state
                        contentDescription = if (expanded) "Show dropdown" else "Hide dropdown"
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            countryCodes.forEach { code ->
                DropdownMenuItem(
                    onClick = {
                        onCountryCodeChange(code)
                        onExpandedChange(false) // Close dropdown after selection
                    },
                    text = { Text(code) } // Display the code in the dropdown item
                )
            }
        }


        // Phone number input field
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(2f) // Occupy 2/3 of the width

        )
    }

    Spacer(modifier = Modifier.height(16.dp))


    Button(
        onClick = {
            if (phoneNumber.isNotBlank()) {
                viewModel.startPhoneNumberVerification(selectedCountryCode + phoneNumber, context)
            } else {
                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        },
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Text("Continue with phone number")
    }
}

@Composable
fun DoctorRegistrationByEmailScreen(
    navController: NavController
) {
    var isPhoneNumberRegistration by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope() // Create a coroutine scope
    val context = LocalContext.current
    var selectedCountryCode by remember { mutableStateOf("+254") } // Default to Kenya
    var inputtedPhoneNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val viewModel: DoctorRegistrationViewModel = viewModel() // Get the ViewModel

    // Check verification status when the screen is composed
    LaunchedEffect(viewModel) {
        viewModel.navigationCallback = object : NavigationCallbackDoctor {
            override fun navigateToDoctorHome() {
                navController.navigate("doctorHome")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create a Doctor account",
            color = Color(0xFF5579C6),
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 40.dp)
        )

        Text(
            text = "Join our network of health linkers and start your health journey",
            color = Color(0xFF5579C6),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 40.dp, bottom = 80.dp)
        )

        Button(
            onClick = { isPhoneNumberRegistration = !isPhoneNumberRegistration },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(
                text = if (isPhoneNumberRegistration) "Sign up with Email" else "Sign up with Phone Number",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        if (isPhoneNumberRegistration) {
            // Call PhoneNumberInput here, passing necessary parameters
            var phoneNumber by remember { mutableStateOf("") } // State for phone number
            PhoneNumberInput(
                phoneNumber = phoneNumber, // Pass the state variable
                onPhoneNumberChange = { phoneNumber = it }, // Update the state
                expanded = expanded,
                selectedCountryCode = selectedCountryCode,
                onExpandedChange = { expanded = it },
                onCountryCodeChange = { selectedCountryCode = it },
                viewModel = viewModel
            )
        } else {
            OutlinedTextField(
                value = email.text,
                onValueChange = { newText -> email = TextFieldValue(newText) },
                label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password.text,
                onValueChange = { newText -> password = TextFieldValue(newText) },
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword.text,
                onValueChange = { newText -> confirmPassword = TextFieldValue(newText) },
                label = { Text("Confirm Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = confirmPassword.text.isBlank() && showError ||
                        (password.text != confirmPassword.text && confirmPassword.text.isNotBlank() && showError),
                modifier = Modifier.fillMaxWidth()
            )

            if (showError && errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    showError = true
                    if (email.text.isBlank() || password.text.isBlank() || confirmPassword.text.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }

                    if (password.text != confirmPassword.text) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }

                    coroutineScope.launch {
                        val registrationSuccess = viewModel.registerUser(email.text.trim(), password.text.trim(), context)
                        if (registrationSuccess) {
                            // Registration and email sending were successful;
                            // now the verification check will happen in the ViewModel
                        } else {
                            Toast.makeText(context, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                // ... (Other button properties)
            ) {
                Text(text = "Continue with email", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add space between login and register card

        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append("Already an existing user? ")
            }
            pushStringAnnotation(tag = "URL", annotation = "doctorLogin")
            withStyle(
                style = SpanStyle(
                    color = Color.Green,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Sign in here")
            }
            pop()
        }

        ClickableText( // Now ClickableText can access annotatedString
            text = annotatedString,
            onClick = { offset ->
                annotatedString
                    .getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        navController.navigate("doctorLogin")
                    }
            }
        )
    }
}

class FirestoreRepository(private val auth: FirebaseAuth = Firebase.auth) {
    private val db = Firebase.firestore
    suspend fun registerUser(email: String, password: String): Boolean {
        // Core registration logic (unchanged)
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            true // Success
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Registration failed: ${e.message}", e)
            false // Failure
        }
    }

    // In FirestoreRepository
    suspend fun addVerifiedUser(userId: String, email: String): Boolean {
        val userDocRef = db.collection("users").document(userId)
        return try {
            val userData = hashMapOf(
                "email" to email,
                "userId" to userId,
                "isVerified" to true,
                "role" to "doctor" // Add role field
            )
            userDocRef.set(userData).await()
            true // Success
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error in addVerifiedUser: ${e.message}", e)
            false // Failure
        }
    }
}

class DoctorRegistrationViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()
    private val auth: FirebaseAuth = Firebase.auth // Declare auth here
    private var verificationId: String? = null // Store verification ID
    var navigationCallback: NavigationCallbackDoctor? = null

    fun startPhoneNumberVerification(phoneNumber: String, context: Context) {
        if (phoneNumber.isEmpty()) {
            // Display error message to the user
            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as ComponentActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Automatic sign-in success
                signInWithPhoneAuthCredential(credential, context)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // Verification failed
                Log.w("DoctorRegistrationViewModel", "onVerificationFailed", e)
                // Handle verification failure (e.g., show an error message)
            }

            // In DoctorRegistrationViewModel
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                this@DoctorRegistrationViewModel.verificationId = verificationId
            }
        })

            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, context: Context) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(context as Activity) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("DoctorRegistrationViewModel", "signInWithCredential:success")
                    val user = task.result?.user
                    // ... (Navigation to patient home screen) ...
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(
                        "DoctorRegistrationViewModel",
                        "signInWithCredential:failure",
                        task.exception
                    )
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(
                            context,
                            "Invalid OTP. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Verification failed. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }


    // In DoctorRegistrationViewModel
    suspend fun registerUser(email: String, password: String, context: Context): Boolean {
        val registrationSuccess = firestoreRepository.registerUser(email, password)
        if (registrationSuccess) {
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification().await()
                Toast.makeText(context, "Verification email sent. Please verify before proceeding.", Toast.LENGTH_SHORT).show()

                // Check verification status repeatedly until verified or error
                var isVerified = false
                var attempts = 0
                val maxAttempts = 10 // Limit the number of attempts

                while (!isVerified && attempts < maxAttempts) {
                    try {
                        withContext(Dispatchers.Main) {
                            user.reload().await() // Reload user to get updated verification status
                        }
                        isVerified = user.isEmailVerified

                        if (isVerified) {
                            val success = firestoreRepository.addVerifiedUser(user.uid, email)
                            if (success) {
                                withContext(Dispatchers.Main) {
                                    navigationCallback?.navigateToDoctorHome() // Trigger navigation
                                }
                            } else {
                                // Handle error adding user to Firestore
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error adding user to database", Toast.LENGTH_SHORT).show()
                                }
                            }
                            break // Exit the loop if verified or error adding to Firestore
                        } else {
                            delay(30000) // Wait for 30 seconds before checking again
                            attempts++
                        }
                    } catch (e: Exception) {
                        Log.e("DoctorRegistrationViewModel", "Error checking verification: ${e.message}", e)
                        // Handle error, e.g., show a message to the user
                        break // Exit the loop if there's an error
                    }
                }

                if (!isVerified && attempts >= maxAttempts) {
                    // Verification timed out
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Verification timed out. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return registrationSuccess
    }
}

@Composable
fun DoctorRegistrationNavigation(activity: Activity) {
    val navController = rememberNavController()
    val auth: FirebaseAuth = Firebase.auth
    val db = Firebase.firestore
    var userRole by remember { mutableStateOf<String?>(null) } // Store user role
    var isNavigationReady by remember { mutableStateOf(false) } // Add a state variable

    // Fetch user role when currentUser changes
    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser
        if (user != null) {
            val userRef = db.collection("users").document(user.uid)
            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userRole = document.getString("role")
                }
            }.addOnFailureListener { exception ->
                Log.e("DoctorRegistrationNavigation", "Error getting user data: ${exception.message}", exception)
            }
        }
    }

    // Conditional navigation based on userRole
    LaunchedEffect(userRole, isNavigationReady) {
        if (isNavigationReady) { // Only navigate if navigation is ready
            when (userRole) {
                "doctor" -> navController.navigate("doctorHome")
                "patient" -> navController.navigate("patientHome")
                else -> {
                    // Handle case where role is null or unknown
                    // This is where your registration screens would be
                }
            }
        }
    }

    // Set isNavigationReady to true after the initial composition
    LaunchedEffect(Unit) {
        isNavigationReady = true
    }

    // Your NavHost setup
    NavHost(navController = navController, startDestination = "doctorRegistration") {
        composable("doctorRegistration") { DoctorRegistrationByEmailScreen(navController) }
        composable(
            route = "doctorOtpVerification/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            DoctorOtpVerificationScreen(phoneNumber, navController)
        }
        composable("doctorHome") { DoctorHomeScreen() }
        composable("patientHome") { PatientHomeScreen() } // Replace with your actual DoctorHomeScreen
        composable("doctorLogin") {  DoctorLoginNavigation() }
    }
}

@Preview(showBackground = true)
@Composable
fun DoctorRegistrationByEmailScreenPreview() {
    HealthLinkerTheme {
        val context = LocalContext.current
        val activity = (context as? Activity) ?: error("Context is not an Activity")
        DoctorRegistrationNavigation(activity)
    }
}

