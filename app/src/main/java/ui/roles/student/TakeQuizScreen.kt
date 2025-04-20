package ui.roles.student

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ui.quiz.Question
import ui.quiz.QuestionType
import ui.quiz.Quiz
import ui.quiz.QuizSubmission
import ui.quiz.SubmissionStatus
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TakeQuizScreen(navController: NavController, quizId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    // 状态变量
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf<MutableMap<String, List<String>>>(mutableMapOf()) }
    var remainingTime by remember { mutableStateOf<Long>(0) }
    var isQuizStarted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isQuizCompleted by remember { mutableStateOf(false) }
    var submissionId by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }
    var totalPoints by remember { mutableStateOf(0) }

    // 格式化剩余时间
    val formattedTime = remember(remainingTime) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) -
                TimeUnit.MINUTES.toSeconds(minutes)
        String.format("%02d:%02d", minutes, seconds)
    }

    // 加载测验数据
    LaunchedEffect(quizId) {
        try {
            val quizDoc = db.collection("quizzes").document(quizId).get().await()
            if (quizDoc.exists()) {
                val loadedQuiz = quizDoc.toObject(Quiz::class.java)
                quiz = loadedQuiz

                // 检查是否已有提交
                val submissionQuery = db.collection("quiz_submissions")
                    .whereEqualTo("quizId", quizId)
                    .whereEqualTo("studentId", userId)
                    .get()
                    .await()

                if (!submissionQuery.isEmpty) {
                    // 已经有提交，获取分数
                    val submission = submissionQuery.documents[0].toObject(QuizSubmission::class.java)
                    if (submission != null) {
                        if (submission.status == SubmissionStatus.COMPLETED) {
                            // 已完成的测验，显示分数
                            isQuizCompleted = true
                            score = submission.score
                            totalPoints = submission.totalPoints
                            submissionId = submission.id
                        } else {
                            // 未完成的测验，恢复答案
                            answers = submission.answers.toMutableMap()
                            submissionId = submission.id

                            // 计算剩余时间
                            val startTime = submission.startedAt
                            val elapsedTime = System.currentTimeMillis() - startTime
                            val quizDuration = loadedQuiz?.timeLimit?.times(60000L) ?: 1800000L
                            remainingTime = (quizDuration - elapsedTime).coerceAtLeast(0L)

                            if (remainingTime > 0) {
                                isQuizStarted = true
                            } else {
                                // 时间已到，但未提交
                                isQuizCompleted = true
                                errorMessage = "Quiz time expired!"
                            }
                        }
                    }
                } else {
                    // 新测验，设置时间
                    remainingTime = loadedQuiz?.timeLimit?.times(60000L) ?: 1800000L
                }
            } else {
                errorMessage = "Quiz not found!"
            }

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading quiz: ${e.message}"
            isLoading = false
        }
    }

    // 倒计时
    LaunchedEffect(isQuizStarted) {
        if (isQuizStarted && remainingTime > 0) {
            object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                }

                override fun onFinish() {
                    // 时间到，自动提交
                    submitQuiz(
                        quiz = quiz,
                        answers = answers,
                        userId = userId,
                        submissionId = submissionId,
                        db = db,
                        onSuccess = { result ->
                            isQuizCompleted = true
                            score = result.first
                            totalPoints = result.second
                        },
                        onError = { msg ->
                            errorMessage = msg
                        }
                    )
                }
            }.start()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz?.title ?: "Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isQuizStarted && !isQuizCompleted) {
                        Text(
                            text = formattedTime,
                            modifier = Modifier.padding(end = 16.dp)
                        )
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
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage != null -> {
                    // 显示错误
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colors.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { navController.navigateUp() }
                        ) {
                            Text("Go Back")
                        }
                    }
                }

                isQuizCompleted -> {
                    // 显示结果
                    QuizResultScreen(
                        score = score,
                        totalPoints = totalPoints,
                        onFinish = { navController.navigateUp() }
                    )
                }

                !isQuizStarted && quiz != null -> {
                    // 显示测验介绍
                    QuizIntroScreen(
                        quiz = quiz!!,
                        onStart = {
                            isQuizStarted = true

                            // 创建或更新进行中的提交
                            val submission = QuizSubmission(
                                id = submissionId.ifEmpty { UUID.randomUUID().toString() },
                                quizId = quizId,
                                studentId = userId,
                                startedAt = System.currentTimeMillis(),
                                status = SubmissionStatus.IN_PROGRESS
                            )

                            db.collection("quiz_submissions")
                                .document(submission.id)
                                .set(submission)
                                .addOnSuccessListener {
                                    submissionId = submission.id
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    )
                }

                isQuizStarted && quiz != null -> {
                    // 显示当前问题
                    val questions = quiz!!.questions

                    if (questions.isEmpty()) {
                        Text(
                            text = "No questions in this quiz.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 进度指示器
                            LinearProgressIndicator(
                                progress = (currentQuestionIndex + 1).toFloat() / questions.size,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                                style = MaterialTheme.typography.subtitle1
                            )

                            // 显示当前问题
                            val currentQuestion = questions[currentQuestionIndex]
                            QuestionView(
                                question = currentQuestion,
                                selectedAnswers = answers[currentQuestion.id] ?: emptyList(),
                                onAnswerSelected = { selected ->
                                    answers = answers.toMutableMap().apply {
                                        put(currentQuestion.id, selected)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp)
                            )

                            // 导航按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        if (currentQuestionIndex > 0) {
                                            currentQuestionIndex--
                                        }
                                    },
                                    enabled = currentQuestionIndex > 0
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Previous")
                                }

                                if (currentQuestionIndex < questions.size - 1) {
                                    Button(
                                        onClick = {
                                            currentQuestionIndex++
                                        }
                                    ) {
                                        Text("Next")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            // 提交测验
                                            isSubmitting = true
                                            submitQuiz(
                                                quiz = quiz,
                                                answers = answers,
                                                userId = userId,
                                                submissionId = submissionId,
                                                db = db,
                                                onSuccess = { result ->
                                                    isSubmitting = false
                                                    isQuizCompleted = true
                                                    score = result.first
                                                    totalPoints = result.second
                                                },
                                                onError = { msg ->
                                                    isSubmitting = false
                                                    errorMessage = msg
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = MaterialTheme.colors.primary
                                        )
                                    ) {
                                        Text("Submit")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Check, contentDescription = "Submit")
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // 未知状态
                    Text(
                        text = "Unknown state. Please go back and try again.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 提交中遮罩
            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Submitting your answers...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizIntroScreen(
    quiz: Quiz,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.h5,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (quiz.description.isNotEmpty()) {
                    Text(
                        text = quiz.description,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Time Limit: ${quiz.timeLimit} minutes",
                    style = MaterialTheme.typography.body2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Questions: ${quiz.questions.size}",
                    style = MaterialTheme.typography.body2
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Start Quiz")
                }
            }
        }
    }
}

@Composable
fun QuestionView(
    question: Question,
    selectedAnswers: List<String>,
    onAnswerSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 问题文本
        Text(
            text = question.text,
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 问题类型
        Text(
            text = if (question.type == QuestionType.SINGLE_CHOICE)
                "Single Choice Question" else "Multiple Choice Question",
            style = MaterialTheme.typography.caption
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 选项列表
        LazyColumn {
            items(question.options.size) { index ->
                val option = question.options[index]
                val isSelected = selectedAnswers.contains(index.toString())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colors.primary else Color.LightGray,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (question.type == QuestionType.SINGLE_CHOICE) {
                        // 单选
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onAnswerSelected(listOf(index.toString()))
                            }
                        )
                    } else {
                        // 多选
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                val newAnswers = selectedAnswers.toMutableList()
                                if (checked) {
                                    newAnswers.add(index.toString())
                                } else {
                                    newAnswers.remove(index.toString())
                                }
                                onAnswerSelected(newAnswers)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = option,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    score: Int,
    totalPoints: Int,
    onFinish: () -> Unit
) {
    val percentage = if (totalPoints > 0) (score * 100 / totalPoints) else 0

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Quiz Completed!",
                    style = MaterialTheme.typography.h5
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your Score",
                    style = MaterialTheme.typography.h6
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$score/$totalPoints",
                    style = MaterialTheme.typography.h4,
                    color = when {
                        percentage >= 80 -> Color(0xFF4CAF50) // 绿色
                        percentage >= 60 -> Color(0xFFFFC107) // 黄色
                        else -> Color(0xFFF44336) // 红色
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.h6
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = score.toFloat() / totalPoints.coerceAtLeast(1),
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        percentage >= 80 -> Color(0xFF4CAF50) // 绿色
                        percentage >= 60 -> Color(0xFFFFC107) // 黄色
                        else -> Color(0xFFF44336) // 红色
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Finish")
                }
            }
        }
    }
}

// 提交测验并计算分数
fun submitQuiz(
    quiz: Quiz?,
    answers: Map<String, List<String>>,
    userId: String,
    submissionId: String,
    db: FirebaseFirestore,
    onSuccess: (Pair<Int, Int>) -> Unit,
    onError: (String) -> Unit
) {
    if (quiz == null) {
        onError("Quiz data is missing")
        return
    }

    // 计算分数
    var score = 0
    var totalPoints = 0

    quiz.questions.forEach { question ->
        val userAnswers = answers[question.id] ?: emptyList()
        val correctAnswers = question.correctAnswers

        totalPoints += question.points

        // 检查答案是否正确
        if (userAnswers.sorted() == correctAnswers.sorted()) {
            score += question.points
        }
    }

    // 创建或更新提交
    val submission = QuizSubmission(
        id = submissionId.ifEmpty { UUID.randomUUID().toString() },
        quizId = quiz.id,
        studentId = userId,
        startedAt = System.currentTimeMillis(), // 这里应该保留原始开始时间，但为简化起见使用当前时间
        completedAt = System.currentTimeMillis(),
        answers = answers,
        score = score,
        totalPoints = totalPoints,
        status = SubmissionStatus.COMPLETED
    )

    // 保存到Firestore
    db.collection("quiz_submissions")
        .document(submission.id)
        .set(submission)
        .addOnSuccessListener {
            onSuccess(Pair(score, totalPoints))
        }
        .addOnFailureListener { e ->
            onError("Error submitting quiz: ${e.message}")
        }
}