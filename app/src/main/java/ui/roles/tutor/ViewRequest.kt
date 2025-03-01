package ui.roles.tutor

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Enrollment(
    val id: String = "", // Enrollment Document ID
    val courseId: String = "",
    val studentId: String = "",
    val status: String = "pending"
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ViewRequests(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var enrollments by remember { mutableStateOf(listOf<Enrollment>()) }

    // Fetch pending enrollment requests
    LaunchedEffect(Unit) {
        val tutorId = auth.currentUser?.uid ?: return@LaunchedEffect

        val pendingEnrollments = mutableListOf<Enrollment>()

        val enrollmentDocs = db.collection("enrollments")
            .whereEqualTo("status", "pending")
            .get().await()

        for (doc in enrollmentDocs.documents) {
            val courseId = doc.getString("courseId") ?: continue
            val studentId = doc.getString("studentId") ?: continue

            // Check if the course belongs to the tutor
            val courseDoc = db.collection("courses").document(courseId).get().await()
            if (courseDoc.exists() && courseDoc.getString("tutorId") == tutorId) {
                pendingEnrollments.add(Enrollment(doc.id, courseId, studentId, "pending"))
            }
        }

        enrollments = pendingEnrollments
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Enrollments") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (enrollments.isEmpty()) {
                Text("No pending enrollments.")
            } else {
                LazyColumn {
                    items(enrollments) { enrollment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Student ID: ${enrollment.studentId}")
                                Text("Course ID: ${enrollment.courseId}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row {
                                    Button(
                                        onClick = {
                                            updateEnrollmentStatus(db, enrollment.id, "approved") {
                                                enrollments = enrollments.filter { it.id != enrollment.id }
                                                Toast.makeText(context, "Student Approved", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Accept")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            deleteEnrollment(db, enrollment.id) {
                                                enrollments = enrollments.filter { it.id != enrollment.id }
                                                Toast.makeText(context, "Request Rejected", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(MaterialTheme.colors.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Function to approve enrollment
fun updateEnrollmentStatus(db: FirebaseFirestore, enrollmentId: String, status: String, onSuccess: () -> Unit) {
    db.collection("enrollments").document(enrollmentId)
        .update("status", status)
        .addOnSuccessListener { onSuccess() }
}

// Function to reject enrollment request
fun deleteEnrollment(db: FirebaseFirestore, enrollmentId: String, onSuccess: () -> Unit) {
    db.collection("enrollments").document(enrollmentId)
        .delete()
        .addOnSuccessListener { onSuccess() }
}
