package com.example.trafficcongestion

import com.google.firebase.messaging.FirebaseMessagingService


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        println("NEW_TOKEN :::::::::::::::::::::::::: $s")
    }
}