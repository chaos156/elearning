package ui.roles.tutor

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import ui.quiz.Question
import ui.quiz.QuestionType
import ui.quiz.Quiz
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreateQuizScreen(navController: NavController, courseId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 测验基本信息
    var quizTitle by remember { mutableStateOf("") }
    var quizDescription by remember { mutableStateOf("") }
    var timeLimit by remember { mutableStateOf("30") } // 默认30分钟

    // 问题列表
    var questions by remember { mutableStateOf(listOf<Question>()) }

    // 当前编辑的问题
    var currentQuestion by remember { mutableStateOf<Question?>(null) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentQuestionIndex by remember { mutableStateOf(-1) }

    // 保存进度
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 保存按钮
                    Button(
                        onClick = {
                            if (quizTitle.isBlank()) {
                                Toast.makeText(context, "Please enter a quiz title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (questions.isEmpty()) {
                                Toast.makeText(context, "Please add at least one question", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // 保存测验到Firestore
                            isSaving = true
                            val quizId = UUID.randomUUID().toString()
                            val quiz = Quiz(
                                id = quizId,
                                courseId = courseId,
                                title = quizTitle,
                                description = quizDescription,
                                timeLimit = timeLimit.toIntOrNull() ?: 30,
                                createdAt = System.currentTimeMillis(),
                                questions = questions
                            )

                            coroutineScope.launch {
                                try {
                                    // 创建测验文档
                                    db.collection("quizzes").document(quizId)
                                        .set(quiz)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Quiz created successfully", Toast.LENGTH_SHORT).show()
                                            // 添加到课程文档中的quizzes字段
                                            db.collection("courses").document(courseId)
                                                .update("quizzes", com.google.firebase.firestore.FieldValue.arrayUnion(quizId))
                                                .addOnSuccessListener {
                                                    isSaving = false
                                                    navController.navigateUp()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "Error updating course: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    isSaving = false
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error saving quiz: ${e.message}", Toast.LENGTH_SHORT).show()
                                            isSaving = false
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 测验信息部分
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quiz Information", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quizTitle,
                        onValueChange = { quizTitle = it },
                        label = { Text("Quiz Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quizDescription,
                        onValueChange = { quizDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = timeLimit,
                        onValueChange = { timeLimit = it },
                        label = { Text("Time Limit (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 问题列表部分
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Questions", style = MaterialTheme.typography.h6)

                        Button(
                            onClick = {
                                currentQuestion = Question(id = UUID.randomUUID().toString())
                                isEditing = false
                                showAddQuestionDialog = true
                            }
                        ) {
                            Text("Add Question")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (questions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No questions added yet")
                        }
                    } else {
                        Column {
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(questions) { index, question ->
                                    QuestionItem(
                                        question = question,
                                        index = index,
                                        onEdit = {
                                            currentQuestion = question
                                            currentQuestionIndex = index
                                            isEditing = true
                                            showAddQuestionDialog = true
                                        },
                                        onDelete = {
                                            questions = questions.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        }
                                    )
                                }
                            }

                            // Add a submit button at the bottom
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (quizTitle.isBlank()) {
                                        Toast.makeText(context, "Please enter a quiz title", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    if (questions.isEmpty()) {
                                        Toast.makeText(context, "Please add at least one question", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // 保存测验到Firestore
                                    isSaving = true
                                    val quizId = UUID.randomUUID().toString()
                                    val quiz = Quiz(
                                        id = quizId,
                                        courseId = courseId,
                                        title = quizTitle,
                                        description = quizDescription,
                                        timeLimit = timeLimit.toIntOrNull() ?: 30,
                                        createdAt = System.currentTimeMillis(),
                                        questions = questions
                                    )

                                    coroutineScope.launch {
                                        try {
                                            // 创建测验文档
                                            db.collection("quizzes").document(quizId)
                                                .set(quiz)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Quiz created successfully", Toast.LENGTH_SHORT).show()
                                                    // 添加到课程文档中的quizzes字段
                                                    db.collection("courses").document(courseId)
                                                        .update("quizzes", com.google.firebase.firestore.FieldValue.arrayUnion(quizId))
                                                        .addOnSuccessListener {
                                                            isSaving = false
                                                            navController.navigateUp()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Toast.makeText(context, "Error updating course: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            isSaving = false
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "Error saving quiz: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    isSaving = false
                                                }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            isSaving = false
                                        }
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Submit Quiz")
                            }
                        }
                    }
                }
            }
        }

        // 添加/编辑问题对话框
        if (showAddQuestionDialog && currentQuestion != null) {
            AddEditQuestionDialog(
                question = currentQuestion!!,
                isEditing = isEditing,
                onSave = { editedQuestion ->
                    if (isEditing) {
                        // 更新问题
                        questions = questions.toMutableList().apply {
                            set(currentQuestionIndex, editedQuestion)
                        }
                    } else {
                        // 添加新问题
                        questions = questions + editedQuestion
                    }
                    showAddQuestionDialog = false
                },
                onDismiss = {
                    showAddQuestionDialog = false
                }
            )
        }

        // 显示保存进度指示器
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Saving...")
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionItem(
    question: Question,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Question ${index + 1}",
                    style = MaterialTheme.typography.subtitle1
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = question.text,
                style = MaterialTheme.typography.body1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (question.type == QuestionType.SINGLE_CHOICE) "Single Choice" else "Multiple Choice",
                style = MaterialTheme.typography.caption
            )

            if (question.options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Options: ${question.options.size}",
                    style = MaterialTheme.typography.caption
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Points: ${question.points}",
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
fun AddEditQuestionDialog(
    question: Question,
    isEditing: Boolean,
    onSave: (Question) -> Unit,
    onDismiss: () -> Unit
) {
    // 问题状态
    var questionText by remember { mutableStateOf(question.text) }
    var questionType by remember { mutableStateOf(question.type) }
    var questionPoints by remember { mutableStateOf(question.points.toString()) }

    // 使用 mutableStateListOf 来管理选项和正确答案
    val options = remember { mutableStateListOf<String>().apply { addAll(question.options) } }
    val correctAnswers = remember { mutableStateListOf<String>().apply { addAll(question.correctAnswers) } }

    // 新选项
    var newOption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Question" else "Add Question") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                item {
                    // 问题文本
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Question Text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 问题类型选择
                    Text("Question Type", style = MaterialTheme.typography.subtitle1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 单选题选项
                        Button(
                            onClick = { questionType = QuestionType.SINGLE_CHOICE },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (questionType == QuestionType.SINGLE_CHOICE)
                                    MaterialTheme.colors.primary else MaterialTheme.colors.surface
                            ),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                "Single Choice",
                                color = if (questionType == QuestionType.SINGLE_CHOICE) Color.White else Color.Black
                            )
                        }

                        // 多选题选项
                        Button(
                            onClick = { questionType = QuestionType.MULTIPLE_CHOICE },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (questionType == QuestionType.MULTIPLE_CHOICE)
                                    MaterialTheme.colors.primary else MaterialTheme.colors.surface
                            ),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                "Multiple Choice",
                                color = if (questionType == QuestionType.MULTIPLE_CHOICE) Color.White else Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 分值
                    OutlinedTextField(
                        value = questionPoints,
                        onValueChange = { questionPoints = it },
                        label = { Text("Points") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 添加选项
                    Text("Options", style = MaterialTheme.typography.subtitle1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newOption,
                            onValueChange = { newOption = it },
                            label = { Text("New Option") },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (newOption.isNotBlank()) {
                                    // 直接添加到选项列表
                                    options.add(newOption)
                                    newOption = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Option")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Select Correct Answer(s):",
                        style = MaterialTheme.typography.caption
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 显示选项列表
                items(options.size) { index ->
                    val option = options[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (questionType == QuestionType.SINGLE_CHOICE) {
                            // 单选
                            RadioButton(
                                selected = correctAnswers.contains(index.toString()),
                                onClick = {
                                    correctAnswers.clear()
                                    correctAnswers.add(index.toString())
                                }
                            )
                        } else {
                            // 多选
                            Checkbox(
                                checked = correctAnswers.contains(index.toString()),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        correctAnswers.add(index.toString())
                                    } else {
                                        correctAnswers.remove(index.toString())
                                    }
                                }
                            )
                        }

                        Text(option, modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                // 删除选项
                                options.removeAt(index)

                                // 更新后续答案的索引
                                val updatedAnswers = mutableListOf<String>()
                                for (answer in correctAnswers) {
                                    val answerIndex = answer.toIntOrNull()
                                    if (answerIndex != null) {
                                        if (answerIndex > index) {
                                            updatedAnswers.add((answerIndex - 1).toString())
                                        } else if (answerIndex < index) {
                                            updatedAnswers.add(answer)
                                        }
                                    }
                                }
                                correctAnswers.clear()
                                correctAnswers.addAll(updatedAnswers)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Option")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 验证问题数据
                    if (questionText.isBlank()) {
                        return@Button
                    }

                    // 确保有选项和正确答案
                    if (options.isEmpty() || correctAnswers.isEmpty()) {
                        return@Button
                    }

                    // 创建更新后的问题
                    val updatedQuestion = question.copy(
                        text = questionText,
                        type = questionType,
                        options = options.toList(),
                        correctAnswers = correctAnswers.toList(),
                        points = questionPoints.toIntOrNull() ?: 1
                    )

                    onSave(updatedQuestion)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}