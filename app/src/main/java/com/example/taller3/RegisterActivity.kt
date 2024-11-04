package com.example.taller3

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.taller3.databinding.ActivityRegisterBinding
import com.example.taller3.model.MyUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var profileImageUri: Uri? = null

    // Launchers para la cámara y la galería
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            profileImageUri?.let { uri ->
                binding.profilePhoto.setImageURI(uri)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            binding.profilePhoto.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Configuración de los botones para tomar foto o abrir galería
        binding.takePic.setOnClickListener { openCamera() }
        binding.gallery.setOnClickListener { openGallery() }

        // Botón de registro
        binding.registroButtom.setOnClickListener { registerUser() }

        // Botón de iniciar sesión
        binding.iniciarsesionbutton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun openCamera() {
        try {
            val file = File(filesDir, "profilePic.jpg")
            if (file.exists() || file.createNewFile()) {
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                profileImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(this, "Failed to create file for the camera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerUser() {
        val nombre = binding.nombre.text.toString()
        val apellido = binding.apellido.text.toString()
        val email = binding.correo.text.toString()
        val password = binding.contrasena.text.toString()
        val id = binding.userId.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val userId = firebaseUser?.uid ?: return@addOnCompleteListener

                        // Convert the Uri to a string
                        val imageUriString = profileImageUri?.toString()

                        // Create the MyUser object with the data and the image URI string
                        val user = MyUser(
                            name = nombre,
                            lastname = apellido,
                            email = email,
                            password = password,
                            image = imageUriString,
                            id = id,
                            latitud = 0.0,  // Replace these values with the actual latitude and longitude
                            longitud = 0.0,
                            available = false
                        )

                        // Save the user to the Firebase Realtime Database
                        database.child("users").child(userId).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                } else {
                                    Toast.makeText(this, "Error al guardar los datos del usuario: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }

                        profileImageUri?.let { uri ->
                            uploadImageFirebase(userId, uri) { imageUrl ->
                                saveUserToDatabase(userId, nombre, apellido, email, password, id, imageUrl)
                            }
                        } ?: saveUserToDatabase(userId, nombre, apellido, email, password, id, null)
                    } else {
                        Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Por favor ingresa correo y contraseña válidos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageFirebase(userId: String, uri: Uri, callback: (String?) -> Unit) {
        val storageRef = storage.reference.child("profileImages/$userId.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    callback(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error uploading image: ${it.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }

    private fun saveUserToDatabase(userId: String, nombre: String, apellido: String, email: String, password: String, id: String, imageUrl: String?) {
        val user = MyUser(
            name = nombre,
            lastname = apellido,
            email = email,
            password = password,
            image = imageUrl,
            id = id,
            latitud = 0.0,
            longitud = 0.0,
            available = false
        )

        database.child("users").child(userId).setValue(user)
            .addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                } else {
                    Toast.makeText(this, "Error al guardar los datos del usuario: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}