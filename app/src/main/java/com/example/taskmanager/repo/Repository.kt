package com.example.taskmanager.repo

import android.util.Log
import com.example.taskmanager.models.Project
import com.example.taskmanager.models.Task
import com.example.taskmanager.models.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

//Общие переменные проекта
class Repository {
    companion object {
        //авторизованный пользователь
        var user: User? = null

        //список участников проекта
        val usersList = ArrayList<User>()

        //список выбранных пользователей
        val selectedUsersList = ArrayList<User>()

        //текущий проект
        var currentProject: Project? = null

        //текущая личная цель
        var currentPersonalProject: Project? = null

        //текущая задача в проекте
        var currentTask: Task? = null

        //текущая задача в личной цели
        var currentPersonalTask: Task? = null

        //список проектов
        val currentProjectList = ArrayList<Project>()

        //список личных целей
        val currentPersonalProjectList = ArrayList<Project>()
        var userInfo: User? = null

        //ответственный в проекте
        var curentLead: User? = null

        //показывать архивные проекты и личные цели
        var isArchive = false

        //получить список пользователей
        fun getUsersList() {
            val db = Firebase.firestore
            val docRef = db.collection("users").get().addOnCompleteListener {
                val data = it.result
                if (data != null) {
                    val documents = data.documents
                    usersList.clear()
                    for (doc in documents) {
                        val uid = doc.id
                        val newEmail = doc["new_email"] as String? ?: ""
                        val email = doc["email"] as String? ?: ""
                        val name = doc["name"] as String? ?: ""
                        val phone = doc["phone"] as String? ?: ""
                        val photo = doc["photo"] as String? ?: ""
                        val fcm_token = doc["fcm_token"] as String? ?: ""
                        val realEmail = if (newEmail.isNotEmpty()) newEmail else email
                        val user = User(
                            uid = uid,
                            name = name,
                            email = realEmail,
                            phone = phone,
                            photo = photo,
                            fcm_token = fcm_token,
                        )
                        usersList.add(user)
                    }
                }
            }
        }

        //получить информацию о авторизованном пользователе
        fun getUserInfo() {
            val auth = Firebase.auth
            val uid = auth.currentUser?.uid ?: return
            val email = auth.currentUser?.email ?: return
            val db = Firebase.firestore
            val docRef = db.collection("users").document(uid)
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TAG", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d("TAG", "Current data: ${snapshot.data}")
                    val data = snapshot.data ?: return@addSnapshotListener
                    val newEmail = data["new_email"] as String? ?: ""
                    val name = data["name"] as String? ?: ""
                    val phone = data["phone"] as String? ?: ""
                    val photo = data["photo"] as String? ?: ""
                    val fcm_token_db = data["fcm_token"] as String? ?: ""
                    val realEmail = if (newEmail.isNotEmpty()) newEmail else email
                    user = User(
                        uid = uid,
                        name = name,
                        email = realEmail,
                        phone = phone,
                        photo = photo,
                        fcm_token = fcm_token_db,
                    )

                } else {
                    Log.d("TAG", "Current data: null")
                }
            }
        }
    }
}