package com.example.taller2_danielperez.model

import org.json.JSONObject
import java.util.Date

class MyLocation(val name : String, val latitude: Double,
                 val longitude: Double){
    fun toJSON() : JSONObject {
        val obj = JSONObject();
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("name", name)
        return obj
    }
}