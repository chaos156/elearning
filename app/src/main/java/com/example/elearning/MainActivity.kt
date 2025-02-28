package com.example.elearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ui.LoginScreen
import ui.Roles.Student.StudentDashboard
import ui.Roles.Tutor.TutorDashboard
import ui.SignupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "signup") {
                composable("signup") { SignupScreen(navController, auth, db) }
                composable("login") { LoginScreen(navController, auth, db) }
                composable("tutorWelcome") { TutorDashboard() }
                composable("studentWelcome") { StudentDashboard() }
            }
        }
    }
}
