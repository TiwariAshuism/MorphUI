package com.app.sdui.data.remote

import com.app.sdui.data.dto.ComponentDto
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService(private val database: DatabaseReference) {
    
    init {
        try {
            database.database.getReference(".info/connected").addValueEventListener(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    android.util.Log.d("FirebaseService", "Connection Status: ${if (connected) "CONNECTED" else "DISCONNECTED"}")
                }
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FirebaseService", "Connection listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Failed to add connection listener", e)
        }
    }
    
    fun observeScreen(screenId: String): Flow<Result<ComponentDto>> = callbackFlow {
        android.util.Log.d("FirebaseService", "Observing screen: $screenId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    android.util.Log.d("FirebaseService", "Data fetched: ${snapshot.value}")
                    val dto = snapshot.getValue(ComponentDto::class.java)
                    if (dto != null) {
                        trySend(Result.success(dto))
                    } else {
                        android.util.Log.d("FirebaseService", "DT is null for path: ${snapshot.ref}")
                        trySend(Result.failure(Exception("No data found")))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseService", "Error parsing data: ${e.message}", e)
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseService", "Database error: ${error.message}")
                trySend(Result.failure(error.toException()))
            }
        }

        val ref = database.child("screens/$screenId")
        android.util.Log.d("FirebaseService", "Adding listener to: $ref")
        ref.addValueEventListener(listener)

        awaitClose {
            android.util.Log.d("FirebaseService", "Removing listener for: $screenId")
            ref.removeEventListener(listener)
        }
    }

    suspend fun fetchScreenOnce(screenId: String): Result<ComponentDto> {
        return try {
            val snapshot = database.child("screens/$screenId").get().await()
            val dto = snapshot.getValue(ComponentDto::class.java)
            if (dto != null) {
                Result.success(dto)
            } else {
                Result.failure(Exception("No data found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
