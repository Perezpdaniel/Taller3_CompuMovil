package com.example.taller3.model

import android.net.Uri
import java.io.Serializable

class MyUser : Serializable {
    var name: String = ""
    var lastname: String = ""
    var email: String = ""
    var password: String = ""
    var image: String? = null
    var id: String = ""
    var latitud: Double = 0.0
    var longitud: Double = 0.0
    var available: Boolean = false

    constructor()

    constructor(
        name: String,
        lastname: String,
        email: String,
        password: String,
        image: String?,
        id: String,
        latitud: Double,
        longitud: Double,
        available: Boolean
    ) {
        this.name = name
        this.lastname = lastname
        this.email = email
        this.password = password
        this.image = image
        this.id = id
        this.latitud = latitud
        this.longitud = longitud
        this.available = available
    }
}