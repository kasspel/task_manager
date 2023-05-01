package com.example.taskmanager.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.taskmanager.R
import com.example.taskmanager.TimeNotification
import com.example.taskmanager.models.Project
import com.example.taskmanager.repo.Repository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

//форма добавления проекта
class AddProjectActivity : AppCompatActivity() {
    //тулбар
    private lateinit var toolbar: Toolbar

    //кнопка выбора начала прокта
    private lateinit var selectStartDate: Button

    //кнопка выбора окончания проекта
    private lateinit var selectEndDate: Button

    //кнопка выбора участников проекта
    private lateinit var membersListView: Button

    //кнопка сохранения формы в базе данных
    private lateinit var saveView: Button

    //название проекта
    private lateinit var nameView: EditText

    //описание проекта
    private lateinit var descriptionView: EditText

    //индикатор сохранения
    private lateinit var progressView: ProgressBar

    //сохранить данные формы в базе данных с предварительной проверкой
    private fun saveWithValidate() {
        val name = nameView.text.toString().trim()
        val desc = descriptionView.text.toString().trim()
        val membersList = Repository.selectedUsersList
        if (name.isEmpty()) {
            Toast.makeText(this, "Заполните название проекта", Toast.LENGTH_SHORT).show()
            return
        }
        if (desc.isEmpty()) {
            Toast.makeText(this, "Заполните описание проекта", Toast.LENGTH_SHORT).show()
            return
        }
        if (membersList.size == 0) {
            Toast.makeText(this, "Добавьте участников проекта", Toast.LENGTH_SHORT).show()
            return
        }
        val oid = Firebase.auth.currentUser?.uid ?: return
        val c = Calendar.getInstance()
        val timestamp = c.timeInMillis
        val project = Project(
            oid = oid,
            name = name,
            desc = desc,
            startDate = selectStartDate.text.toString(),
            endDate = selectEndDate.text.toString(),
            timestamp = timestamp,
        )
        progressView.visibility = View.VISIBLE
        saveView.isEnabled = false
        saveProject(project)
    }

    //сохранить форму в базе данных
    private fun saveProject(project: Project) {
        val db = Firebase.firestore
        val data = hashMapOf(
            "oid" to project.oid,
            "name" to project.name,
            "desc" to project.desc,
            "start_date" to project.startDate,
            "end_date" to project.endDate,
            "timestamp" to project.timestamp,
            "is_archive" to false,

            )
        val project_id = db.collection("projects").document().id
        db.collection("projects").document(project_id).set(data).addOnSuccessListener {
            //add owner
            val owner = Repository.user!!
            Repository.selectedUsersList.add(owner)

            //add members
            for (user in Repository.selectedUsersList) {
                val memberData = hashMapOf(
                    "project_id" to project_id,
                    "project_oid" to project.oid,
                    "project_name" to project.name,
                    "project_desc" to project.desc,
                    "project_start_date" to project.startDate,
                    "project_end_date" to project.endDate,
                    "project_timestamp" to project.timestamp,
                    "user_uid" to user.uid,
                    "user_name" to user.name,
                    "user_email" to user.email,
                    "user_phone" to user.phone,
                    "user_photo" to user.photo,
                    "user_fcm_token" to user.fcm_token,
                    "is_archive" to false,
                )
                db.collection("members").document().set(memberData)
                setAlarmDedline(project.endDate, project_id, project.name)
            }
            Toast.makeText(this, "Проект сохранен", Toast.LENGTH_SHORT).show()
            Repository.selectedUsersList.clear()
            saveView.isEnabled = true
            progressView.visibility = View.GONE
            finish()
        }
    }

    private fun setAlarmDedline(date: String, pid: String, name: String){
        val dt = date.split(".")
        val day = dt[0].toInt() - 1
        val month = dt[1].toInt() - 1
        val year = dt[2].toInt()

        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month)
        c.set(Calendar.DAY_OF_MONTH, day)
        val _hour = c.get(Calendar.HOUR_OF_DAY)
        val _minute = c.get(Calendar.MINUTE)
        val _second = c.get(Calendar.SECOND)

        val endmils = c.timeInMillis
        val amils = endmils - 86340000L
        c.timeInMillis = amils
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH) + 1

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimeNotification::class.java)
        intent.putExtra("message","Через сутки время окончания проекта")
        intent.putExtra("title",name)
        intent.putExtra("pid", pid)
        intent.putExtra("uid", Repository.user?.uid ?: "")
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        am.cancel(pendingIntent)
        am.set(AlarmManager.RTC_WAKEUP, amils, pendingIntent)
        Log.d("anna", "test")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        selectStartDate = findViewById(R.id.pick_start_date)
        selectEndDate = findViewById(R.id.pick_end_date)
        membersListView = findViewById(R.id.members_list)
        saveView = findViewById(R.id.save)
        nameView = findViewById(R.id.name)
        descriptionView = findViewById(R.id.description)
        progressView = findViewById(R.id.progress)
        progressView.visibility = View.GONE
        setupDate()

        //обработчик нажатия на кнопку сохранения формы
        saveView.setOnClickListener {
            saveWithValidate()
        }
        //задать начало проекта
        selectStartDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
                val m = monthOfYear + 1
                var monthSt = m.toString()
                if (m < 10) monthSt = "0" + monthSt
                val text = "" + dayOfMonth + "." + monthSt + "." + year
                selectStartDate.setText(text)
            }, year, month, day)
            dpd.show()
        }
        //задать окончание проекта
        selectEndDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
                val m = monthOfYear + 1
                var monthSt = m.toString()
                if (m < 10) monthSt = "0" + monthSt
                val text = "" + dayOfMonth + "." + monthSt + "." + year
                selectEndDate.setText(text)
            }, year, month, day)
            dpd.show()
        }
        //показать экран выбора участников проекта
        membersListView.setOnClickListener {
            val intent = Intent(this, AddMembersActivity::class.java)
            startActivity(intent)
        }
    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        Repository.selectedUsersList.clear()
    }

    //метод задания начальной даты по умолчанию
    private fun setupDate() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH) + 1
        var monthSt = month.toString()
        if (month < 10) monthSt = "0" + monthSt
        val day = c.get(Calendar.DAY_OF_MONTH)
        val text = "" + day + "." + monthSt + "." + year
        selectStartDate.text = text
        selectEndDate.text = text
    }
}