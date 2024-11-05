package com.example.taller3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller3.databinding.ActivityHomeBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private var userMarker: Marker? = null

    //permiso de la ubicacion
    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(), ActivityResultCallback {
            if(it){//granted
                locationSettings()
            }else {//denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        })

    private val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "GPS OFF!", Toast.LENGTH_SHORT).show()
            }
        }
    )


    //ubicacion del usuario
    lateinit var locationClient: FusedLocationProviderClient
    private var posActual : Location?= null

    //Actualizar posicion
    lateinit var locationRequest : LocationRequest
    lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallBack()

        auth = FirebaseAuth.getInstance()
        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        readLocations()
    }

    fun drawMarker(location : LatLng, description : String?, icon: Int){
        val addressMarker = mMap.addMarker(MarkerOptions().position(location).icon(bitmapDescriptorFromVector(this,
            icon)))!!
        if(description!=null){
            addressMarker.title=description
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
    }

    fun bitmapDescriptorFromVector(context : Context, vectorResId : Int) : BitmapDescriptor {
        val vectorDrawable : Drawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        val bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888);
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun createLocationRequest() : LocationRequest {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return locationRequest
    }


    fun createLocationCallBack(): LocationCallback {
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            val location = result.lastLocation
            if (location != null) {
                if (posActual == null) {
                    posActual = location
                } else {
                    if (distancia(LatLng(posActual!!.latitude, posActual!!.longitude), location) > 0.001) {
                        posActual = location
                    }
                }
                posActual = result.lastLocation!!
                userMarker?.remove()

                val userId = auth.currentUser?.uid ?: return
                val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
                database.get().addOnSuccessListener { dataSnapshot ->
                    val userName = dataSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val userLastname = dataSnapshot.child("lastname").getValue(String::class.java) ?: "User"
                    val fullName = "$userName $userLastname"
                    userMarker = mMap.addMarker(
                        MarkerOptions().position(LatLng(posActual!!.latitude, posActual!!.longitude))
                            .title(fullName)
                            .icon(bitmapDescriptorFromVector(this@HomeActivity, R.drawable.baseline_add_location_24))
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(posActual!!.latitude, posActual!!.longitude)))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))

                    database.child("latitud").setValue(posActual!!.latitude)
                    database.child("longitud").setValue(posActual!!.longitude)
                }.addOnFailureListener {
                    Toast.makeText(this@HomeActivity, "Failed to retrieve user name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    return locationCallback
}

    private fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Handle the exception
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun distancia( longpress : LatLng , actual : Location) : Float {
        val pk = (180f / Math.PI).toFloat()

        val a1: Double = longpress.latitude / pk
        val a2: Double = longpress.longitude / pk
        val b1: Double = actual.latitude / pk
        val b2: Double = actual.longitude / pk

        val t1 = cos(a1) * cos(a2) * cos(b1) * cos(b2)
        val t2 = cos(a1) * sin(a2) * cos(b1) * sin(b2)
        val t3 = sin(a1) * sin(b1)
        val tt = acos(t1 + t2 + t3)

        return (6366000 * tt).toFloat()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.homenu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.signOut -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                true
            }

            R.id.available -> {
                val userId = auth.currentUser?.uid
                if (userId != null) {

                    val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    database.child("available").setValue(true)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Available status updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
                        }
                }
                true
            }
            R.id.notAvailable -> {
                val userId = auth.currentUser?.uid
                if (userId != null) {

                    val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    database.child("available").setValue(false)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Available status updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
                        }
                }
                true
            }
            R.id.search -> {
                startActivity(Intent(this, ListUsersActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    }

    fun readLocations() {
        val json_string = this.assets.open("locations.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json_string)
        val jsonArray = jsonObject.getJSONArray("locationsArray")
        for (i in 0 until jsonArray.length()) {
            val locationObject = jsonArray.getJSONObject(i)
            val latitude = locationObject.getDouble("latitude")
            val longitude = locationObject.getDouble("longitude")
            val name = locationObject.getString("name")

            Log.i("Location", "Location: $name, $latitude, $longitude")
            val latLng = LatLng(latitude, longitude)
            drawMarker(latLng, name, R.drawable.baseline_location_pin_24)
        }
    }

}