package com.example.taller3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.adapter.UserAdapter
import com.example.taller3.databinding.ActivityListUsersBinding
import com.example.taller3.model.MyUser
import com.google.firebase.database.*

class ListUsersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListUsersBinding
    private lateinit var database: DatabaseReference
    private lateinit var userList: MutableList<MyUser>
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().getReference("users")
        userList = mutableListOf()
        adapter = UserAdapter(this, userList)

        binding.main.adapter = adapter

        fetchUsers()
    }

    private fun fetchUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(MyUser::class.java)
                    if (user != null && user.available) {
                        user.id = userSnapshot.key ?: ""
                        userList.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}