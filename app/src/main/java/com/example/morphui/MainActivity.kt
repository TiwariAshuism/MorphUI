package com.example.morphui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.morphui.ui.theme.MorphUITheme
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        enableEdgeToEdge()
        setContent {
            MorphUITheme {
                FirebaseScreen(database)
            }
        }
    }
}

@Composable
fun FirebaseScreen(database: DatabaseReference) {

    var inputText by remember { mutableStateOf("") }
    var dbValue by remember { mutableStateOf("Waiting for data...") }

    // 🔥 Read data in real-time
    LaunchedEffect(Unit) {
        database.child("message")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dbValue = snapshot.getValue(String::class.java) ?: "No Data"
                }

                override fun onCancelled(error: DatabaseError) {
                    dbValue = "Error: ${error.message}"
                }
            })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(text = "Realtime DB Value:")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = dbValue)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter message") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 🔥 Write to Firebase
                database.child("message").setValue(inputText)
                inputText = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save to Firebase")
        }
    }
}
