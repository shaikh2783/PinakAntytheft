package com.app.antitheft.data.datasource

import com.google.firebase.database.*

object FirebaseLocationFlagDataSource {

    private var listenerMap = mutableMapOf<String, ValueEventListener>()

    fun observeLocationFlag(userId: String, callback: (Boolean) -> Unit) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$userId/Tracking/isLocationRequire")

        // If already listening → ignore
        if (listenerMap.containsKey(userId)) return

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Boolean::class.java) ?: false
                callback(value)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        listenerMap[userId] = listener
        ref.addValueEventListener(listener)
    }

    fun removeListener(userId: String) {
        val listener = listenerMap[userId] ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$userId/Tracking/isLocationRequire")

        ref.removeEventListener(listener)
        listenerMap.remove(userId)
    }
}
