package ui.roles.tutor

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.elearning.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * 获取drawable资源列表
 * 通过反射获取所有符合条件的drawable资源ID
 */
fun getDrawableResources(context: Context): List<Int> {
    val drawableClass = R.drawable::class.java
    return drawableClass.fields
        .filter { field ->
            // 过滤头像图片，这里包含: profile开头、student开头、tutor开头和默认launcher图标
            val name = field.name
            name.startsWith("profile") ||
                    name.startsWith("student") ||
                    name.startsWith("tutor") ||
                    name == "ic_launcher_foreground" ||
                    name == "ic_launcher_background"
        }
        .map { field ->
            try {
                field.getInt(null)
            } catch (e: Exception) {
                0 // 获取失败返回0
            }
        }
        .filter { it != 0 } // 过滤掉无效的资源ID
}

@Composable
fun ViewTutorProfile(navController: NavController) {
    // Editable user data (stored in memory for the session)
    var userName by remember { mutableStateOf("Unknown") }
    var userBio by remember { mutableStateOf("This is your bio.") }
    var selectedAvatarId by remember { mutableStateOf(R.drawable.profile) } // Default avatar
    var showAvatarDialog by remember { mutableStateOf(false) }

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Get context for Toast messages
    val context = LocalContext.current

    // 动态获取可用头像
    val avatarOptions = remember {
        getDrawableResources(context)
    }

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

                    // Get avatar ID if it exists, otherwise use default
                    val avatarId = document.getLong("avatarId")?.toInt()
                    if (avatarId != null) {
                        selectedAvatarId = avatarId
                    }
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
                    navController.navigateUp() // Go back to the previous screen
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Avatar Section
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colors.primary, CircleShape)
                    .background(Color.LightGray)
                    .clickable { showAvatarDialog = true }
            ) {
                Image(
                    painter = painterResource(id = selectedAvatarId),
                    contentDescription = "Profile Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Camera icon overlay for indicating it's changeable
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Avatar",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display user's name text below avatar
        Text(
            text = userName,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Editable Name
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Editable Bio
        OutlinedTextField(
            value = userBio,
            onValueChange = { userBio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(onClick = {
            val user = auth.currentUser
            if (user != null) {
                // Prepare the updated data to be pushed to Firestore
                val updatedData = hashMapOf(
                    "name" to userName,
                    "bio" to userBio,
                    "avatarId" to selectedAvatarId
                )

                // Update Firestore with new data
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
        },
            modifier = Modifier.fillMaxWidth()) {
            Text("Save Changes")
        }
    }

    // Avatar selection dialog - 改用网格布局展示所有可用头像
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Select Profile Avatar") },
            text = {
                Column {
                    Text("Choose an avatar from the options below:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // 使用LazyVerticalGrid替代简单Row，以更好地支持多个头像选项
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),  // 每行展示3个头像
                        modifier = Modifier.height(240.dp)  // 设置高度以便滚动
                    ) {
                        items(avatarOptions) { avatarId ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (selectedAvatarId == avatarId) 3.dp else 1.dp,
                                        color = if (selectedAvatarId == avatarId)
                                            MaterialTheme.colors.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedAvatarId = avatarId
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = avatarId),
                                    contentDescription = "Avatar Option",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAvatarDialog = false }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showAvatarDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}