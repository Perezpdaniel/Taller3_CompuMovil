package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller3.databinding.ActivityUserLocationBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.launch
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class UserLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityUserLocationBinding
    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private var userMarker: Marker? = null
    private var actualMarker: Marker? = null
    private var userId: String? = null
    private var username: String? = null
    lateinit var locationClient: FusedLocationProviderClient
    private var posActual : Location?= null
    private var posUsuario : LatLng? = null
    lateinit var locationRequest : LocationRequest
    lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userId = intent.getStringExtra("USER_ID")
        username = intent.getStringExtra("USER_NAME")
        database = FirebaseDatabase.getInstance().getReference("users").child(userId!!)
        binding.distancia.setText("$username : 0.0 m")

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallBack()
        startLocationUpdates()

        Log.i("User_ID","$userId")
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        trackUserLocation()
    }

    private fun trackUserLocation() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java) ?: return
                val lng = snapshot.child("longitud").getValue(Double::class.java) ?: return
                val userLocation = LatLng(lat, lng)
                posUsuario = LatLng(lat,lng)

                if (userMarker == null) {
                    userMarker = mMap.addMarker(MarkerOptions().position(userLocation).title("User Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } else {
                    userMarker!!.position = userLocation
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
                UpdateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error getting data", error.toException())
            }
        })
    }

    fun createLocationCallBack() : LocationCallback{
        val locationCallback = object : LocationCallback(){
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val location= result.lastLocation
                if(location!=null){
                    if(posActual==null){
                        posActual=location
                    }else{
                        if(distancia(LatLng(posActual!!.latitude,posActual!!.longitude), location)>0.05){
                            posActual=location
                        }
                    }
                    UpdateUI()
                    if (actualMarker == null) {
                        actualMarker = mMap.addMarker(MarkerOptions().position(LatLng(posActual!!.latitude,posActual!!.longitude)).title("Posicion Actual"))
                        actualMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    } else {
                        actualMarker!!.position = LatLng(posActual!!.latitude,posActual!!.longitude)
                    }

                }
            }
        }
        return locationCallback
    }

    fun createLocationRequest() : LocationRequest {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return locationRequest
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

    fun UpdateUI(){
        if(posActual!=null && posUsuario!=null){
            val dist = distancia(posUsuario!!,posActual!!)
            binding.distancia.setText("$username : $dist m")
        }
    }
}