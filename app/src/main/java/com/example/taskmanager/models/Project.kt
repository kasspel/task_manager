package com.example.taskmanager.models

data class Project(
    val oid: String, //owner id
    val name: String,
    val desc: String,
    val startDate: String,
    val endDate: String,
    val timestamp: Long, //время создания проекта
    val pid: String = "",
)