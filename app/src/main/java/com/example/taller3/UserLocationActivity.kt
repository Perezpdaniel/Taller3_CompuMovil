package com.example.taller3

import android.os.Bundle
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

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        trackUserLocation()
    }

    private fun trackUserLocation() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java) ?: return
                val lng = snapshot.child("longitud").getValue(Double::class.java) ?: return
                val userLocation = LatLng(lat, lng)

                if (userMarker == null) {
                    userMarker = mMap.addMarker(MarkerOptions().position(userLocation).title("User Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } else {
                    userMarker!!.position = userLocation
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}