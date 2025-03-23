package ui.roles.tutor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class StudentProgress(
    val studentId: String,
    val studentName: String,
    val completedLessons: Int,
    val totalLessons: Int
)

@Composable
fun CourseProgressChart(courseId: String) {
    val db = FirebaseFirestore.getInstance()
    var studentProgress by remember { mutableStateOf<List<StudentProgress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch data
    LaunchedEffect(courseId) {
        val startTime = System.currentTimeMillis()
        println("📊 Starting to load student progress data for course $courseId")

        // 1. Get lesson IDs
        val lessonQuery = db.collection("lessons")
            .whereEqualTo("courseId", courseId)

        val lessonDocs = withContext(Dispatchers.IO) {
            lessonQuery.get().await()
        }

        val totalLessons = lessonDocs.size()
        val lessonIds = lessonDocs.documents.map { it.id }

        println("📊 Found $totalLessons lessons")

        if (lessonIds.isEmpty()) {
            println("📊 No lessons found, returning early")
            studentProgress = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        // 2. Get enrolled students
        val enrollmentQuery = db.collection("enrollments")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("status", "approved")

        val enrolledStudents = withContext(Dispatchers.IO) {
            enrollmentQuery.get().await()
        }

        println("📊 Found ${enrolledStudents.size()} enrolled students")

        if (enrolledStudents.documents.isEmpty()) {
            println("📊 No enrolled students found, returning early")
            studentProgress = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        // 3. Batch get student info and completion records
        withContext(Dispatchers.Default) {
            coroutineScope {
                val progressData = enrolledStudents.documents.mapNotNull { enrollment ->
                    val studentId = enrollment.getString("studentId")
                    if (studentId == null) {
                        println("⚠️ Skipping enrollment record without studentId")
                        return@mapNotNull null
                    }

                    async {
                        // Get student name
                        val studentDoc = db.collection("users").document(studentId).get().await()
                        val studentName = studentDoc.getString("name") ?: "Unknown Student"

                        println("📊 Processing student: $studentName (ID: $studentId)")

                        // Get all lesson submissions for this student
                        val submissions = db.collection("lesson_submissions")
                            .whereEqualTo("userId", studentId)
                            .get().await()

                        // Calculate completed lessons
                        val submittedLessonIds = submissions.documents.mapNotNull {
                            it.getString("lessonId")
                        }

                        val completedLessons = lessonIds.count { it in submittedLessonIds }

                        println("📊 Student $studentName: completed $completedLessons/$totalLessons lessons")

                        StudentProgress(
                            studentId = studentId,
                            studentName = studentName,
                            completedLessons = completedLessons,
                            totalLessons = totalLessons
                        )
                    }
                }.awaitAll()

                // Update state
                studentProgress = progressData

                val endTime = System.currentTimeMillis()
                println("✅ Student progress data loaded, took ${endTime - startTime} ms")
            }
        }

        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Student Progress",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading student progress data...")
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                studentProgress.isEmpty() -> {
                    Text(
                        text = "No students enrolled in this course yet.",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // Simple bar chart
                    SimpleBarChart(studentProgress)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Student progress list
                    Text(
                        text = "Student Progress Details",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    studentProgress.forEach { progress ->
                        StudentProgressItem(progress)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(data: List<StudentProgress>) {
    // Check if data is empty
    if (data.isEmpty()) {
        Text(
            text = "No data available to display chart",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp)
        )
        return
    }

    val maxHeight = 200.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight + 64.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw horizontal grid lines
            val strokeWidth = 1f
            val divisions = 5

            for (i in 0..divisions) {
                val y = canvasHeight - (canvasHeight * i / divisions)

                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = strokeWidth
                )
            }

            // Draw bars
            val barCount = data.size
            val barWidth = if (barCount > 0) canvasWidth / (barCount * 2) else 0f

            data.forEachIndexed { index, progress ->
                val percentage = if (progress.totalLessons > 0) {
                    progress.completedLessons.toFloat() / progress.totalLessons
                } else {
                    0f
                }

                val barHeight = canvasHeight * percentage
                val barColor = when {
                    percentage >= 0.8f -> Color(0xFF4CAF50) // Green
                    percentage >= 0.5f -> Color(0xFFFFC107) // Yellow
                    else -> Color(0xFFF44336) // Red
                }

                val left = index * (canvasWidth / barCount) + (canvasWidth / barCount - barWidth) / 2
                val top = canvasHeight - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight)
                )
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { progress ->
                Text(
                    text = progress.studentName.take(8) + if (progress.studentName.length > 8) "..." else "",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Y-axis label
        Text(
            text = "Completion %",
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun StudentProgressItem(progress: StudentProgress) {
    val percentage = if (progress.totalLessons > 0) {
        progress.completedLessons.toFloat() / progress.totalLessons
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = progress.studentName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )

                Text(
                    text = "${(percentage * 100).toInt()}% complete",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = percentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    percentage >= 0.8f -> Color(0xFF4CAF50) // Green for high progress
                    percentage >= 0.5f -> Color(0xFFFFC107) // Yellow for medium progress
                    else -> Color(0xFFF44336) // Red for low progress
                },
                backgroundColor = Color.LightGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${progress.completedLessons}/${progress.totalLessons} lessons completed",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}