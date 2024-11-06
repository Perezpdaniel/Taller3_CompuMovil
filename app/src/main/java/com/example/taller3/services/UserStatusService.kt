package com.example.taller3.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taller3.MainActivity
import com.example.taller3.UserLocationActivity
import com.example.taller3.model.MyUser
import com.google.firebase.auth.FirebaseAuth
import com.example.taller3.R
import com.google.firebase.database.*
import com.example.taller3.services.UserStatusService


class UserStatusService : Service() {

    private val database = FirebaseDatabase.getInstance().reference.child("users")
    private val auth = FirebaseAuth.getInstance()
    private lateinit var userListener: ValueEventListener

    private val userStatusMap = mutableMapOf<String, Boolean>()


    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        Log.d("UserStatusService", "Servicio iniciado en Primer Plano")
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "USER_STATUS_CHANNEL")
            .setContentTitle("Servicio Activo")
            .setContentText("Escuchando cambios en la disponibilidad de usuarios.")
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .build()
        startForeground(1, notification)
        listenForAvailableUsers()
    }


    private fun listenForAvailableUsers() {
        Log.d("UserStatusService", "Iniciando carga de usuarios iniciales")
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserStatusService", "Datos iniciales cargados")
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(MyUser::class.java)
                    val userId = userSnapshot.key

                    if (user != null && userId != null) {
                        userStatusMap[userId.toString()] = user.available
                    }
                }
                addRealtimeListener()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserStatusService", "Error al cargar usuarios iniciales: ${error.message}")
            }
        })
    }

    private fun addRealtimeListener() {
        Log.d("UserStatusService", "Añadiendo listener en tiempo real")
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserStatusService", "Detectando cambios en usuarios")
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(MyUser::class.java)
                    val currentUser = auth.currentUser
                    val userId = userSnapshot.key

                    if (user != null && currentUser != null && userId != currentUser.uid) {
                        val wasAvailable = userStatusMap[userId.toString()] ?: false
                        if (user.available && !wasAvailable) {
                            Log.d("UserStatusService", "Usuario ${user.name} ${user.lastname} ahora está disponible")
                            sendNotification(user)
                        }
                        userStatusMap[userId.toString()] = user.available
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserStatusService", "Error al escuchar cambios: ${error.message}")
            }
        }
        database.addValueEventListener(userListener)
    }

    private fun sendNotification(user: MyUser) {
        if (user.id.isEmpty()) {
            Log.e("UserStatusService", "El ID del usuario está vacío. No se puede enviar notificación.")
            return
        }

        val intent = Intent(this, UserLocationActivity::class.java).apply {
            putExtra("latitude", user.latitud)
            putExtra("longitude", user.longitud)
            putExtra("userName", "${user.name} ${user.lastname}")
            putExtra("userEmail", user.email)
            putExtra("USER_ID", user.id)
        }

        val targetActivity = if (auth.currentUser != null) UserLocationActivity::class.java else MainActivity::class.java
        intent.setClass(this, targetActivity)

        val pendingIntent = PendingIntent.getActivity(
            this,
            user.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "USER_STATUS_CHANNEL")
            .setContentTitle("Usuario Disponible")
            .setContentText("${user.name} ${user.lastname} está ahora disponible.")
            .setSmallIcon(R.drawable.baseline_notifications_active_24) // Verifica este recurso
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(user.id.hashCode(), notification)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "USER_STATUS_CHANNEL",
                "Notificaciones de Usuarios",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones cuando un usuario se pone disponible"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        database.removeEventListener(userListener)
    }
}
