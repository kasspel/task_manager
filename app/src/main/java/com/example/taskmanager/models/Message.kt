package com.example.taskmanager.models

data class Message(
    val pid: String,//id проекта в котором отправлено сообщение
    val uid: String,//id пользователя который отправил сообщение
    val name: String,//имя пользователя который отправил сообщение
    val photo: String, //фотография пользователя который отправил сообщение
    val timestamp: Long,//время отправки сообщения
    val text: String, //текст сообщения
    val file: String,//ссылка на файл прикрепленный к сообщению
    val filename: String,//имя файла
)
