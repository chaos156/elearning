package ui.roles.student

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class for Page Content
data class PageContent(
    val imageUrl: String? = null,
    val textContent: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "RememberReturnType")
@Composable
fun LessonContent(navController: NavController, lessonId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State to hold the lesson pages and submission status
    val (pages, setPages) = remember { mutableStateOf<List<PageContent>>(emptyList()) }
    val (isSubmitted, setIsSubmitted) = remember { mutableStateOf(false) }  // Track submission status
    val (isLoading, setIsLoading) = remember { mutableStateOf(true) }
    val (errorMessage, setErrorMessage) = remember { mutableStateOf<String?>(null) }
    val (lessonTitle, setLessonTitle) = remember { mutableStateOf("Lesson Content") }

    // Fetch the pages for the selected lesson
    LaunchedEffect(lessonId) {
        try {
            setIsLoading(true)
            setErrorMessage(null)

            println("🔍 Starting to load content for lesson ID: $lessonId")

            // First fetch the lesson details to get the title
            val lessonDoc = db.collection("lessons").document(lessonId).get().await()
            lessonDoc.getString("title")?.let { title ->
                setLessonTitle(title)
                println("✅ Lesson title: $title")
            }

            // Fetch pages for the current lesson - without using pageOrder
            val pagesQuery = db.collection("lessons").document(lessonId).collection("pages")
                .get().await()

            println("📄 Found ${pagesQuery.documents.size} pages for lesson")

            if (pagesQuery.documents.isEmpty()) {
                println("⚠️ No pages found for this lesson")
                setPages(emptyList())
                setIsLoading(false)
                return@LaunchedEffect
            }

            // Map pages to PageContent objects with additional debugging
            val fetchedPages = pagesQuery.documents.mapNotNull { document ->
                println("📄 Processing page document: ${document.id}")

                val data = document.data
                if (data != null) {
                    // Get properties with improved defaults
                    val imageUrl = data["imageUrl"] as? String
                    val textContent = data["textContent"] as? String ?: "No specific content provided for this page."
                    val mediaUrl = data["mediaUrl"] as? String
                    val mediaType = data["mediaType"] as? String

                    // Debug logging
                    println("📄 Page ${document.id}:")
                    println("   - Text: ${textContent.take(30)}...")
                    println("   - Image URL: $imageUrl")
                    println("   - Media URL: $mediaUrl")
                    println("   - Media Type: $mediaType")

                    PageContent(imageUrl, textContent, mediaUrl, mediaType)
                } else {
                    println("   ⚠️ Document data is null")
                    null
                }
            }

            // Update state with fetched pages
            setPages(fetchedPages)

            if (fetchedPages.isEmpty()) {
                println("⚠️ No valid pages found for this lesson")
            } else {
                println("✅ Successfully loaded ${fetchedPages.size} pages")
            }

            // Check if this lesson has been already submitted
            val userId = auth.currentUser?.uid ?: return@LaunchedEffect
            val submissionQuery = db.collection("lesson_submissions")
                .whereEqualTo("lessonId", lessonId)
                .whereEqualTo("userId", userId)
                .get().await()

            // If there is a submission for this lesson, lock the button
            val hasSubmission = submissionQuery.documents.isNotEmpty()
            setIsSubmitted(hasSubmission)
            if (hasSubmission) {
                println("✅ Lesson has been previously submitted")
            }

            setIsLoading(false)
        } catch (e: Exception) {
            println("❌ Error fetching page content: ${e.message}")
            println("❌ Error stack trace: ${e.stackTraceToString()}")
            setErrorMessage("Error loading lesson content: ${e.message}")
            setIsLoading(false)
        }
    }

    // Function to handle lesson submission
    fun handleCompleteLesson() {
        val userId = auth.currentUser?.uid ?: return // Get the current user's ID from FirebaseAuth

        // Mark this lesson as completed by the student
        if (!isSubmitted) {
            db.collection("lesson_submissions")
                .add(mapOf(
                    "lessonId" to lessonId,
                    "userId" to userId,
                    "submittedAt" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    setIsSubmitted(true)  // Lock the button after submission
                    println("✅ Lesson submission successful!")
                }
                .addOnFailureListener { e ->
                    println("❌ Error submitting lesson: ${e.message}")
                    setErrorMessage("Failed to submit lesson: ${e.message}")
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Loading indicator
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading lesson content...")
                }
            }
            // Error message
            else if (errorMessage != null) {
                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colors.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colors.error
                        )
                    }
                }

                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Go Back")
                }
            }
            // No pages found
            else if (pages.isEmpty()) {
                Text("No content available for this lesson.", style = MaterialTheme.typography.h6)

                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Go Back")
                }
            }
            // Display pages content
            else {
                // Display pages content
                pages.forEachIndexed { index, page ->
                    Text(
                        text = "Page ${index + 1}",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // If there's an image, load it
                    page.imageUrl?.let { imageUrl ->
                        LoadImageFromUrl(url = imageUrl)
                    }

                    // Display the text content
                    Text(
                        text = page.textContent,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Display media content if available
                    page.mediaUrl?.let { mediaUrl ->
                        when (page.mediaType) {
                            "pdf" -> EnhancedPDFViewer(url = mediaUrl)
                            "audio" -> AudioPlayer(url = mediaUrl)
                            "video" -> VideoPlayer(url = mediaUrl)
                            else -> {
                                // If media type is unknown but URL exists, try to open it generically
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    elevation = 4.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Unknown Media Type",
                                            style = MaterialTheme.typography.subtitle1
                                        )
                                        Text(
                                            "Media URL: $mediaUrl",
                                            style = MaterialTheme.typography.caption
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Text("Open Media")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Display the "Complete" button at the end of the lesson content
                if (!isSubmitted) {
                    Button(
                        onClick = { handleCompleteLesson() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Complete Lesson")
                    }
                } else {
                    // Button is locked after submission
                    Button(
                        onClick = { /* No action, already submitted */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        enabled = false  // Disable the button after submission
                    ) {
                        Text("Lesson Completed")
                    }
                }
            }
        }
    }
}

// Load image from URL using Coil
@Composable
fun LoadImageFromUrl(url: String) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Using Coil's rememberAsyncImagePainter to load images
    val painter = rememberAsyncImagePainter(
        model = url,
        onSuccess = { isLoading = false },
        onError = {
            isLoading = false
            hasError = true
        }
    )

    // Display the image
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Loading image...")
            }
        }

        if (hasError) {
            Card(
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Failed to load image",
                            color = MaterialTheme.colors.error
                        )
                        Text(
                            text = "URL: $url",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            Image(
                painter = painter,
                contentDescription = "Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

// Enhanced PDF Viewer Component with better error handling and fallback options
@Composable
fun EnhancedPDFViewer(url: String) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = "PDF Document",
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PDF Document",
                    style = MaterialTheme.typography.subtitle1
                )
            }

            // Display the URL for debugging
            Text(
                text = "PDF URL: $url",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Primary PDF button
            OutlinedButton(
                onClick = {
                    try {
                        // First try to open with a PDF intent
                        val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                            type = "application/pdf"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        // Check if there's an app to handle PDF viewing
                        if (pdfIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(pdfIntent)
                            errorMessage = null
                        } else {
                            // Fallback to browser if no PDF viewer is available
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(browserIntent)
                            errorMessage = null
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error opening PDF: ${e.message}"
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open PDF Document")
            }

            // Display error message if any
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Alternative download option
            OutlinedButton(
                onClick = {
                    try {
                        // Open in browser as fallback
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(browserIntent)
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = "Error opening browser: ${e.message}"
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Open in Browser")
            }
        }
    }
}

// Audio Player Component
@Composable
fun AudioPlayer(url: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mediaPlayer = remember { MediaPlayer() }
    val context = LocalContext.current

    DisposableEffect(url) {
        try {
            mediaPlayer.apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    // Media is ready
                    isLoading = false
                }
                setOnErrorListener { mp, what, extra ->
                    isLoading = false
                    errorMessage = "Error loading audio: $what-$extra"
                    true
                }
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Failed to initialize audio: ${e.message}"
            e.printStackTrace()
        }

        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AudioFile,
                    contentDescription = "Audio Player",
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audio Player",
                    style = MaterialTheme.typography.subtitle1
                )
            }

            // Show loading indicator
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("Loading audio...")
                }
            }
            // Show error message
            else if (errorMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colors.error
                    )
                }

                // Fallback option
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Open in External Player")
                }
            }
            // Show player controls
            else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer.pause()
                            } else {
                                mediaPlayer.start()
                            }
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    Text(
                        if (isPlaying) "Pause" else "Play",
                        style = MaterialTheme.typography.body1
                    )
                }
            }

            // Show audio URL for debugging
            Text(
                text = "Audio URL: $url",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Video Player Component using ExoPlayer
@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    // Create an ExoPlayer instance
    val exoPlayer = remember {
        try {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                prepare()
                isInitialized = true
            }
        } catch (e: Exception) {
            errorMessage = "Failed to initialize video player: ${e.message}"
            null
        }
    }

    // Handle lifecycle events
    if (exoPlayer != null) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        exoPlayer.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        // Don't auto-play on resume
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        exoPlayer.release()
                    }
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                exoPlayer.release()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "Video Player",
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Video Player",
                    style = MaterialTheme.typography.subtitle1
                )
            }

            // If there was an error initializing ExoPlayer
            if (errorMessage != null) {
                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colors.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colors.error
                            )
                        }

                        Text(
                            text = "Video URL: $url",
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(url), "video/*")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Open in External Player")
                        }
                    }
                }
            }
            // If ExoPlayer was initialized successfully
            else if (isInitialized && exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f/9f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        }
                    ) {
                        Text(if (exoPlayer.isPlaying) "Pause" else "Play")
                    }
                }

                // Show video URL for debugging
                Text(
                    text = "Video URL: $url",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}