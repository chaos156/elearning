package ui.roles.tutor

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
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class for Course (only one declaration)
data class Course(
    val id: String = "",
    val courseName: String = "",
    val courseDescription: String = "",
    val subject: String = "",
    var enrollmentStatus: String = "none" // "none", "pending", "approved"
)

data class Lesson(val id: String = "", val title: String = "")

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ViewStudentProgress(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var courses by remember { mutableStateOf(listOf<Course>()) }

    // Fetch all courses for the tutor
    LaunchedEffect(Unit) {
        val tutorId = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
            val courseDocs = db.collection("courses")
                .whereEqualTo("tutorId", tutorId)
                .get().await()

            courses = courseDocs.documents.map { doc ->
                Course(
                    id = doc.id,
                    courseName = doc.getString("courseName") ?: "No Name",
                    courseDescription = doc.getString("courseDescription") ?: "No Description",
                    subject = doc.getString("subject") ?: "No Subject"
                )
            }
        } catch (e: Exception) {
//            Toast.makeText(context, "Error fetching courses: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Courses") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(courses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { navController.navigate("courseStats/${course.id}") },
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = course.courseName, style = MaterialTheme.typography.h6)
                        Text(text = course.subject, style = MaterialTheme.typography.body2)
                        Text(text = course.courseDescription, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CourseStats(navController: NavController, courseId: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var lessons by remember { mutableStateOf(listOf<Lesson>()) }
    var enrolledStudents by remember { mutableStateOf(0) }
    var courseName by remember { mutableStateOf("") }

    // Fetch course stats and lessons
    LaunchedEffect(courseId) {
        try {
            val courseDoc = db.collection("courses").document(courseId).get().await()
            courseName = courseDoc.getString("courseName") ?: "Unknown Course"

            // Get the lessons in the course
            val lessonDocs = db.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get().await()

            lessons = lessonDocs.documents.map { doc ->
                Lesson(id = doc.id, title = doc.getString("title") ?: "Untitled")
            }

            // Get the number of enrolled students
            val enrollmentDocs = db.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", "approved")  // Only count approved students
                .get().await()

            enrolledStudents = enrollmentDocs.size()
        } catch (e: Exception) {
//            Toast.makeText(context, "Error fetching course stats: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Stats for $courseName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Display stats
            Text("Number of Lessons: ${lessons.size}", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Number of Students Enrolled: $enrolledStudents", style = MaterialTheme.typography.h6)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Lessons:", style = MaterialTheme.typography.h6)
            LazyColumn {
                items(lessons) { lesson ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Text(text = lesson.title, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Placeholder Button (for future use)
            Button(onClick = { /* Handle any actions for course management */ }) {
                Text("Manage Course")
            }
        }
    }
}
