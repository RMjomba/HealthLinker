package com.reginaldateya.healthlinker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reginaldateya.healthlinker.ui.theme.HealthLinkerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable fullscreen immersive mode by removing system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthLinkerTheme {
                HealthLinkerApp()
            }
        }
    }
}


@Composable
fun MainScreen(navController: NavController) {

    var showMenu by remember { mutableStateOf(false) }

    // Set the background to dark blue
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // White color
    ) {
        // Scrollable content
        val scrollState = rememberLazyListState()

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Welcome note above the buttons
                Text(
                    text = "Welcome to HealthLinker!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }
            item {
                // Button for the doctor
                CustomButton(
                    text = "Login as a Doctor",
                    onClick = { navController.navigate("doctorLogin") }
                )
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
            item {
                // Button for the patient
                CustomButton(
                    text = "Login as a Patient",
                    onClick = { navController.navigate("patientLogin")}
                )
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
            item {
                // Link for new users below the buttons
                // AnnotatedString allows applying different styles to parts of the text
                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.Black)) { // Default color for most of the text
                        append("New user? ")
                    }
                    withStyle(style = SpanStyle(color = Color.Green, textDecoration = TextDecoration.Underline)) { // Yellow color for "Sign up here"
                        append("Sign up here")
                    }
                }

                Box { // Wrap Text in a Box for dropdown placement
                    Text(
                        text = annotatedString,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { showMenu = true } // Show menu on click
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White) // Set background color
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sign up as a Doctor") },
                            onClick = {
                                navController.navigate("doctorSignUp") {
                                    popUpTo("main") { inclusive = true } // Optional: Clear back stack
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign up as a Patient") },
                            onClick = {
                                navController.navigate("patientSignUp") // Navigate to patient sign-up screen
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF006CFF), // Blue color for button
            contentColor = Color.White // White text on the button
        ),
        modifier = Modifier
            .fillMaxWidth(0.6f) // Set button width to 60% of the screen width
            .height(50.dp), // Button height
        shape = RoundedCornerShape(10.dp) // Rounded corners for the button
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun HealthLinkerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as Activity // Get the Activity
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("doctorLogin") { DoctorLoginNavigation() } // Route for doctor login
        composable("patientLogin") { PatientLoginNavigation() } // Route for patient login
        composable("doctorSignUp") { DoctorRegistrationNavigation (activity) } // Route for doctor sign-up
        composable("patientSignUp") { PatientRegistrationNavigation(activity) } // Route for patient sign-up
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HealthLinkerTheme {
        MainScreen(navController = rememberNavController()) // Use rememberNavController()
    }
}
