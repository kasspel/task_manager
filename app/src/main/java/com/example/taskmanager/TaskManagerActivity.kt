package com.example.taskmanager

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.screens.AuthActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class TaskManagerActivity : AppCompatActivity() {
    //сcылка на авторизацию
    private lateinit var auth: FirebaseAuth

    //переменные для привязки к элементам макета
    lateinit var navController: NavController
    lateinit var toolbar: Toolbar
    private lateinit var slider: NavigationView
    private lateinit var root: DrawerLayout

    fun setTitle(title: String){
        toolbar.title = title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_manager)

        Repository.getUserInfo()
        Repository.getUsersList()
        //задать переменные навигации и заголовка
        navController = findNavController(R.id.navHostFragment)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        //настроить интерфейс
        setupUi()
        //настроить контроллер навигации
        setupNavController()
        //настроить боковое меню
        setupDrawer()

        //получить сылку на авторизацию
        auth = Firebase.auth
        //если пользователь вышел из системы перейти на экран входа
        auth.addAuthStateListener {
            if(auth.currentUser == null){
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }

    }

    //привязать переменные нижнего и бокового меню к элементам макета
    private fun setupUi(){
        root = findViewById(R.id.mainRoot)
        slider = findViewById(R.id.slider)
    }
    //настройка контроллера навигации и привязка его к заголовку приложения
    private fun setupNavController(){
        supportActionBar?.setHomeButtonEnabled(true)
        navController.addOnDestinationChangedListener { _, d, _ ->
            supportActionBar?.title = d.label
        }
    }
    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {

                if (!root.isDrawerOpen(slider)) {
                    root.openDrawer(slider)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
    //обработка нажатий на элеенты бокового меню с переходом на соответствующие экраны
    private fun setupDrawer(){
        slider.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.projectsFragment -> {
                    Repository.isArchive = false
                    goProjects()
                }
                R.id.archiveFragment -> {
                    Repository.isArchive = true
                    goProjects()
                    setTitle("Архив проектов")
                }
                R.id.profileFragment -> goProfile()
                R.id.aboutUsFragment -> goAbout()
                R.id.exit -> goExit()
            }
            return@setNavigationItemSelectedListener true
        }
    }
    //скрыть боковое меню
    private fun closeDrawer(){
        if (root.isDrawerOpen(slider)) {
            root.closeDrawer(slider)
        }
    }
    private fun goProjects(){
        navController.navigate(R.id.projectsFragment)
        closeDrawer()
    }
    private fun goArchive(){
        navController.navigate(R.id.archiveFragment)
        closeDrawer()
    }
    private fun goProfile(){
        navController.navigate(R.id.profileFragment)
        closeDrawer()
    }
    private fun goAbout(){
        navController.navigate(R.id.aboutUsFragment)
        closeDrawer()
    }
    private fun goExit(){
        auth.signOut()
    }
}