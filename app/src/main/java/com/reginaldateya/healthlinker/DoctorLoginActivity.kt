package com.reginaldateya.healthlinker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
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
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.reginaldateya.healthlinker.ui.theme.HealthLinkerTheme
import java.util.concurrent.TimeUnit

class DoctorLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthLinkerTheme {
                DoctorLoginNavigation()
            }
        }
    }
}

@Composable
fun DoctorLoginPhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    onCountryCodeChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: DoctorLoginViewModel
) {
    val countryCodes = listOf("+254", "+1", "+44", "+91")
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = selectedCountryCode,
            onValueChange = {},
            label = { Text("Code") },
            readOnly = true,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
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
                        onExpandedChange(false)
                    },
                    text = { Text(code) }
                )
            }
        }

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(2f)
        )
    }

    Button(
        onClick = {
            if (phoneNumber.isNotBlank()) {
                viewModel.startPhoneNumberVerification(selectedCountryCode + phoneNumber, context)
            } else {
                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Continue with phone number")
    }
}



@Composable
fun DoctorLoginScreen(
    navController: NavController, viewModel: DoctorLoginViewModel = viewModel()
) {
    var isPhoneNumberDoctorLogin by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope() // Create a coroutine scope
    val context = LocalContext.current
    var selectedCountryCode by remember { mutableStateOf("+254") } // Default to Kenya
    var inputtedPhoneNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Show error messages within the UI
    if (showError && errorMessage.isNotBlank()) {
        Text(
            text = errorMessage,
            color = Color.Red,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // Loading state handling (optional)
    var isLoading by remember { mutableStateOf(false) }

    // Check verification status when the screen is composed
    LaunchedEffect(viewModel) {
        viewModel.navigationCallback = object : NavigationCallbackDoctorLogin {
            override fun navigateToDoctorHome() {
                navController.navigate("doctorHome") {
                    popUpTo("doctorLogin") { inclusive = true }
                }
            }

            override fun navigateToDoctorOtpVerificationScreen(
                verificationId: String,
                phoneNumber: String
            ) {
                navController.navigate("doctorOtpVerification/$verificationId/$phoneNumber")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp), // Set padding for the entire layout
        verticalArrangement = Arrangement.spacedBy(16.dp), // Provide consistent spacing
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(
            text = "Login into a Doctor account",
            color = Color.Blue,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 40.dp)
        )

        Button(
            onClick = { isPhoneNumberDoctorLogin = !isPhoneNumberDoctorLogin },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(
                text = if (isPhoneNumberDoctorLogin) "Continue with Email" else "Continue with Phone Number",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        if (isPhoneNumberDoctorLogin) {
            // Call PhoneNumberInput here, passing necessary parameters
            var phoneNumber by remember { mutableStateOf("") } // State for phone number
            DoctorLoginPhoneNumberInput(
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

            if (showError && errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (email.text.isBlank() || password.text.isBlank()) {
                        Toast.makeText(
                            context,
                            "Please fill in all fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text)
                            .matches()
                    ) {
                        Toast.makeText(
                            context,
                            "Please enter a valid email",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Call the login function in the ViewModel
                        viewModel.loginWithEmail(
                            email.text.trim(),
                            password.text.trim(),
                            context
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006CFF))
            ) {
                Text(
                    text = "Login",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }


            Text(
                text = "Forgot password?",
                color = Color(0xFF5579C6),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

        }




        Spacer(modifier = Modifier.height(16.dp)) // Add space between login and register card


        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append("Not an existing user? ")
            }
            pushStringAnnotation(tag = "URL", annotation = "doctorRegistration")
            withStyle(
                style = SpanStyle(
                    color = Color.Green,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Sign up here")
            }
            pop()
        }

        ClickableText( // Now ClickableText can access annotatedString
            text = annotatedString,
            onClick = { offset ->
                annotatedString
                    .getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        navController.navigate("doctorRegistration")
                    }
            }
        )
    }
}






    interface NavigationCallbackDoctorLogin {
        fun navigateToDoctorHome()
        fun navigateToDoctorOtpVerificationScreen(verificationId: String, phoneNumber: String)
    }


    class DoctorLoginViewModel : ViewModel() {
        private val auth: FirebaseAuth = FirebaseAuth.getInstance()
        var navigationCallback: NavigationCallbackDoctorLogin? = null
        private var verificationId: String? = null // Store verification ID

        // Email/Password Login

        private fun handleLoginSuccess(context: Context) {
            checkUserRole(context)
        }

        private fun checkUserRole(context: Context) {
            val userId = auth.currentUser?.uid ?: return
            val dbRef = FirebaseFirestore.getInstance().collection("users").document(userId)

            dbRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "doctor") {
                        // Navigate to the patient home if the role is patient
                        navigationCallback?.navigateToDoctorHome()
                    } else {
                        // Show an error if the role is not patient
                        Toast.makeText(context, "You do not have access as a doctor", Toast.LENGTH_SHORT).show()
                        FirebaseAuth.getInstance().signOut()
                    }
                } else {
                    Toast.makeText(context, "User document does not exist", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to verify user role", Toast.LENGTH_SHORT).show()
            }
        }


        fun loginWithEmail(email: String, password: String, context: Context) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Navigate to home screen or show success message
                        handleLoginSuccess(context)
                    } else {
                        // Handle failure, show error message
                        Toast.makeText(
                            context,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }


        // Phone Authentication: Step 1 - Send OTP
        fun startPhoneNumberVerification(phoneNumber: String, context: Context) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(context as ComponentActivity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Automatic sign-in success
                        signInWithPhoneAuthCredential(credential, context)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        // Verification failed
                        Toast.makeText(
                            context,
                            "Verification failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        // Store verificationId and navigate to OTP entry screen
                        this@DoctorLoginViewModel.verificationId = verificationId
                        navigationCallback?.navigateToDoctorOtpVerificationScreen(
                            verificationId,
                            phoneNumber
                        )
                    }
                })
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        // Phone Authentication: Step 2 - Verify OTP
        fun verifyOtp(verificationId: String, otp: String, context: Context) {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            signInWithPhoneAuthCredential(credential, context)
        }

        private fun signInWithPhoneAuthCredential(
            credential: PhoneAuthCredential,
            context: Context
        ) {
            auth.signInWithCredential(credential)
                .addOnCompleteListener(context as ComponentActivity) { task ->
                    if (task.isSuccessful) {
                        navigationCallback?.navigateToDoctorHome()
                    } else {
                        Toast.makeText(
                            context,
                            "Invalid OTP. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }


    @Composable
    fun DoctorLoginNavigation() {
        val navController = rememberNavController()
        val context = LocalContext.current
        val activity = context as Activity // Get the Activity
        NavHost(navController = navController, startDestination = "doctorLogin") {
            composable("doctorLogin") { DoctorLoginScreen(navController) }
            composable(
                route = "doctorOtpVerification/{verificationId}/{phoneNumber}",
                arguments = listOf(
                    navArgument("verificationId") { type = NavType.StringType },
                    navArgument("phoneNumber") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
                val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                DoctorOtpVerificationScreen(phoneNumber, navController)
            }
            composable("doctorHome") {
                DoctorHomeScreen() // Replace with your DoctorHome composable screen
            }
            composable("doctorRegistration") { DoctorRegistrationNavigation(activity) }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DoctorLoginScreenPreview() {
        HealthLinkerTheme {
            DoctorLoginNavigation()
        }
    }
