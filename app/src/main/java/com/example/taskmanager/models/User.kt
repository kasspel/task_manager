package com.example.taskmanager.models

data class User(
    val uid: String,
    val name:String,
    val email:String,
    val phone:String,
    val photo:String,
    val mid: String = "", //что это?
    var fcm_token: String, //где берется этот токен?
)