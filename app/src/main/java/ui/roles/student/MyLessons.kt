package ui.roles.student

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class for Course with additional fields
data class StudentCourse(
    val id: String = "",
    val courseName: String = "",
    val courseDescription: String = "",
    val subject: String = "",
    val completedLessons: Int = 0,  // Number of completed lessons
    val totalLessons: Int = 0      // Total number of lessons
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MyLessons(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var courses by remember { mutableStateOf<List<StudentCourse>>(emptyList()) }

    // Fetch courses the student is enrolled in
    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val enrolledCourses = db.collection("enrollments")
                    .whereEqualTo("studentId", userId)
                    .get().await()

                val courseIds = enrolledCourses.documents.mapNotNull { it.getString("courseId") }

                // Remove duplicates based on courseId
                val uniqueCourseIds = courseIds.distinct()

                val courseList = uniqueCourseIds.mapNotNull { courseId ->
                    val courseDoc = db.collection("courses").document(courseId).get().await()
                    val data = courseDoc.data
                    if (data != null) {
                        // Fetch course details including courseName, courseDescription, and subject
                        val courseName = data["courseName"] as? String ?: "Untitled Course"
                        val courseDescription = data["courseDescription"] as? String ?: "No description"
                        val subject = data["subject"] as? String ?: "No subject"

                        // Fetch lessons for the course
                        val lessonsQuery = db.collection("lessons")
                            .whereEqualTo("courseId", courseId)
                            .get().await()

                        // Get the total number of lessons
                        val totalLessons = lessonsQuery.size()

                        // Count the completed lessons
                        val completedLessons = lessonsQuery.documents.count { lessonDoc ->
                            val lessonId = lessonDoc.id
                            val isCompleted = isLessonsCompleted(lessonId, userId)
                            isCompleted
                        }

                        StudentCourse(
                            id = courseId,
                            courseName = courseName,
                            courseDescription = courseDescription,
                            subject = subject,
                            completedLessons = completedLessons,
                            totalLessons = totalLessons
                        )
                    } else {
                        null
                    }
                }
                courses = courseList
            } catch (e: Exception) {
                println("Error fetching courses: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Courses") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (courses.isEmpty()) {
                Text("No courses enrolled yet.", style = MaterialTheme.typography.h6)
            } else {
                LazyColumn {
                    items(courses) { course ->
                        CourseItem(course = course, onClick = {
                            navController.navigate("courseDetails/${course.id}")
                        })
                    }
                }
            }
        }
    }
}

// Function to check if a lesson is completed by the current user
suspend fun isLessonsCompleted(lessonId: String, userId: String?): Boolean {
    val db = FirebaseFirestore.getInstance()

    // Check Firestore for the lesson submission by this user
    val submissionsQuery = db.collection("lesson_submissions")
        .whereEqualTo("lessonId", lessonId)
        .whereEqualTo("userId", userId)
        .get().await()

    return submissionsQuery.documents.isNotEmpty()
}

// UI for each course item
@Composable
fun CourseItem(course: StudentCourse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = course.courseName, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = course.courseDescription, style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Subject: ${course.subject}", style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.height(8.dp))
            // Display completed lessons in the format "completedLessons/totalLessons"
            Text(
                text = "${course.completedLessons}/${course.totalLessons} lessons completed",
                style = MaterialTheme.typography.body2
            )
        }
    }
}
