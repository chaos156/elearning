package ui.roles.tutor

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class for Course
data class TutorCourse(
    val id: String = "",
    val courseName: String = "",
    val courseDescription: String = "",
    val subject: String = ""
)

data class CourseLesson(val id: String = "", val title: String = "")

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ViewStudentProgress(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var courses by remember { mutableStateOf(listOf<TutorCourse>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch data
    LaunchedEffect(Unit) {
        val tutorId = auth.currentUser?.uid
        if (tutorId == null) {
            errorMessage = "Unable to get user information. Please login again."
            isLoading = false
            return@LaunchedEffect
        }

        println("Getting courses for tutor $tutorId")

        val courseDocs = db.collection("courses")
            .whereEqualTo("tutorId", tutorId)
            .get().await()

        println("Found ${courseDocs.size()} courses")

        courses = courseDocs.documents.mapNotNull { doc ->
            TutorCourse(
                id = doc.id,
                courseName = doc.getString("courseName") ?: "No Name",
                courseDescription = doc.getString("courseDescription") ?: "No Description",
                subject = doc.getString("subject") ?: "No Subject"
            )
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Courses") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            navController.navigateUp()
                        }) {
                            Text("Go Back")
                        }
                    }
                }
            }
            courses.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("You haven't created any courses yet.")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable {
                                    if (course.id.isNotEmpty()) {
                                        println("Navigating to courseStats/${course.id}")
                                        navController.navigate("courseStats/${course.id}")
                                    } else {
                                        Toast.makeText(context, "Invalid course ID", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = course.courseName,
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Subject: ${course.subject}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = course.courseDescription,
                                    style = MaterialTheme.typography.body2
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Click to view student progress",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressChartWrapper(courseId: String, onShowError: () -> Unit) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(courseId) {
        if (courseId.isBlank()) {
            hasError = true
            errorMessage = "Invalid course ID"
            onShowError()
        }
    }

    if (hasError) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Unable to load student progress chart",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Error: $errorMessage",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onShowError) {
                    Text("Hide Chart")
                }
            }
        }
    } else {
        CourseProgressChart(courseId)
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CourseStats(navController: NavController, courseId: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var lessons by remember { mutableStateOf(listOf<CourseLesson>()) }
    var enrolledStudents by remember { mutableStateOf(0) }
    var courseName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProgressChart by remember { mutableStateOf(true) }

    // Validate courseId
    LaunchedEffect(Unit) {
        if (courseId.isBlank()) {
            errorMessage = "Invalid course ID"
            isLoading = false
            return@LaunchedEffect
        }

        println("Getting stats for course $courseId")

        // Get course info
        val courseDoc = db.collection("courses").document(courseId).get().await()
        if (!courseDoc.exists()) {
            errorMessage = "Course not found"
            isLoading = false
            return@LaunchedEffect
        }

        courseName = courseDoc.getString("courseName") ?: "Unknown Course"
        println("Course name: $courseName")

        // Get course lessons
        val lessonDocs = db.collection("lessons")
            .whereEqualTo("courseId", courseId)
            .get().await()

        println("Found ${lessonDocs.size()} lessons")

        lessons = lessonDocs.documents.mapNotNull { doc ->
            CourseLesson(
                id = doc.id,
                title = doc.getString("title") ?: "Untitled"
            )
        }

        // Get enrolled students count
        val enrollmentDocs = db.collection("enrollments")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("status", "approved")
            .get().await()

        enrolledStudents = enrollmentDocs.size()
        println("Enrolled students: $enrolledStudents")

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Statistics") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            navController.popBackStack()
                        }) {
                            Text("Go Back")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Course header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = courseName,
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                StatItemCard(
                                    value = lessons.size.toString(),
                                    label = "Lessons",
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )

                                StatItemCard(
                                    value = enrolledStudents.toString(),
                                    label = "Students",
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Student progress chart with wrapper
                    if (enrolledStudents > 0 && showProgressChart) {
                        ProgressChartWrapper(
                            courseId = courseId,
                            onShowError = {
                                showProgressChart = false
                            }
                        )
                    } else if (enrolledStudents == 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No students enrolled in this course yet.",
                                    style = MaterialTheme.typography.body1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Lessons list
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Course Lessons",
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (lessons.isEmpty()) {
                                Text(
                                    text = "No lessons available for this course.",
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                lessons.forEachIndexed { index, lesson ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = lesson.title,
                                                style = MaterialTheme.typography.body1,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Course management button
                    Button(
                        onClick = { /* Handle course management */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage Course")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItemCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.h4,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )
    }
}