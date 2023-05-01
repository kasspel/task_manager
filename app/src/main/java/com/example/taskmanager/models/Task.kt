package com.example.taskmanager.models

data class Task(
    val pid: String,//project id
    val oid: String,//owner id создатель
    val name: String,
    val desc: String,
    val leadName: String,
    val leadPhoto: String,
    val leadUid: String,//user id Lead?
    val leadMid: String,//member in project id
    val leadFcm: String,
    val leadEmail: String,
    val leadPhone: String,
    val dateOfEnd: String,
    val dateOfEndTimestamp: Long,
    val fileName: String,
    val filePath: String,
    val isSubTask: Boolean,
    val timestampTask: Long, //время создания задачи
    var status: Long,
    val tid: String, //task id
    var resultFileName:String,
    var resultFilePath:String,
)
