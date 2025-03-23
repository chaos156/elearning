package com.example.elearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ui.LoginScreen
import ui.SignupScreen
import ui.roles.student.CourseDetails
import ui.roles.student.LessonContent
import ui.roles.student.MyLessons
import ui.roles.student.StudentBookingScreen
import ui.roles.student.StudentDashboard
import ui.roles.student.ViewLesson
import ui.roles.student.ViewStudentProfile
import ui.roles.tutor.CourseStats
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

                // 学生端路由
                composable("studentWelcome") { StudentDashboard(navController) }
                composable("myLessons") { MyLessons(navController) }
                composable("viewLessons") { ViewLesson(navController) }
                composable("viewCalendar") { StudentBookingScreen(navController) }

                // 定义特定的学生profile路由
                composable("studentProfile") { ViewStudentProfile(navController) }

                // 教师端路由
                composable("tutorWelcome") { TutorDashboard(navController) }
                composable("viewCourses") { ViewCourses(navController) }
                composable("createLessons") { CreateLesson(navController) }
                composable("viewRequests") { ViewRequests(navController) }
                composable("viewStudentProgress") { ViewStudentProgress(navController) }
                composable("createCalender") { CreateCalendar(navController) }

                // 定义特定的教师profile路由
                composable("tutorProfile") { ViewTutorProfile(navController) }

                // 通用的viewProfile路由，内联ProfileRouter功能
                composable("viewProfile") {
                    // 内联定义的Profile路由器Composable
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    LaunchedEffect(key1 = Unit) {
                        try {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // 使用协程等待查询完成
                                val document = db.collection("users").document(userId).get().await()
                                val role = document.getString("role")

                                // 根据角色导航到对应页面
                                if (role == "Tutor") {
                                    navController.navigate("tutorProfile") {
                                        popUpTo("viewProfile") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("studentProfile") {
                                        popUpTo("viewProfile") { inclusive = true }
                                    }
                                }
                            } else {
                                // 用户未登录，返回登录页
                                navController.navigate("login")
                            }
                        } catch (e: Exception) {
                            // 发生错误时默认导航到学生页面
                            navController.navigate("studentProfile") {
                                popUpTo("viewProfile") { inclusive = true }
                            }
                        }
                    }
                }

                composable(
                    route = "courseStats/{courseId}",
                    arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                    CourseStats(navController = navController, courseId = courseId)
                }

                composable(
                    route = "courseDetails/{courseId}",
                    arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                    CourseDetails(navController, courseId)
                }

                composable(
                    route = "lessonContent/{lessonId}",
                    arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                    LessonContent(navController, lessonId)
                }
            }
        }
    }
}