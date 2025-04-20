package ui.roles.student

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class for Lesson
data class CourseLesson(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false, // New field to track completion status
    val lessonOrder: Int = 0 // Add lessonOrder to determine the order of lessons
)

// 测验数据类
data class QuizInfo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val timeLimit: Int = 30,
    val isCompleted: Boolean = false
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CourseDetails(navController: NavController, courseId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Define the state using mutableStateOf
    val (lessons, setLessons) = remember { mutableStateOf<List<CourseLesson>>(emptyList()) }

    // 添加测验状态
    var quizzes by remember { mutableStateOf<List<QuizInfo>>(emptyList()) }

    // Fetch lessons for the selected course based on courseId (document ID)
    LaunchedEffect(courseId) {
        try {
            // Fetch all lessons, filtering by courseId to get lessons for the current course
            val lessonsQuery = db.collection("lessons")
                .whereEqualTo("courseId", courseId)  // Filter lessons by courseId
                .get().await()

            // Print out the raw data fetched for debugging purposes
            println("Fetched lessons: ${lessonsQuery.documents}")

            // Map the documents to CourseLesson objects
            val fetchedLessons = lessonsQuery.documents.mapNotNull { document ->
                val data = document.data
                if (data != null) {
                    val title = data["title"] as? String ?: "Untitled Lesson"
                    val description = data["description"] as? String ?: "No description"
                    val lessonOrder = (data["lessonOrder"] as? Long)?.toInt() ?: 0

                    // Check if this lesson is completed by the current user
                    val userId = auth.currentUser?.uid
                    val isCompleted = userId?.let { isLessonCompleted(document.id, it) } ?: false

                    // Create CourseLesson object with the lesson data
                    CourseLesson(
                        id = document.id,
                        title = title,
                        description = description,
                        isCompleted = isCompleted, // Set completion status
                        lessonOrder = lessonOrder
                    )
                } else {
                    null
                }
            }

            // Sort lessons by lessonOrder
            val sortedLessons = fetchedLessons.sortedBy { it.lessonOrder }

            // Update the state with the fetched and sorted lessons
            setLessons(sortedLessons)

        } catch (e: Exception) {
            println("Error fetching lessons: ${e.message}")
        }

        try {
            // 获取该课程的所有测验
            val quizzesQuery = db.collection("quizzes")
                .whereEqualTo("courseId", courseId)
                .get().await()

            // 获取当前用户的测验提交记录
            val userId = auth.currentUser?.uid

            val fetchedQuizzes = mutableListOf<QuizInfo>()

            for (quizDoc in quizzesQuery.documents) {
                val quizId = quizDoc.id
                val title = quizDoc.getString("title") ?: "Untitled Quiz"
                val description = quizDoc.getString("description") ?: ""
                val timeLimit = quizDoc.getLong("timeLimit")?.toInt() ?: 30

                // 检查用户是否已完成该测验
                var isCompleted = false
                if (userId != null) {
                    val submissionQuery = db.collection("quiz_submissions")
                        .whereEqualTo("quizId", quizId)
                        .whereEqualTo("studentId", userId)
                        .whereEqualTo("status", "COMPLETED")
                        .get().await()

                    isCompleted = !submissionQuery.isEmpty
                }

                fetchedQuizzes.add(
                    QuizInfo(
                        id = quizId,
                        title = title,
                        description = description,
                        timeLimit = timeLimit,
                        isCompleted = isCompleted
                    )
                )
            }

            quizzes = fetchedQuizzes

        } catch (e: Exception) {
            println("Error fetching quizzes: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lessons") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 课程部分
            item {
                Text(
                    text = "Lessons",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (lessons.isEmpty()) {
                    Text("No lessons available for this course.",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // 显示课程项目
            items(lessons) { lesson ->
                // Disable the next lesson if the previous one is not completed
                val previousLesson = lessons.find { it.lessonOrder == lesson.lessonOrder - 1 }
                val canAccessLesson = previousLesson?.isCompleted == true || lesson.lessonOrder == 1

                LessonItem(
                    lesson = lesson,
                    isEnabled = canAccessLesson, // Lock the lesson if previous one is not completed
                    onClick = {
                        if (canAccessLesson) {
                            navController.navigate("lessonContent/${lesson.id}")
                        }
                    }
                )
            }

            // 测验部分
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quizzes",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (quizzes.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Text(
                            text = "No quizzes available for this course.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // 显示测验项目
            items(quizzes) { quiz ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate("takeQuiz/${quiz.id}")
                        },
                    elevation = 4.dp,
                    backgroundColor = if (quiz.isCompleted)
                        Color.Green.copy(alpha = 0.1f) else MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quiz.title,
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.weight(1f)
                            )

                            if (quiz.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.Green
                                )
                            }
                        }

                        if (quiz.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quiz.description,
                                style = MaterialTheme.typography.body2
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Time Limit: ${quiz.timeLimit} minutes",
                            style = MaterialTheme.typography.caption
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (quiz.isCompleted) "Completed" else "Take Quiz",
                            color = if (quiz.isCompleted) Color.Green else MaterialTheme.colors.primary,
                            style = MaterialTheme.typography.button
                        )
                    }
                }
            }
        }
    }
}

// Function to check if a lesson is completed by the current user
suspend fun isLessonCompleted(lessonId: String, userId: String?): Boolean {
    val db = FirebaseFirestore.getInstance()

    // Check Firestore for the lesson submission by this user
    val submissionsQuery = db.collection("lesson_submissions")
        .whereEqualTo("lessonId", lessonId)
        .whereEqualTo("userId", userId)
        .get().await()

    return submissionsQuery.documents.isNotEmpty()
}

// UI for each lesson item
@Composable
fun LessonItem(lesson: CourseLesson, isEnabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                if (isEnabled) {
                    onClick() // Navigate to the lesson content
                }
            },
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Highlight completed lesson in green
            val lessonTextColor = if (lesson.isCompleted) Color.Green else MaterialTheme.typography.h6.color
            Text(text = lesson.title, style = MaterialTheme.typography.h6.copy(color = lessonTextColor))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = lesson.description, style = MaterialTheme.typography.body2)

            // Optionally display additional information
            if (lesson.isCompleted) {
                Text(text = "Completed", style = MaterialTheme.typography.body2.copy(color = Color.Green))
            } else if (!isEnabled) {
                Text(
                    text = "Complete previous lesson to access",
                    style = MaterialTheme.typography.body2.copy(color = Color.Red)
                )
            }
        }
    }
}