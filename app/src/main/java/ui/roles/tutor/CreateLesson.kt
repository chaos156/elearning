package ui.roles.tutor

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// Data class for Lesson Page
data class LessonPage(
    var textContent: String = "",
    var imageUrl: String? = null
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreateLesson(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var lessonTitle by remember { mutableStateOf("") }
    var lessonPages by remember { mutableStateOf(mutableListOf(LessonPage())) }
    var selectedCourse by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Fetch courses for dropdown
    var courses by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        val courseDocs = db.collection("courses")
            .whereEqualTo("tutorId", auth.currentUser?.uid)
            .get().await()
        courses = courseDocs.documents.map { it.id }
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
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Page Button
            Button(onClick = { lessonPages = lessonPages.toMutableList().apply { add(LessonPage()) } }) {
                Text("Add Page")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Lesson Button
            Button(
                onClick = {
                    val user = auth.currentUser
                    if (user != null && selectedCourse != null) {
                        val lessonData = hashMapOf(
                            "title" to lessonTitle,
                            "courseId" to selectedCourse,
                            "tutorId" to user.uid
                        )

                        // Save lesson and pages
                        db.collection("lessons").add(lessonData)
                            .addOnSuccessListener { lessonRef ->
                                lessonPages.forEachIndexed { index, page ->
                                    val pageData = hashMapOf(
                                        "textContent" to page.textContent,
                                        "imageUrl" to page.imageUrl
                                    )
                                    lessonRef.collection("pages").document("Page ${index + 1}")
                                        .set(pageData)
                                }
                                Toast.makeText(context, "Lesson Created Successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Lesson")
            }
        }
    }
}

// Composable for each lesson page input
@Composable
fun LessonPageInput(
    lessonPage: LessonPage,
    onTextChange: (String) -> Unit,
    onImageSelected: (String?) -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                uploadImageToFirebase(storage, uri) { url ->
                    onImageSelected(url)
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

            // Select Image Button
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Select Image")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display Selected Image
            if (lessonPage.imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(lessonPage.imageUrl),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }
    }
}

// Function to upload image to Firebase Storage
fun uploadImageToFirebase(
    storage: FirebaseStorage,
    imageUri: Uri,
    onSuccess: (String) -> Unit
) {
    val ref = storage.reference.child("lesson_images/${imageUri.lastPathSegment}")
    ref.putFile(imageUri).addOnSuccessListener {
        ref.downloadUrl.addOnSuccessListener { uri ->
            onSuccess(uri.toString())
        }
    }
}
