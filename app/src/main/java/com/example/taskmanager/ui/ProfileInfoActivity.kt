package com.example.taskmanager.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.taskmanager.R
import com.example.taskmanager.repo.Repository
import com.squareup.picasso.Picasso

//информация о пользователе
class ProfileInfoActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private val user = Repository.userInfo
    //фото
    private lateinit var photoView: ImageView
    //имя
    private lateinit var nameView: TextView
    //почта
    private lateinit var emailView: TextView
    //телефон
    private lateinit var phoneView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_info)
        if(user == null){
            finish()
            return
        }
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        photoView = findViewById(R.id.photo)
        nameView = findViewById(R.id.name)
        emailView = findViewById(R.id.email)
        phoneView = findViewById(R.id.phone)
        if(user.photo.isNotEmpty()) Picasso.get().load(user.photo).into(photoView)
        nameView.text = user.name
        emailView.text = user.email
        phoneView.text = user.phone


    }
    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}