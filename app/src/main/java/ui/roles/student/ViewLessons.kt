package ui.roles.student

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data classes
data class Course(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val subject: String = "",
    var enrollmentStatus: String = "none", // "none", "pending", "approved"
    var enrollmentId: String? = null // Stores the enrollment document ID for canceling requests
)

data class Lesson(val id: String = "", val title: String = "")

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ViewLesson(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var courses by remember { mutableStateOf(listOf<Course>()) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var lessons by remember { mutableStateOf(listOf<Lesson>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    val subjects = listOf("All", "Computer Science", "Mathematics", "History", "Physics", "Biology", "Economics")

    // Fetch courses & enrollment status
    // Fetch courses & enrollment status
    LaunchedEffect(Unit) {
        val user = auth.currentUser ?: return@LaunchedEffect
        val enrolledCourses = db.collection("enrollments")
            .whereEqualTo("studentId", user.uid)
            .get().await()
            .documents.associate {
                val courseId = it.getString("courseId") ?: ""
                val status = it.getString("status") ?: "none"
                courseId to status // Using a pair instead of destructuring
            }

        val courseDocs = db.collection("courses").get().await()
        courses = courseDocs.documents.map { doc ->
            val courseId = doc.id
            val status = enrolledCourses[courseId] ?: "none"
            Course(
                id = courseId,
                name = doc.getString("courseName") ?: "",
                description = doc.getString("courseDescription") ?: "",
                subject = doc.getString("subject") ?: "",
                enrollmentStatus = status
            )
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedCourse == null) "Browse Courses" else selectedCourse!!.name) },
                navigationIcon = {
                    if (selectedCourse != null) {
                        IconButton(onClick = { selectedCourse = null }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (selectedCourse == null) {
                // 📌 Course List View
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subject Filter Dropdown
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(text = selectedSubject)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(onClick = {
                                selectedSubject = subject
                                expanded = false
                            }) {
                                Text(subject)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    val filteredCourses = courses.filter {
                        (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)) &&
                                (selectedSubject == "All" || it.subject == selectedSubject)
                    }

                    items(filteredCourses) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { selectedCourse = course },
                            elevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = course.name, style = MaterialTheme.typography.subtitle1)
                                Text(text = "Subject: ${course.subject}", style = MaterialTheme.typography.body2)
                                Text(text = course.description, style = MaterialTheme.typography.body2)
                                if (course.enrollmentStatus == "pending") {
                                    Text("Pending Approval", color = MaterialTheme.colors.primary)
                                } else if (course.enrollmentStatus == "approved") {
                                    Text("Already Enrolled", color = MaterialTheme.colors.primary)
                                }
                            }
                        }
                    }
                }
            } else {
                // 📌 Course Detail View
                val course = selectedCourse!!

                LaunchedEffect(course.id) {
                    val lessonDocs = db.collection("lessons")
                        .whereEqualTo("courseId", course.id)
                        .get().await()
                    lessons = lessonDocs.documents.map { doc ->
                        Lesson(id = doc.id, title = doc.getString("title") ?: "")
                    }
                }

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

                when (course.enrollmentStatus) {
                    "approved" -> {
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Already Enrolled")
                        }
                    }
                    "pending" -> {
                        Button(
                            onClick = {
                                course.enrollmentId?.let {
                                    db.collection("enrollments").document(it).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Enrollment Request Canceled", Toast.LENGTH_SHORT).show()
                                            courses = courses.map { c ->
                                                if (c.id == course.id) c.copy(enrollmentStatus = "none", enrollmentId = null) else c
                                            }
                                            selectedCourse = null
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Request")
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                val enrollmentData = hashMapOf(
                                    "studentId" to auth.currentUser!!.uid,
                                    "courseId" to course.id,
                                    "status" to "pending"
                                )

                                db.collection("enrollments").add(enrollmentData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Enrollment Request Sent", Toast.LENGTH_SHORT).show()
                                        courses = courses.map { c -> if (c.id == course.id) c.copy(enrollmentStatus = "pending") else c }
                                        selectedCourse = null
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enroll Now")
                        }
                    }
                }
            }
        }
    }
}
