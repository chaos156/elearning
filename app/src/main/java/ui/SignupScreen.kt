package ui

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Button
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.RadioButton
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Fix for unresolved reference

@Composable
fun SignupScreen(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val role = remember { mutableStateOf("Student") } // Default role
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = email.value, onValueChange = { email.value = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            RadioButton(selected = role.value == "Student", onClick = { role.value = "Student" })
            Text("Student")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = role.value == "Tutor", onClick = { role.value = "Tutor" })
            Text("Tutor")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                auth.createUserWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val user = hashMapOf("email" to email.value, "role" to role.value)

                            userId?.let {
                                db.collection("users").document(it).set(user)
                                    .addOnSuccessListener { Toast.makeText(context, "Signup Successful", Toast.LENGTH_SHORT).show() }
                                    .addOnFailureListener { Toast.makeText(context, "Error Saving Role", Toast.LENGTH_SHORT).show() }
                            }
                        } else {
                            task.exception?.let {
                                Toast.makeText(context, "Signup Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }) {
                Text("Sign Up")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Login Button
            Button(onClick = { navController.navigate("login") }) {
                Text("Login")
            }
        }
    }
}

