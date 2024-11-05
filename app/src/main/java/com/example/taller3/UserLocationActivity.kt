package com.example.taller3

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.launch

class UserLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityUserLocationBinding
    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private var userMarker: Marker? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userId = intent.getStringExtra("USER_ID")
        database = FirebaseDatabase.getInstance().getReference("users").child(userId!!)
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
                Log.i("USER_LOCATION", userLocation.toString())
                if (userMarker == null) {
                    userMarker = mMap.addMarker(MarkerOptions().position(userLocation).title("User Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } else {
                    userMarker!!.position = userLocation
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error getting data", error.toException())
            }
        })
    }
}