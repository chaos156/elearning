package com.example.elearning

//import ui.roles.student.ViewStudentProfileScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ui.LoginScreen
import ui.SignupScreen
import ui.roles.student.CourseDetails
import ui.roles.student.LessonContent
import ui.roles.student.MyLessons
import ui.roles.student.StudentBookingScreen
import ui.roles.student.StudentDashboard
import ui.roles.student.ViewLesson
import ui.roles.student.ViewStudentProfile
import ui.roles.tutor.CreateCalendar
import ui.roles.tutor.CreateLesson
import ui.roles.tutor.TutorDashboard
import ui.roles.tutor.ViewCourses
import ui.roles.tutor.ViewRequests
import ui.roles.tutor.ViewStudentProgress
import ui.roles.tutor.ViewTutorProfile

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
                composable("tutorWelcome") { TutorDashboard(navController) }
                composable("studentWelcome") { StudentDashboard(navController) }
                composable("viewProfile") { ViewStudentProfile(navController) }
                composable("viewProfile") { ViewTutorProfile(navController) }
                composable("viewCourses") { ViewCourses(navController) }
                composable("createLessons") { CreateLesson(navController) }
                composable("viewLessons") { ViewLesson(navController) }
                composable("viewRequests") { ViewRequests(navController) }
                composable("viewStudentProgress") { ViewStudentProgress(navController) }
                composable("myLessons") { MyLessons(navController) }
                composable("myLessons") { MyLessons(navController) }
                composable("createCalender") { CreateCalendar(navController) }
                composable("viewCalendar") { StudentBookingScreen (navController) }
                composable("courseDetails/{courseId}") { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                    CourseDetails(navController, courseId)
                }
                composable("lessonContent/{lessonId}") { backStackEntry ->
                    val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                    LessonContent(navController, lessonId)
                }
            }
        }
    }
}
