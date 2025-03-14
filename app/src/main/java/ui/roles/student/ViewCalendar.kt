package ui.roles.student

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun StudentBookingScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var selectedDate by remember { mutableStateOf("") }
    var availableSlots by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }

    // Fetch available slots when selectedDate changes
    LaunchedEffect(selectedDate) {
        if (selectedDate.isNotEmpty()) {
            val query = db.collection("availability")
                .whereEqualTo("date", selectedDate)
                .get().await()

            val slots = query.documents.flatMap { doc ->
                val tutorId = doc.getString("tutorId") ?: ""
                val docId = doc.id  // Get the document ID for updating the "booked" status
                val times = doc.get("timeSlots") as? List<Map<String, Any>> ?: emptyList()

                times.filter { !(it["booked"] as Boolean) }
                    .map { Triple(tutorId, it["time"] as String, docId) }
            }

            availableSlots = slots
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Book a Session") },navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        ) }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = { Text("Select Date (YYYY-MM-DD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (availableSlots.isEmpty() && selectedDate.isNotEmpty()) {
                Text("No available slots for this date.", style = MaterialTheme.typography.h6)
            } else {
                LazyColumn {
                    items(availableSlots) { (tutorId, time, docId) ->
                        Button(
                            onClick = {
                                val studentId = auth.currentUser?.uid ?: return@Button
                                val bookingData = hashMapOf(
                                    "tutorId" to tutorId,
                                    "studentId" to studentId,
                                    "date" to selectedDate,
                                    "time" to time,
                                    "status" to "confirmed"
                                )

                                // Add the booking to Firestore
                                db.collection("bookings").add(bookingData)
                                    .addOnSuccessListener {
                                        println("Booking confirmed")

                                        // **Mark the slot as booked in the availability document**
                                        db.collection("availability").document(docId)
                                            .get()
                                            .addOnSuccessListener { doc ->
                                                val timeSlots = doc.get("timeSlots") as? MutableList<Map<String, Any>> ?: return@addOnSuccessListener
                                                val updatedSlots = timeSlots.map {
                                                    if (it["time"] == time) it.toMutableMap().apply { this["booked"] = true }
                                                    else it
                                                }

                                                // Update Firestore with the modified timeSlots list
                                                db.collection("availability").document(docId)
                                                    .update("timeSlots", updatedSlots)
                                                    .addOnSuccessListener {
                                                        println("Slot marked as booked")
                                                        availableSlots = availableSlots.filterNot { it.second == time }  // Remove booked slot from UI
                                                    }
                                                    .addOnFailureListener { e -> println("Failed to update slot: ${e.message}") }
                                            }
                                    }
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text("Book $time with Tutor $tutorId")
                        }
                    }
                }
            }
        }
    }
}
