package com.example.taller3.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.taller3.R
import com.example.taller3.UserLocationActivity
import com.example.taller3.model.MyUser
import com.squareup.picasso.Picasso

class UserAdapter(private val context: Context, private val userList: List<MyUser>) : BaseAdapter() {

    override fun getCount(): Int {
        return userList.size
    }

    override fun getItem(position: Int): Any {
        return userList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.userow, parent, false)

        val user = userList[position]

        val photo = view.findViewById<ImageView>(R.id.photo)
        val userName = view.findViewById<TextView>(R.id.userName)
        val seeLocationButton = view.findViewById<Button>(R.id.seeLocation)

        userName.text = "${user.name} ${user.lastname}"
        if (user.image != null && user.image!!.isNotEmpty()) {
            Picasso.get().load(user.image).into(photo)
        } else {
            photo.setImageResource(R.drawable.baseline_person_24)
        }

        seeLocationButton.setOnClickListener {
            val intent = Intent(context, UserLocationActivity::class.java)
            intent.putExtra("USER_ID", user.id)
            intent.putExtra("USER_NAME", "${user.name} ${user.lastname}")
            intent.putExtra("USER_IMAGE", user.image)
            context.startActivity(intent)
        }

        return view
    }
}