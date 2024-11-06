package com.example.taller3

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taller3.databinding.ActivityMainBinding
import com.example.taller3.model.MyUser
import com.example.taller3.services.UserStatusService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
//:)
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    val TAG = "FIREBASE_APP"

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            comenzarServicioNotificaciones()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_LONG).show()
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Verificar si el usuario ya está logueado
        if (auth.currentUser != null) {
            Log.d(TAG, "User already logged in, updating UI.")
            updateUI(auth.currentUser)
        }

        // Listener para el botón de registro
        binding.signup.setOnClickListener {
            startActivity(Intent(baseContext, RegisterActivity::class.java))
        }

        // Listener para el botón de iniciar sesión
        binding.logInButton.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            Log.d(TAG, "Botón iniciar sesión presionado")
            if (validateForm(email, password)) {
                Log.d(TAG, "Formulario validado, intentando iniciar sesión.")
                loginUser(email, password)
            }
        }
        permisoNotificacionesYServicio(android.Manifest.permission.POST_NOTIFICATIONS)

    }

    // Actualizar la UI si el usuario está autenticado
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            /*
            val userId = currentUser.uid
            userRef = database.getReference("users").child(userId)

            Log.d(TAG, "Attempting to retrieve user data for userId: $userId")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(MyUser::class.java)
                        if (user != null) {
                            Log.d(TAG, "User data retrieved, navigating to HomeActivity.")
                            val intent = Intent(baseContext, HomeActivity::class.java)
                            intent.putExtra("user", user)
                            startActivity(intent)
                        } else {
                            Log.d(TAG, "User data is null")
                            Toast.makeText(baseContext, "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d(TAG, "User data not found")
                        Toast.makeText(baseContext, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error retrieving user data: ${databaseError.message}")

                }

            })
            */
             startActivity(Intent(baseContext, HomeActivity::class.java))
        }
    }

    // Función para manejar el login con Firebase
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Login successful, updating UI.")
                Toast.makeText(baseContext, "Login successful!", Toast.LENGTH_SHORT).show()
                updateUI(auth.currentUser)
            } else {
                val message = it.exception?.message
                Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Login error: $message")
                binding.email.text.clear()
                binding.password.text.clear()
            }
        }
    }

    // Validar el formulario antes de iniciar sesión
    private fun validateForm(email: String, password: String): Boolean {
        var valid = false
        if (email.isEmpty()) {
            binding.email.error = "Required!"
        } else if (!validEmailAddress(email)) {
            binding.email.error = "Invalid email address"
        } else if (password.isEmpty()) {
            binding.password.error = "Required!"
        } else if (password.length < 6) {
            binding.password.error = "Password should be at least 6 characters long!"
        } else {
            valid = true
        }
        return valid
    }

    // Validar formato de correo electrónico
    private fun validEmailAddress(email: String): Boolean {
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(regex.toRegex())
    }

    private fun permisoNotificacionesYServicio(permission:String) {
        if(ContextCompat.checkSelfPermission(this,permission)== PackageManager.PERMISSION_DENIED){
            if(shouldShowRequestPermissionRationale(permission)){
                Toast.makeText(this,"Por favor acepte las notificaciones para poder recibir notificaciones",
                    Toast.LENGTH_LONG).show()
            }
            notificationPermissionLauncher.launch(permission)
        }else{
            comenzarServicioNotificaciones()
        }
    }

    private fun comenzarServicioNotificaciones() {
        val intent = Intent(this, UserStatusService::class.java)
        ContextCompat.startForegroundService(this, intent) // Usa startForegroundService si es necesario
    }


}