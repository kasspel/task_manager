package com.example.taskmanager.ui

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.taskmanager.R
import com.example.taskmanager.models.Project
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.repo.Util
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

//экран информации о личной цели
class InfoPersonalProjectActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var currentPersonalProject: Project
    //кнопка выбора даты начала личной цели
    private lateinit var selectStartDate: Button
    //кнопка выбора даты окончания личной цели
    private lateinit var selectEndDate: Button
    //кнопка сохранения формы
    private lateinit var saveView: Button
    //поле ввода названия личной цели
    private lateinit var nameView: EditText
    //поле ввода описания личной цели
    private lateinit var descriptionView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_personal_project)
        if (Repository.currentPersonalProject == null) {
            finish()
            return
        }
        currentPersonalProject = Repository.currentPersonalProject ?: return
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.subtitle = currentPersonalProject.name

        selectStartDate = findViewById(R.id.pick_start_date)
        selectEndDate = findViewById(R.id.pick_end_date)
        saveView = findViewById(R.id.save)
        nameView = findViewById(R.id.name)
        descriptionView = findViewById(R.id.description)

        if(Repository.isArchive){
            selectEndDate.isEnabled = false
            selectStartDate.isEnabled = false
            saveView.visibility = View.GONE
            nameView.isEnabled = false
            descriptionView.isEnabled = false
        }
        //настройка интерфейса
        setupUi()
        //сохранить изменения с предварительной проверкой
        saveView.setOnClickListener {
            updateWithValidate()
        }

        //выбрать дату начала личной цели
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
        //выбрать дату окончания личной цели
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
    }
    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
    //начальная настройка интерфейса
    private fun setupUi() {
        nameView.setText(Repository.currentPersonalProject?.name ?: "")
        descriptionView.setText(Repository.currentPersonalProject?.desc ?: "")
        selectStartDate.text = Repository.currentPersonalProject?.startDate ?: ""
        selectEndDate.text = Repository.currentPersonalProject?.endDate ?: ""
    }
    //сохранить форму в базе данных с предварительной проверкой
    private fun updateWithValidate() {
        val name = nameView.text.toString().trim()
        val desc = descriptionView.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Заполните название проекта", Toast.LENGTH_SHORT).show()
            return
        }
        if (desc.isEmpty()) {
            Toast.makeText(this, "Заполните описание проекта", Toast.LENGTH_SHORT).show()
            return
        }
        val oid = Firebase.auth.currentUser?.uid ?: return
        val pid = Repository.currentPersonalProject?.pid ?: return
        val c = Calendar.getInstance()
        val timestamp = c.timeInMillis
        val project = Project(
            oid = oid,
            name = name,
            desc = desc,
            startDate = selectStartDate.text.toString(),
            endDate = selectEndDate.text.toString(),
            timestamp = timestamp,
            pid = pid,
        )
        saveProject(project)
    }

    private fun saveProject(project: Project){
        val pid = project.pid
        if(pid.isEmpty()){
            Toast.makeText(this, "Не удалось обновить проект", Toast.LENGTH_SHORT).show()
            return
        }
        val db = Firebase.firestore
        val data = hashMapOf(
            "name" to project.name,
            "desc" to project.desc,
            "start_date" to project.startDate,
            "end_date" to project.endDate,
            "timestamp" to project.timestamp,
            "is_archive" to false,
            "is_personal" to true,
        )
        db.collection("projects").document(pid).update(data as Map<String, Any>).addOnSuccessListener {
            Util.setAlarmDedline(this,project.endDate, pid, project.name)
            Toast.makeText(this, "Проект обновлен", Toast.LENGTH_SHORT).show()
            finish()
        }

    }


}