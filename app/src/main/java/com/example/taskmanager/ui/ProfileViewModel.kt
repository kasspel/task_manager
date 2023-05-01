package com.example.taskmanager.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskmanager.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import org.json.JSONObject
import java.io.File

class ProfileViewModel: ViewModel() {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth
    private val storage: FirebaseStorage = Firebase.storage

    private var avatarUri: String? = ""
    private lateinit var storageRefAvatars: StorageReference

    private val _showLoadingAvatar = MutableLiveData<Boolean>().apply { value = false }
    private val _currentAvatar = MutableLiveData<String>().apply { value = "" }
    private val _name = MutableLiveData<String>().apply { value = "" }
    private val _phone = MutableLiveData<String>().apply { value = "" }
    private val _email = MutableLiveData<String>().apply { value = "" }
    private val _persentLoadingAvatar = MutableLiveData<Double>().apply { value = 0.0 }

    init {
        if(auth.currentUser != null){
            storageRefAvatars = storage.reference.child("users/avatars/" + auth.currentUser?.uid)
        }

    }
    fun saveAvatar(filepath: String){
        val filename = File(filepath).name
        val spaceRef = storageRefAvatars.child(filename)
        val path = spaceRef.path
        val name = spaceRef.name
        val file = Uri.fromFile(File(filepath))
        val uploadTask = spaceRef.putFile(file)
        _showLoadingAvatar.value = true
        uploadTask.addOnFailureListener {
            _showLoadingAvatar.value = false
        }.addOnSuccessListener { taskSnapshot ->
            val totalBites = taskSnapshot.totalByteCount.toDouble()
            val currentBites = taskSnapshot.bytesTransferred.toDouble()
            val persentLoading = currentBites / totalBites * 100.0
            _persentLoadingAvatar.value = persentLoading
        }
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            spaceRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                //avatarUri = "https://" + downloadUri?.host + downloadUri?.path
                avatarUri = downloadUri.toString()
                _showLoadingAvatar.value = false
            } else {
                _showLoadingAvatar.value = false
            }
        }

    }
    private val _progress = MutableLiveData<Boolean>().apply { value = false }
    val name: LiveData<String> = _name
    val email: LiveData<String> = _email
    val phone: LiveData<String> = _phone
    val persentLoadingAvatar: LiveData<Double> = _persentLoadingAvatar
    val showLoadingAvatar: LiveData<Boolean> = _showLoadingAvatar
    val progress: LiveData<Boolean> = _progress
    val currentAvatar: LiveData<String> = _currentAvatar

    fun save(user: User) {
        if(auth.currentUser == null) return
        val uid = auth.currentUser!!.uid
        _progress.value = true
        val pathAvatar = if(avatarUri == null || avatarUri!!.isEmpty()) "" else avatarUri
        val data = hashMapOf(
            "name" to user.name,
            "new_email" to user.email,
            "phone" to user.phone,
        )
        if(!pathAvatar!!.isEmpty()) data.put("photo", pathAvatar)
        db.collection("users")
            .document(uid)
            .update(data as Map<String, Any>)
            .addOnSuccessListener { documentReference ->
                _progress.value = false
            }
            .addOnFailureListener { e ->
                _progress.value = false
            }
    }
}