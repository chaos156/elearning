package ui.roles.student

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class for Page Content
data class PageContent(
    val imageUrl: String? = null,
    val textContent: String = ""
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "RememberReturnType")
@Composable
fun LessonContent(navController: NavController, lessonId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // State to hold the lesson pages and submission status
    val (pages, setPages) = remember { mutableStateOf<List<PageContent>>(emptyList()) }
    val (isSubmitted, setIsSubmitted) = remember { mutableStateOf(false) }  // Track submission status

    // Fetch the pages for the selected lesson
    LaunchedEffect(lessonId) {
        try {
            // Fetch pages for the current lesson
            val pagesQuery = db.collection("lessons").document(lessonId).collection("pages")
                .get().await()

            // Map pages to PageContent objects
            val fetchedPages = pagesQuery.documents.mapNotNull { document ->
                val data = document.data
                if (data != null) {
                    // Get imageUrl and textContent from the document
                    val imageUrl = data["imageUrl"] as? String
                    val textContent = data["textContent"] as? String ?: "No content available"
                    PageContent(imageUrl, textContent)
                } else {
                    null
                }
            }

            // Update state with fetched pages
            setPages(fetchedPages)

            // Check if this lesson has been already submitted
            val userId = auth.currentUser?.uid ?: return@LaunchedEffect
            val submissionQuery = db.collection("lesson_submissions")
                .whereEqualTo("lessonId", lessonId)
                .whereEqualTo("userId", userId)
                .get().await()

            // If there is a submission for this lesson, lock the button
            setIsSubmitted(submissionQuery.documents.isNotEmpty())

        } catch (e: Exception) {
            println("Error fetching page content: ${e.message}")
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
                    println("Lesson submission successful!")
                }
                .addOnFailureListener { e ->
                    println("Error submitting lesson: ${e.message}")
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson Content") },
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
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (pages.isEmpty()) {
                Text("No content available for this lesson.", style = MaterialTheme.typography.h6)
            } else {
                // Display pages in a list (if multiple pages)
                pages.forEach { page ->
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
                }
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

// Load image from URL using Coil
@Composable
fun LoadImageFromUrl(url: String) {
    // Using Coil's rememberImagePainter to load images
    val painter = rememberImagePainter(url)

    // Display the image
    Image(painter = painter, contentDescription = "Image", modifier = Modifier.fillMaxSize())
}
