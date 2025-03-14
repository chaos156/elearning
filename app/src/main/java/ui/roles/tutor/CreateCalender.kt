package ui.roles.tutor

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
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
import java.util.Calendar


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreateCalendar(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf("") }
    var timeSlots by remember { mutableStateOf(mutableListOf<String>()) }
    var newTimeSlot by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Show Date Picker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = "$year-${month + 1}-$day"
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Availability") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            Text("Select Date", style = MaterialTheme.typography.h6)
            Button(onClick = { showDatePicker = true }) {
                Text(text = selectedDate.ifEmpty { "Choose a Date" })
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newTimeSlot,
                onValueChange = { newTimeSlot = it },
                label = { Text("Enter Time (HH:mm)") },
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (newTimeSlot.isNotBlank()) {
                        timeSlots = (timeSlots + newTimeSlot).toMutableList()
                        newTimeSlot = ""
                    } else {
                        Toast.makeText(context, "Enter a valid time slot", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Time Slot")
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(timeSlots) { time ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = 4.dp
                    ) {
                        Text(
                            text = time,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.h6
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val tutorId = auth.currentUser?.uid ?: return@Button

                    if (selectedDate.isBlank() || timeSlots.isEmpty()) {
                        Toast.makeText(context, "Please select a date and add time slots!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val availabilityData = hashMapOf(
                        "tutorId" to tutorId,
                        "date" to selectedDate,
                        "timeSlots" to timeSlots.map { mapOf("time" to it, "booked" to false) }
                    )

                    db.collection("availability").add(availabilityData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Availability added successfully!", Toast.LENGTH_SHORT).show()
                            selectedDate = ""
                            timeSlots = mutableListOf()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error adding availability!", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Availability")
            }
        }
    }
}
