package ui.roles.student

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.elearning.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun StudentDashboard(navController: NavController) {
    // Get user from Firebase Authentication
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Remember drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope() // Create a coroutine scope

    // Remember student data (email and name)
    val userEmail = remember { mutableStateOf(user?.email ?: "No Email") }
    val userName = remember { mutableStateOf("Unknown") } // Default to "Unknown" if no name exists

    // Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()

    // Fetch the user's name from Firestore
    LaunchedEffect(user?.uid) {
        if (user != null) {
            // Get user data from Firestore using the user's UID
            db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Update the userName with the data from Firestore
                    userName.value = document.getString("name") ?: "Unknown"
                }
            }
        }
    }

    // Use ModalDrawer to handle drawer content
    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile picture or default image
                    val profileImage = painterResource(id = R.drawable.profile) // Use a default profile image
                    Image(
                        painter = profileImage,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Welcome, ${userName.value}",
                        style = TextStyle(fontSize = 18.sp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Drawer elements as clickable Text
                Text(
                    text = "View Profile",
                    style = TextStyle(fontSize = 16.sp, color = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate("viewProfile") // Navigate to View Profile
                            coroutineScope.launch {
                                drawerState.close() // Close the drawer after navigating
                            }
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Logout",
                    style = TextStyle(fontSize = 16.sp, color = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            // Add logout logic here
                            auth.signOut()
                            navController.navigate("login") // Navigate to login screen
                            coroutineScope.launch {
                                drawerState.close() // Close the drawer after logging out
                            }
                        }
                )
            }
        }
    ) {
        // Main content of the screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Welcome, ${userName.value}") },
                    navigationIcon = {
                        // Hamburger Menu Icon
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.open() // Open the drawer when clicked
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) {
            // Main content area with padding applied here
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Apply padding to the column
            ) {
                Text("Student Dashboard", modifier = Modifier.padding(16.dp))

                // Display the student's email
                Text("Email: ${userEmail.value}")
            }
        }
    }
}
