package com.example.taller3.model

import android.net.Uri
import java.io.Serializable

class MyUser : Serializable {
    var name: String = ""
    var lastname: String = ""
    var email: String = ""
    var password: String = ""
    var image: Uri? = null
    var id: String = ""
    var latitud: Double = 0.0
    var longitud: Double = 0.0

    constructor()

    constructor(
        name: String,
        lastname: String,
        email: String,
        password: String,
        image: Uri?,
        id: String,
        latitud: Double,
        longitud: Double
    ) {
        this.name = name
        this.lastname = lastname
        this.email = email
        this.password = password
        this.image = image
        this.id = id
        this.latitud = latitud
        this.longitud = longitud
    }
}