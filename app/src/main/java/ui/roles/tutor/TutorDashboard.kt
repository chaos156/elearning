package ui.roles.tutor

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

data class ActivityItem(
    val title: String,
    val subtitle: String,
    val time: String,
    val type: String
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TutorDashboard(navController: NavController) {
    // Get user from Firebase Authentication
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    // Remember drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State variables
    var userName by remember { mutableStateOf("Tutor") }
    var userEmail by remember { mutableStateOf(user?.email ?: "No Email") }
    var totalCourses by remember { mutableStateOf(0) }
    var totalStudents by remember { mutableStateOf(0) }
    var totalLessons by remember { mutableStateOf(0) }
    var pendingRequests by remember { mutableStateOf(0) }
    var recentActivities by remember { mutableStateOf(listOf<ActivityItem>()) }

    // Fetch the user's data from Firestore
    LaunchedEffect(user?.uid) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("name") ?: "Tutor"
                    }
                }

            // Count courses
            db.collection("courses")
                .whereEqualTo("tutorId", user.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    totalCourses = querySnapshot.size()

                    // Get lesson count for all courses
                    var lessonCount = 0
                    for (document in querySnapshot.documents) {
                        db.collection("lessons")
                            .whereEqualTo("courseId", document.id)
                            .get()
                            .addOnSuccessListener { lessonSnapshot ->
                                lessonCount += lessonSnapshot.size()
                                totalLessons = lessonCount
                            }
                    }
                }

            // Count enrolled students (unique)
            db.collection("enrollments")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener { enrollmentSnapshot ->
                    val studentIds = mutableSetOf<String>()

                    for (document in enrollmentSnapshot.documents) {
                        val courseId = document.getString("courseId") ?: continue
                        val studentId = document.getString("studentId") ?: continue

                        db.collection("courses").document(courseId).get()
                            .addOnSuccessListener { courseDoc ->
                                if (courseDoc.getString("tutorId") == user.uid) {
                                    studentIds.add(studentId)
                                    totalStudents = studentIds.size
                                }
                            }
                    }
                }

            // Count pending enrollment requests
            db.collection("enrollments")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { enrollmentSnapshot ->
                    var count = 0

                    for (document in enrollmentSnapshot.documents) {
                        val courseId = document.getString("courseId") ?: continue

                        db.collection("courses").document(courseId).get()
                            .addOnSuccessListener { courseDoc ->
                                if (courseDoc.getString("tutorId") == user.uid) {
                                    count++
                                    pendingRequests = count
                                }
                            }
                    }
                }
        }
    }

    // Define menu items
    val menuItems = listOf(
        DashboardItem(
            "Create Course",
            Icons.Outlined.LibraryAdd,
            "viewCourses",
            Color(0xFFF44336)
        ),
        DashboardItem(
            "Create Lesson",
            Icons.Outlined.PostAdd,
            "createLessons",
            Color(0xFF9C27B0)
        ),
        DashboardItem(
            "Student Progress",
            Icons.Outlined.TrendingUp,
            "viewStudentProgress",
            Color(0xFF3F51B5)
        ),
        DashboardItem(
            "Enrollment Requests",
            Icons.Outlined.PersonAdd,
            "viewRequests",
            Color(0xFF009688)
        ),
        DashboardItem(
            "Calendar",
            Icons.Outlined.CalendarToday,
            "createCalender",
            Color(0xFFFF9800)
        ),
        DashboardItem(
            "Profile",
            Icons.Outlined.Person,
            "viewProfile", // 恢复回"viewProfile"
            Color(0xFF795548)
        )
    )

    // Use ModalDrawer to handle drawer content
    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile picture or default image
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
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = userEmail,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Menu Items
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
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()

                // Logout
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
                    title = { Text("Tutor Dashboard") },
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
                        // Notification icon with badge if there are pending requests
                        if (pendingRequests > 0) {
                            BadgedBox(
                                badge = {
                                    Badge { Text(pendingRequests.toString()) }
                                }
                            ) {
                                IconButton(onClick = { navController.navigate("viewRequests") }) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications"
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { /* No action */ }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
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
                // Welcome Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Welcome back, $userName",
                            style = MaterialTheme.typography.h5
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage your courses and monitor student progress",
                            style = MaterialTheme.typography.body2,
                            color = Color.Gray
                        )
                    }
                }

                // Statistics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Courses Stats
                    StatCard(
                        value = totalCourses.toString(),
                        label = "Courses",
                        color = Color(0xFFF44336),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    // Students Stats
                    StatCard(
                        value = totalStudents.toString(),
                        label = "Students",
                        color = Color(0xFF3F51B5),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )

                    // Lessons Stats
                    StatCard(
                        value = totalLessons.toString(),
                        label = "Lessons",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions Section
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // First row of actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Create Course Action
                    ActionCard(
                        item = menuItems[0],
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        onClick = { navController.navigate(menuItems[0].route) }
                    )

                    // Create Lesson Action
                    ActionCard(
                        item = menuItems[1],
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        onClick = { navController.navigate(menuItems[1].route) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Second row of actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Student Progress Action
                    ActionCard(
                        item = menuItems[2],
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        onClick = { navController.navigate(menuItems[2].route) }
                    )

                    // Enrollment Requests Action
                    ActionCard(
                        item = menuItems[3],
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        onClick = { navController.navigate(menuItems[3].route) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Third row of actions - Calendar and Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Calendar Action
                    ActionCard(
                        item = menuItems[4],
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        onClick = { navController.navigate(menuItems[4].route) }
                    )

                    // Profile Action
                    ActionCard(
                        item = menuItems[5],
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        onClick = { navController.navigate(menuItems[5].route) }
                    )
                }

                // 如果有pendingRequests，显示通知卡片
                if (pendingRequests > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("viewRequests") },
                        elevation = 4.dp,
                        backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Pending Requests",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pending Enrollment Requests",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "You have $pendingRequests pending student requests",
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = TextStyle(fontSize = 14.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActionCard(
    item: DashboardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}