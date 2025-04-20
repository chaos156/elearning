package ui.quiz

// 测验数据模型
data class Quiz(
    val id: String = "",
    val courseId: String = "",  // 关联的课程ID
    val title: String = "",
    val description: String = "",
    val timeLimit: Int = 30,    // 分钟
    val createdAt: Long = 0,
    val questions: List<Question> = emptyList()
)

// 问题类型枚举
enum class QuestionType {
    MULTIPLE_CHOICE,    // 多选题
    SINGLE_CHOICE       // 单选题
}

// 问题数据模型
data class Question(
    val id: String = "",
    val text: String = "",
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String> = emptyList(),    // 选项列表
    val correctAnswers: List<String> = emptyList(), // 正确答案索引
    val points: Int = 1         // 分值
)

// 学生提交的测验答案
data class QuizSubmission(
    val id: String = "",
    val quizId: String = "",
    val studentId: String = "",
    val startedAt: Long = 0,
    val completedAt: Long = 0,
    val answers: Map<String, List<String>> = emptyMap(), // 问题ID -> 学生答案
    val score: Int = 0,
    val totalPoints: Int = 0,
    val status: SubmissionStatus = SubmissionStatus.IN_PROGRESS
)

// 提交状态枚举
enum class SubmissionStatus {
    IN_PROGRESS,    // 进行中
    COMPLETED       // 已完成
}

// 单个学生的测验结果，用于结果仪表板
data class StudentQuizResult(
    val studentId: String = "",
    val studentName: String = "",
    val score: Int = 0,
    val totalPoints: Int = 0,
    val completedAt: Long = 0
)