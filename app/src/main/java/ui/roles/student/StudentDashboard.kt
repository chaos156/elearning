package ui.roles.student

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.elearning.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DashboardMenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun StudentDashboard(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var userName by remember { mutableStateOf("Student") }
    var userEmail by remember { mutableStateOf(user?.email ?: "No Email") }
    var enrolledCourses by remember { mutableStateOf(0) }
    var completedLessons by remember { mutableStateOf(0) }
    var totalLessons by remember { mutableStateOf(0) }
    var progressPercentage by remember { mutableStateOf(0f) }

    // Fetch user data and statistics
    LaunchedEffect(user?.uid) {
        if (user != null) {
            try {
                // Get user profile data
                val userDoc = db.collection("users").document(user.uid).get().await()
                userName = userDoc.getString("name") ?: "Student"

                // Count enrolled courses
                val enrollments = db.collection("enrollments")
                    .whereEqualTo("studentId", user.uid)
                    .whereEqualTo("status", "approved")
                    .get().await()

                enrolledCourses = enrollments.documents.size

                // Calculate lesson progress
                var completed = 0
                var total = 0
                val courseIds = enrollments.documents.mapNotNull { it.getString("courseId") }

                for (courseId in courseIds) {
                    val lessons = db.collection("lessons")
                        .whereEqualTo("courseId", courseId)
                        .get().await()

                    total += lessons.size()

                    for (lesson in lessons.documents) {
                        val lessonId = lesson.id
                        val submission = db.collection("lesson_submissions")
                            .whereEqualTo("lessonId", lessonId)
                            .whereEqualTo("userId", user.uid)
                            .get().await()

                        if (submission.documents.isNotEmpty()) {
                            completed++
                        }
                    }
                }

                completedLessons = completed
                totalLessons = total
                progressPercentage = if (totalLessons > 0) {
                    completedLessons.toFloat() / totalLessons
                } else {
                    0f
                }
            } catch (e: Exception) {
                println("Error fetching student data: ${e.message}")
            }
        }
    }

    // Define menu items
    val menuItems = listOf(
        DashboardMenuItem(
            "My Courses",
            Icons.Outlined.MenuBook,
            "myLessons",
            Color(0xFF4CAF50)
        ),
        DashboardMenuItem(
            "Browse Courses",
            Icons.Outlined.Search,
            "viewLessons",
            Color(0xFF2196F3)
        ),
        DashboardMenuItem(
            "Book a Session",
            Icons.Outlined.CalendarToday,
            "viewCalendar",
            Color(0xFFFFC107)
        ),
        DashboardMenuItem(
            "Profile",
            Icons.Outlined.Person,
            "viewProfile",
            Color(0xFFE91E63)
        )
    )

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Profile section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    val profileImage = painterResource(id = R.drawable.profile)
                    Image(
                        painter = profileImage,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = userName,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = userEmail,
                            style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                        )
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Drawer menu items
                menuItems.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(item.route)
                                coroutineScope.launch {
                                    drawerState.close()
                                }
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = item.color
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.title,
                            style = TextStyle(fontSize = 16.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()

                // Logout option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        .padding(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = "Logout",
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Logout",
                        style = TextStyle(fontSize = 16.sp, color = Color.Red)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Student Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        // Notification icon (placeholder for future functionality)
                        IconButton(onClick = { /* Handle notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
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
            ) {
                // Welcome card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Welcome back, $userName!",
                            style = MaterialTheme.typography.h5
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continue your learning journey",
                            style = MaterialTheme.typography.body1,
                            color = Color.Gray
                        )
                    }
                }

                // Statistics section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Enrolled courses stats
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .padding(end = 8.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = enrolledCourses.toString(),
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            )
                            Text(
                                text = "Enrolled Courses",
                                style = TextStyle(fontSize = 14.sp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Completed lessons stats
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .padding(start = 8.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$completedLessons/$totalLessons",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            )
                            Text(
                                text = "Lessons Completed",
                                style = TextStyle(fontSize = 14.sp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Progress indicator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Overall Progress",
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progressPercentage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            backgroundColor = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progressPercentage * 100).toInt()}% complete",
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // Quick access menu
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(menuItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { navController.navigate(item.route) },
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = item.color.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.color,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}