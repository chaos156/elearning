package ui.roles.student

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Button
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Icon
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.IconButton
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.OutlinedTextField
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
//noinspection UsingMaterialAndMaterial3Libraries
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ViewStudentProfile(navController: NavController) {
    // Editable user data (stored in memory for the session)
    var userName by remember { mutableStateOf("Unknown") }
    var userBio by remember { mutableStateOf("This is your bio.") }

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Get context for Toast messages
    val context = LocalContext.current

    // Fetch user data from FirebaseAuth when the screen is loaded
    LaunchedEffect(true) {
        val user = auth.currentUser
        if (user != null) {
            // Fetch user data from Firestore using the userId (UID) as document ID
            db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Set the user name and bio from Firestore or defaults
                    userName = document.getString("name") ?: "Unknown"
                    userBio = document.getString("bio") ?: "This is your bio."
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // App Bar with Back Button
        TopAppBar(
            title = { Text("Edit Profile") },
            navigationIcon = {
                IconButton(onClick = {
                    // Use navigateUp() to go back to the previous screen (Student Dashboard)
                    navController.navigateUp() // Go back to the previous screen
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Editable Name
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Editable Bio
        OutlinedTextField(
            value = userBio,
            onValueChange = { userBio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(onClick = {
            val user = auth.currentUser
            if (user != null) {
                // Prepare the updated data to be pushed to Firestore
                val updatedData = hashMapOf(
                    "name" to userName,
                    "bio" to userBio
                )

                // Update Firestore with new name and bio
                db.collection("users").document(user.uid).update(updatedData as Map<String, Any>)
                    .addOnSuccessListener {
                        // Successfully updated the profile
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        // Handle failure
                        Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }) {
            Text("Save Changes")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ViewStudentProfilePreview() {
    // Use a dummy NavController here for preview purposes
    ViewStudentProfile(navController = rememberNavController())
}