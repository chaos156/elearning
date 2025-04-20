package ui.roles.tutor

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
// 修正图标导入
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await


// Data class for Lesson Page
data class LessonPage(
    var textContent: String = "",
    var imageUrl: String? = null,
    var mediaUrl: String? = null,
    var mediaType: String? = null  // "pdf", "audio", "video"
)

fun sendEmailNotifications(
    functions: FirebaseFunctions,
    studentEmails: List<String>,
    lessonTitle: String,
    courseId: String,
    courseName: String
) {
    val data = hashMapOf(
        "studentEmails" to studentEmails,
        "lessonTitle" to lessonTitle,
        "courseId" to courseId,
        "courseName" to courseName
    )

    functions.getHttpsCallable("sendLessonNotification")
        .call(data)
        .addOnSuccessListener {
            println("✅ Email notifications sent successfully!")
        }
        .addOnFailureListener { e ->
            println("❌ Error sending emails: ${e.message}")
        }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreateLesson(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    val functions = FirebaseFunctions.getInstance()

    var lessonTitle by remember { mutableStateOf("") }
    var lessonPages by remember { mutableStateOf(mutableListOf(LessonPage())) }
    var selectedCourse by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch courses for dropdown
    var courses by remember { mutableStateOf(listOf<String>()) }

    // Fetch the highest lesson order for the selected course
    var highestLessonOrder by remember { mutableStateOf(0) }

    // Fetch course list on first launch
    LaunchedEffect(Unit) {
        try {
            val courseDocs = db.collection("courses")
                .whereEqualTo("tutorId", auth.currentUser?.uid)
                .get().await()
            courses = courseDocs.documents.map { it.id }
            println("✅ Loaded ${courses.size} courses")
        } catch (e: Exception) {
            println("❌ Error loading courses: ${e.message}")
        }
    }

    // Fetch the highest lesson order when the selected course changes
    LaunchedEffect(selectedCourse) {
        if (selectedCourse != null) {
            try {
                // Get lessons for the selected course and find the highest order
                val lessonsQuery = db.collection("lessons")
                    .whereEqualTo("courseId", selectedCourse)
                    .get().await()

                // Set the highest lessonOrder based on the existing lessons
                highestLessonOrder = lessonsQuery.documents.maxOfOrNull {
                    it.getLong("lessonOrder")?.toInt() ?: 0
                } ?: 0
                println("✅ Current highest lesson order: $highestLessonOrder")
            } catch (e: Exception) {
                println("❌ Error fetching lesson order: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Lesson") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // Course Selection Dropdown
            Text("Select Course", style = MaterialTheme.typography.h6)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = selectedCourse ?: "Choose a Course")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    courses.forEach { course ->
                        DropdownMenuItem(onClick = {
                            selectedCourse = course
                            expanded = false
                        }) {
                            Text(course)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lesson Title Input
            OutlinedTextField(
                value = lessonTitle,
                onValueChange = { lessonTitle = it },
                label = { Text("Lesson Title") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lesson Pages List
            Text("Lesson Pages", style = MaterialTheme.typography.h6)
            LazyColumn {
                items(lessonPages.size) { index ->
                    LessonPageInput(
                        lessonPage = lessonPages[index],
                        onTextChange = { updatedText ->
                            lessonPages = lessonPages.toMutableList().apply {
                                this[index] = this[index].copy(textContent = updatedText)
                            }
                        },
                        onImageSelected = { updatedImageUrl ->
                            lessonPages = lessonPages.toMutableList().apply {
                                this[index] = this[index].copy(imageUrl = updatedImageUrl)
                            }
                        },
                        onMediaSelected = { mediaUrl, mediaType ->
                            lessonPages = lessonPages.toMutableList().apply {
                                this[index] = this[index].copy(mediaUrl = mediaUrl, mediaType = mediaType)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Page Button
            Button(onClick = {
                lessonPages = lessonPages.toMutableList().apply { add(LessonPage()) }
            }) {
                Text("Add Page")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message display
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Save Lesson Button
            Button(
                onClick = {
                    val user = auth.currentUser
                    if (user != null && selectedCourse != null) {
                        isCreating = true
                        errorMessage = null

                        // Increment lessonOrder based on the highest order
                        val newLessonOrder = highestLessonOrder + 1

                        // Check if any page has media content
                        val hasMediaContent = lessonPages.any {
                            it.mediaUrl != null && it.mediaType != null
                        }

                        // Log for debugging
                        println("⚠️ Creating lesson with ${lessonPages.size} pages")
                        lessonPages.forEachIndexed { index, page ->
                            println("📄 Page ${index + 1}:")
                            println("   - Text: ${page.textContent.take(30)}...")
                            println("   - Image URL: ${page.imageUrl}")
                            println("   - Media URL: ${page.mediaUrl}")
                            println("   - Media Type: ${page.mediaType}")
                        }

                        val lessonData = hashMapOf(
                            "title" to lessonTitle,
                            "courseId" to selectedCourse,
                            "tutorId" to user.uid,
                            "lessonOrder" to newLessonOrder,
                            "hasMedia" to hasMediaContent,
                            "createdAt" to System.currentTimeMillis()
                        )

                        // Save lesson and pages
                        db.collection("lessons").add(lessonData)
                            .addOnSuccessListener { lessonRef ->
                                println("✅ Lesson created with ID: ${lessonRef.id}")

                                // Add a counter to track completion
                                var pagesCompleted = 0
                                val totalPages = lessonPages.size

                                lessonPages.forEachIndexed { index, page ->
                                    val pageData = hashMapOf(
                                        "textContent" to page.textContent,
                                        "imageUrl" to page.imageUrl,
                                        "mediaUrl" to page.mediaUrl,
                                        "mediaType" to page.mediaType,
                                        "pageOrder" to index + 1
                                    )

                                    // Log the data being saved
                                    println("📤 Saving page ${index + 1} data:")
                                    pageData.forEach { (key, value) ->
                                        println("   $key: $value")
                                    }

                                    lessonRef.collection("pages").document("Page ${index + 1}")
                                        .set(pageData)
                                        .addOnSuccessListener {
                                            pagesCompleted++
                                            println("✅ Page ${index + 1} saved successfully ($pagesCompleted/$totalPages)")

                                            // Verify data after saving
                                            lessonRef.collection("pages").document("Page ${index + 1}")
                                                .get()
                                                .addOnSuccessListener { doc ->
                                                    println("🔍 Verification - Page ${index + 1} data in Firestore:")
                                                    doc.data?.forEach { (key, value) ->
                                                        println("   $key: $value")
                                                    }

                                                    // Check if media data was correctly saved
                                                    if (page.mediaUrl != null) {
                                                        val savedMediaUrl = doc.getString("mediaUrl")
                                                        val savedMediaType = doc.getString("mediaType")

                                                        if (savedMediaUrl != page.mediaUrl || savedMediaType != page.mediaType) {
                                                            println("⚠️ WARNING: Media data mismatch!")
                                                            println("   Original - URL: ${page.mediaUrl}, Type: ${page.mediaType}")
                                                            println("   Saved - URL: $savedMediaUrl, Type: $savedMediaType")
                                                        } else {
                                                            println("✅ Media data correctly saved")
                                                        }
                                                    }

                                                    // Once all pages are completed, finish the process
                                                    if (pagesCompleted == totalPages) {
                                                        isCreating = false
                                                        Toast.makeText(context, "Lesson Created Successfully", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    println("❌ Failed to verify page data: ${e.message}")
                                                    if (pagesCompleted == totalPages) {
                                                        isCreating = false
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            println("❌ Failed to save page ${index + 1}: ${e.message}")
                                            errorMessage = "Error saving page ${index + 1}: ${e.message}"
                                            isCreating = false
                                        }
                                }

                                // 发送邮件通知给已注册学生
                                db.collection("enrollments")
                                    .whereEqualTo("courseId", selectedCourse)
                                    .whereEqualTo("status", "approved") // 只获取已批准的注册
                                    .get()
                                    .addOnSuccessListener { enrollmentDocs ->
                                        val studentIds = enrollmentDocs.documents.mapNotNull {
                                            it.getString("studentId")
                                        }

                                        if (studentIds.isNotEmpty()) {
                                            // 查询每个学生的邮箱
                                            val studentEmails = mutableListOf<String>()
                                            var completedQueries = 0

                                            for (studentId in studentIds) {
                                                db.collection("users").document(studentId).get()
                                                    .addOnSuccessListener { userDoc ->
                                                        val email = userDoc.getString("email")
                                                        if (email != null) {
                                                            studentEmails.add(email)
                                                        }

                                                        completedQueries++
                                                        // 当所有学生信息都查询完毕后发送邮件
                                                        if (completedQueries == studentIds.size && studentEmails.isNotEmpty()) {
                                                            // 获取课程名称
                                                            db.collection("courses").document(selectedCourse!!)
                                                                .get()
                                                                .addOnSuccessListener { courseDoc ->
                                                                    val courseName = courseDoc.getString("courseName") ?: "课程"

                                                                    sendEmailNotifications(
                                                                        functions,
                                                                        studentEmails,
                                                                        lessonTitle,
                                                                        selectedCourse!!,
                                                                        courseName
                                                                    )
                                                                }
                                                        }
                                                    }
                                            }
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                println("❌ Failed to create lesson: ${e.message}")
                                errorMessage = "Error creating lesson: ${e.message}"
                                isCreating = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Handle missing user or course
                        val message = when {
                            user == null -> "You must be logged in to create a lesson"
                            selectedCourse == null -> "Please select a course"
                            else -> "Unknown error"
                        }
                        errorMessage = message
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating && lessonTitle.isNotBlank() && selectedCourse != null
            ) {
                Text(if (isCreating) "Creating..." else "Create Lesson")
            }

            // Show progress indicator when creating
            if (isCreating) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// Composable for each lesson page input
@Composable
fun LessonPageInput(
    lessonPage: LessonPage,
    onTextChange: (String) -> Unit,
    onImageSelected: (String?) -> Unit,
    onMediaSelected: (String?, String?) -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var showMediaOptions by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // Image Picker
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                isUploading = true
                uploadError = null

                uploadImageToFirebase(
                    storage = storage,
                    imageUri = uri,
                    onSuccess = { url ->
                        isUploading = false
                        onImageSelected(url)
                    },
                    onError = { e ->
                        isUploading = false
                        uploadError = "Failed to upload image: ${e.message}"
                    },
                    onProgress = { progress ->
                        uploadProgress = progress
                    }
                )
            }
        }

    // Media (PDF, Audio, Video) Picker
    val mediaPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                mediaUri = uri
                val mimeType = context.contentResolver.getType(uri)

                // 详细记录 MIME 类型以便调试
                println("🔍 选择的媒体文件 MIME 类型: $mimeType")
                println("🔍 文件 URI: $uri")

                // 根据 MIME 类型确定媒体类型
                val mediaType = when {
                    mimeType?.startsWith("application/pdf") == true -> "pdf"
                    mimeType?.startsWith("audio") == true -> "audio"
                    mimeType?.startsWith("video") == true -> "video"
                    else -> {
                        // 如果 MIME 类型不明确，尝试从文件扩展名推断
                        val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
                        println("🔍 文件扩展名: $extension")

                        when (extension) {
                            "pdf" -> "pdf"
                            "mp3", "wav", "ogg", "aac" -> "audio"
                            "mp4", "mov", "avi", "mkv" -> "video"
                            else -> {
                                // 如果无法确定，显示错误消息
                                uploadError = "无法确定媒体类型，请选择 PDF、音频或视频文件"
                                println("❌ 无法确定媒体类型")
                                null
                            }
                        }
                    }
                }

                if (mediaType != null) {
                    isUploading = true
                    uploadError = null

                    println("🔍 确定的媒体类型: $mediaType")

                    // 使用改进的上传功能
                    uploadMediaToFirebase(
                        storage = storage,
                        mediaUri = uri,
                        mediaType = mediaType,
                        onSuccess = { url ->
                            isUploading = false
                            println("✅ 媒体上传成功。URL: $url, 类型: $mediaType")
                            onMediaSelected(url, mediaType)
                        },
                        onError = { e ->
                            isUploading = false
                            uploadError = "上传媒体失败: ${e.message}"
                            println("❌ 媒体上传错误: ${e.message}")
                            e.printStackTrace() // 打印完整堆栈跟踪以便调试
                        },
                        onProgress = { progress ->
                            uploadProgress = progress
                        }
                    )
                }
            }
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = lessonPage.textContent,
                onValueChange = { onTextChange(it) },
                label = { Text("Page Text") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Media Options Row
            Row(modifier = Modifier.fillMaxWidth()) {
                // Select Image Button
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    enabled = !isUploading
                ) {
                    Text("Add Image")
                }

                // Media Button
                Button(
                    onClick = { showMediaOptions = !showMediaOptions },
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    enabled = !isUploading
                ) {
                    Text("Add Media")
                }
            }

            // Upload Status
            if (isUploading) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Uploading...",
                        color = MaterialTheme.colors.primary
                    )
                    LinearProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }

            // Error message
            uploadError?.let {
                Text(
                    it,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Media Options Dropdown
            if (showMediaOptions) {
                Card(
                    elevation = 4.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    // 尝试启动选择器，并捕获任何可能发生的异常
                                    mediaPickerLauncher.launch("application/pdf")
                                    showMediaOptions = false
                                } catch (e: Exception) {
                                    uploadError = "启动 PDF 选择器失败: ${e.message}"
                                    println("❌ 启动 PDF 选择器时出错: ${e.message}")
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading
                        ) {
                            Text("Add PDF")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                try {
                                    mediaPickerLauncher.launch("audio/*")
                                    showMediaOptions = false
                                } catch (e: Exception) {
                                    uploadError = "启动音频选择器失败: ${e.message}"
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading
                        ) {
                            Text("Add Audio")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                try {
                                    mediaPickerLauncher.launch("video/*")
                                    showMediaOptions = false
                                } catch (e: Exception) {
                                    uploadError = "启动视频选择器失败: ${e.message}"
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading
                        ) {
                            Text("Add Video")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display Selected Image
            if (lessonPage.imageUrl != null) {
                Text("Selected Image:", style = MaterialTheme.typography.subtitle1)
                Image(
                    painter = rememberAsyncImagePainter(lessonPage.imageUrl),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            }

            // Display Selected Media Info
            if (lessonPage.mediaUrl != null) {
                val mediaTypeText = when (lessonPage.mediaType) {
                    "pdf" -> "PDF Document"
                    "audio" -> "Audio File"
                    "video" -> "Video File"
                    else -> "Media File"
                }

                Card(
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 修改图标引用
                            Icon(
                                imageVector = when (lessonPage.mediaType) {
                                    "pdf" -> Icons.Outlined.PictureAsPdf
                                    "audio" -> Icons.Outlined.AudioFile
                                    "video" -> Icons.Outlined.Videocam
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = mediaTypeText,
                                tint = MaterialTheme.colors.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "$mediaTypeText selected",
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.primary
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Remove media button
                            IconButton(onClick = { onMediaSelected(null, null) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove media",
                                    tint = MaterialTheme.colors.error
                                )
                            }
                        }

                        // Debug information
                        Text(
                            "Media URL: ${lessonPage.mediaUrl?.take(60)}...",
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            "Media Type: ${lessonPage.mediaType}",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }
}

// 改进的图片上传函数，与媒体上传函数保持一致
fun uploadImageToFirebase(
    storage: FirebaseStorage,
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit = { e -> println("Error uploading image: ${e.message}") },
    onProgress: (Float) -> Unit = {}
) {
    try {
        // 1. 确保使用安全的文件名 - 移除所有特殊字符
        val timestamp = System.currentTimeMillis()
        val safeFileName = imageUri.lastPathSegment?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?: "image_$timestamp"

        // 2. 构建安全的存储路径
        val storageRef = storage.reference
        val imageRef = storageRef.child("lesson_images/image_${timestamp}_$safeFileName")

        println("🔍 准备上传图片到路径: ${imageRef.path}")

        // 3. 设置适当的内容类型
        val metadata = storageMetadata {
            contentType = "image/jpeg"  // 可以根据实际情况调整
        }

        // 4. 上传文件并添加完整的错误处理
        val uploadTask = imageRef.putFile(imageUri, metadata)

        // 5. 监听上传进度
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat() / 100f
            println("📊 上传进度: ${(progress * 100).toInt()}%")
            onProgress(progress)
        }

        // 6. 处理上传成功
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            println("✅ 图片上传成功，正在获取下载 URL...")
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result.toString()
                println("🔗 获得下载 URL: $downloadUrl")
                onSuccess(downloadUrl)
            } else {
                val exception = task.exception ?: Exception("未知错误")
                println("❌ 获取下载 URL 失败: ${exception.message}")
                onError(exception)
            }
        }.addOnFailureListener { e ->
            println("❌ 上传失败: ${e.message}")
            onError(e)
        }
    } catch (e: Exception) {
        println("❌ 上传过程中出现异常: ${e.message}")
        e.printStackTrace()
        onError(e)
    }
}

// 改进的媒体上传函数，处理错误并避免路径问题
fun uploadMediaToFirebase(
    storage: FirebaseStorage,
    mediaUri: Uri,
    mediaType: String,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit = { e -> println("Error uploading media: ${e.message}") },
    onProgress: (Float) -> Unit = {}
) {
    try {
        // 1. 确保使用安全的文件名 - 移除所有特殊字符
        val timestamp = System.currentTimeMillis()
        val safeFileName = mediaUri.lastPathSegment?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?: "file_$timestamp"

        // 2. 构建安全的存储路径
        val storageRef = storage.reference
        val mediaRef = storageRef.child("lesson_media/${mediaType}_${timestamp}_$safeFileName")

        println("🔍 准备上传文件到路径: ${mediaRef.path}")

        // 3. 设置适当的内容类型
        val metadata = storageMetadata {
            contentType = when (mediaType) {
                "pdf" -> "application/pdf"
                "audio" -> "audio/mpeg"
                "video" -> "video/mp4"
                else -> "application/octet-stream"
            }
        }

        // 4. 上传文件并添加完整的错误处理
        val uploadTask = mediaRef.putFile(mediaUri, metadata)

        // 5. 监听上传进度
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat() / 100f
            println("📊 上传进度: ${(progress * 100).toInt()}%")
            onProgress(progress)
        }

        // 6. 处理上传成功
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            println("✅ 文件上传成功，正在获取下载 URL...")
            mediaRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result.toString()
                println("🔗 获得下载 URL: $downloadUrl")
                onSuccess(downloadUrl)
            } else {
                val exception = task.exception ?: Exception("未知错误")
                println("❌ 获取下载 URL 失败: ${exception.message}")
                onError(exception)
            }
        }.addOnFailureListener { e ->
            println("❌ 上传失败: ${e.message}")
            onError(e)
        }
    } catch (e: Exception) {
        println("❌ 上传过程中出现异常: ${e.message}")
        e.printStackTrace()
        onError(e)
    }
}