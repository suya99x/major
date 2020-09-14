package com.example.trafficcongestion.modal

import com.google.firebase.database.Exclude
import java.util.*
import kotlin.collections.HashMap


class User {
    var Name: String? = null
    var Email: String? = null
    var Phone: String?=null

    constructor() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    constructor(name: String?, email: String?, phone: String?) {
        this.Name = name
        this.Email = email
        this.Phone=phone
    }
}

data class Predicated(
     var Count:Double? = 0.0,
     var Time:Int?=0
)

/*class Entry{
    var xaxis:Int?=0
    var yaxis:Float?=null

    constructor() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }
    constructor(xaxis: Int?, yaxis: Float?) {
        this.xaxis = xaxis
        this.yaxis = yaxis
    }

    fun getxvalue(): Int? {
        return xaxis
    }
    fun getyvalue(): Float? {
        return yaxis
    }

}*/
