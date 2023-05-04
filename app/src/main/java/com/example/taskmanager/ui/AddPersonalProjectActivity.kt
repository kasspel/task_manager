package com.example.taskmanager.ui

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
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

//экран добавления личной цели
class AddPersonalProjectActivity : AppCompatActivity() {
    //тулбар
    private lateinit var toolbar: Toolbar
    //кнопка выбора начальной даты личной цели
    private lateinit var selectStartDate: Button
    //кнопка выбора конечной даты личной цели
    private lateinit var selectEndDate: Button
    //кнопка сохранения данных формы
    private lateinit var saveView: Button
    //название личной цели
    private lateinit var nameView: EditText
    //описание личной цели
    private lateinit var descriptionView: EditText

    //сохрание данных формы в базе данных с предварительной проверкой
    private fun saveWithValidate(){
        val name = nameView.text.toString().trim()
        val desc = descriptionView.text.toString().trim()
        if(name.isEmpty()){
            Toast.makeText(this, "Заполните название проекта", Toast.LENGTH_SHORT).show()
            return
        }
        if(desc.isEmpty()){
            Toast.makeText(this, "Заполните описание проекта", Toast.LENGTH_SHORT).show()
            return
        }
        val oid = Firebase.auth.currentUser?.uid ?: return
        val c = Calendar.getInstance()
        val timestamp = c.timeInMillis
        val project = Project(
            oid = oid, //ид создателя личной цели
            name = name, //название личной цели
            desc = desc, //описание личной цели
            startDate = selectStartDate.text.toString(),//дата начала личной цели
            endDate = selectEndDate.text.toString(),//дата окончания личной цели
            timestamp = timestamp,//время создания личной цели
        )
        saveProject(project)
    }
//сохранение в базе данных
    private fun saveProject(project: Project){
        val db = Firebase.firestore
        val data = hashMapOf(
            "oid" to project.oid,
            "name" to project.name,
            "desc" to project.desc,
            "start_date" to project.startDate,
            "end_date" to project.endDate,
            "timestamp" to project.timestamp,
            "is_archive" to false,
            "is_personal" to true,
        )
        val project_id = db.collection("projects").document().id
        db.collection("projects").document(project_id).set(data).addOnSuccessListener {
            Toast.makeText(this, "Личная цель сохранена", Toast.LENGTH_SHORT).show()
            Util.setAlarmDedline(this, project.endDate, project_id, project.name)
            Util.setAlarmDedline5(this, project.endDate, project_id, project.name)
            Repository.selectedUsersList.clear()
            finish()

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_personal_project)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        selectStartDate = findViewById(R.id.pick_start_date)
        selectEndDate = findViewById(R.id.pick_end_date)
        saveView = findViewById(R.id.save)
        nameView = findViewById(R.id.name)
        descriptionView = findViewById(R.id.description)
        //задать подписи на кнопка задания дат
        setupDate()

        //обработка нажатия на кнопку сохранить
        saveView.setOnClickListener {
            saveWithValidate()
        }
        //выбор начальной даты
        selectStartDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
                val m = monthOfYear + 1
                var monthSt = m.toString()
                if(m < 10) monthSt = "0" + monthSt
                val text = "" + dayOfMonth + "." + monthSt + "." + year
                selectStartDate.setText(text)
            }, year, month, day)
            dpd.show()

        }
        //выбор конечной даты
        selectEndDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
                val m = monthOfYear + 1
                var monthSt = m.toString()
                if(m < 10) monthSt = "0" + monthSt
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

    private fun setupDate(){
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH) + 1
        var monthSt = month.toString()
        if(month < 10) monthSt = "0" + monthSt
        val day = c.get(Calendar.DAY_OF_MONTH)
        val text = "" + day + "." + monthSt + "." + year
        selectStartDate.text = text
        selectEndDate.text = text
    }

}