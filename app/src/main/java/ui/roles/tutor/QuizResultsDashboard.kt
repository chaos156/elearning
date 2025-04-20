package ui.roles.tutor

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ui.quiz.QuizSubmission
import ui.quiz.SubmissionStatus
import ui.quiz.StudentQuizResult
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun QuizResultsDashboard(navController: NavController, quizId: String) {
    val db = FirebaseFirestore.getInstance()
    var quizTitle by remember { mutableStateOf("") }
    var studentResults by remember { mutableStateOf<List<StudentQuizResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalStudents by remember { mutableStateOf(0) }
    var submittedCount by remember { mutableStateOf(0) }
    var averageScore by remember { mutableStateOf(0.0) }
    var highestScore by remember { mutableStateOf(0) }
    var lowestScore by remember { mutableStateOf(0) }

    // 加载测验结果数据
    LaunchedEffect(quizId) {
        try {
            // 获取测验信息
            val quizDoc = db.collection("quizzes").document(quizId).get().await()
            quizTitle = quizDoc.getString("title") ?: "Quiz Results"

            // 获取所有提交
            val submissions = db.collection("quiz_submissions")
                .whereEqualTo("quizId", quizId)
                .whereEqualTo("status", SubmissionStatus.COMPLETED.name)
                .get()
                .await()

            // 解析提交数据
            val results = mutableListOf<StudentQuizResult>()
            var scoreSum = 0
            var maxScore = 0
            var minScore = Int.MAX_VALUE
            var studentsWithSubmission = 0

            for (doc in submissions.documents) {
                val submission = doc.toObject(QuizSubmission::class.java)
                if (submission != null) {
                    studentsWithSubmission++

                    // 获取学生姓名
                    val userDoc = db.collection("users").document(submission.studentId).get().await()
                    val studentName = userDoc.getString("name") ?: "Unknown Student"

                    val result = StudentQuizResult(
                        studentId = submission.studentId,
                        studentName = studentName,
                        score = submission.score,
                        totalPoints = submission.totalPoints,
                        completedAt = submission.completedAt
                    )

                    results.add(result)

                    // 更新统计数据
                    scoreSum += submission.score
                    maxScore = maxOf(maxScore, submission.score)
                    if (submission.score > 0) {
                        minScore = minOf(minScore, submission.score)
                    }
                }
            }

            // 按分数排序（高到低）
            results.sortByDescending { it.score }

            // 更新状态
            studentResults = results
            submittedCount = studentsWithSubmission

            if (submittedCount > 0) {
                averageScore = scoreSum.toDouble() / submittedCount
                highestScore = maxScore
                lowestScore = if (minScore == Int.MAX_VALUE) 0 else minScore
            }

            // 获取总学生数（不论是否提交）
            val courseId = quizDoc.getString("courseId") ?: ""
            if (courseId.isNotEmpty()) {
                val enrollments = db.collection("enrollments")
                    .whereEqualTo("courseId", courseId)
                    .whereEqualTo("status", "approved")
                    .get()
                    .await()

                totalStudents = enrollments.size()
            }

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading quiz results: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Results") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn {
                        item {
                            // 测验标题
                            Text(
                                text = quizTitle,
                                style = MaterialTheme.typography.h5,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // 统计卡片
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                elevation = 4.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Statistics",
                                        style = MaterialTheme.typography.h6,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 左侧统计
                                        Column {
                                            StatItem(label = "Total Students", value = totalStudents.toString())
                                            StatItem(label = "Submissions", value = "$submittedCount/$totalStudents")
                                            StatItem(
                                                label = "Completion Rate",
                                                value = if (totalStudents > 0)
                                                    "${(submittedCount * 100 / totalStudents)}%"
                                                else "0%"
                                            )
                                        }

                                        // 右侧统计
                                        Column {
                                            StatItem(
                                                label = "Average Score",
                                                value = String.format("%.1f", averageScore)
                                            )
                                            StatItem(label = "Highest Score", value = highestScore.toString())
                                            StatItem(label = "Lowest Score", value = lowestScore.toString())
                                        }
                                    }
                                }
                            }

                            // 学生结果列表标题
                            if (studentResults.isNotEmpty()) {
                                Text(
                                    text = "Student Results",
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    elevation = 2.dp,
                                    backgroundColor = Color.LightGray.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "No students have completed this quiz yet.",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        }

                        // 学生结果列表
                        items(studentResults) { result ->
                            StudentResultCard(result = result)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.body2
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StudentResultCard(result: StudentQuizResult) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val submissionDate = if (result.completedAt > 0) {
        dateFormatter.format(Date(result.completedAt))
    } else {
        "Unknown"
    }

    val scorePercentage = if (result.totalPoints > 0) {
        (result.score * 100) / result.totalPoints
    } else {
        0
    }

    // 根据分数设置颜色
    val scoreColor = when {
        scorePercentage >= 80 -> Color(0xFF4CAF50) // 绿色
        scorePercentage >= 60 -> Color(0xFFFFC107) // 黄色
        else -> Color(0xFFF44336) // 红色
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 学生姓名
                Text(
                    text = result.studentName,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )

                // 分数
                Text(
                    text = "${result.score}/${result.totalPoints} ($scorePercentage%)",
                    style = MaterialTheme.typography.subtitle1,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 完成时间
            Text(
                text = "Completed: $submissionDate",
                style = MaterialTheme.typography.caption
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 进度条
            LinearProgressIndicator(
                progress = if (result.totalPoints > 0) {
                    result.score.toFloat() / result.totalPoints
                } else {
                    0f
                },
                modifier = Modifier.fillMaxWidth(),
                color = scoreColor
            )
        }
    }
}