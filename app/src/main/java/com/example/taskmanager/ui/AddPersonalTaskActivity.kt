package com.example.taskmanager.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.example.taskmanager.R
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.repo.Util
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.*

//экран/форма для создания задачи в личной цели
class AddPersonalTaskActivity : AppCompatActivity() {
    val RESULT_SELECT_FILE = 1134

    //тулбар
    private lateinit var toolbar: Toolbar

    //название задачи
    private lateinit var nameTaskView: EditText
    //описание задачи
    private lateinit var descTaskView: EditText
    //начало задачи
    private lateinit var selectDateView: Button
    //переключатель подзадачи
    private lateinit var swithSubTaskView: Switch
    //кнопка сохранить
    private lateinit var saveButtonView: Button
    //индикатор сохранения
    private lateinit var progressView: ProgressBar
    //значек прикрепленного файла
    private lateinit var attachFileView: ImageView
    //имя приклепленного файла
    private lateinit var fileNameView: TextView
    //кнопка прикрепить файла
    private lateinit var attachFileButton: Button

    //имя файла
    private var fileName = ""
    //путь к файлу в облаке
    private var filePath = ""
    private var selectDateTimestamp: Long = 0L

    private lateinit var storageRefFiles: StorageReference
    //сохранить с проверкой
    private fun saveWithValidate(){
        val nameTask = nameTaskView.text.toString()
        val descTask = descTaskView.text.toString()
        val date = selectDateView.text
        val timestampSD = selectDateTimestamp
        val filename = fileName
        val filepath = filePath
        val isSubTask = swithSubTaskView.isChecked
        val user = Repository.user
        val c = Calendar.getInstance()
        val timestamp = c.timeInMillis
        val pid = Repository.currentPersonalProject?.pid ?: return
        val oid = Repository.currentPersonalProject?.oid ?: return
        if(user == null) {
            Toast.makeText(this, "Не удалось сохранить (нет пользователя)", Toast.LENGTH_SHORT).show()
            return
        } else if(nameTask.isEmpty()){
            Toast.makeText(this, "Задайте имя задачи", Toast.LENGTH_SHORT).show()
            return
        } else if(descTask.isEmpty()){
            Toast.makeText(this, "Задайте описание задачи", Toast.LENGTH_SHORT).show()
            return
        } else {
            val db = Firebase.firestore
            val data = hashMapOf(
                "pid" to pid,
                "oid" to oid,
                "name" to nameTask,
                "desc" to descTask,
                "lead_name" to user.name,
                "lead_photo" to user.photo,
                "lead_uid" to user.uid,
                "lead_mid" to user.mid,
                "lead_fcm" to user.fcm_token,
                "lead_email" to user.email,
                "lead_phone" to user.phone,
                "date_of_end" to date,
                "date_of_end_timestamp" to timestampSD,
                "file_name" to filename,
                "file_path" to filepath,
                "is_subtask" to isSubTask,
                "status" to 0L,
                "timestamp" to timestamp,
            )
            progressView.visibility = View.VISIBLE
            saveButtonView.isEnabled = false
            db.collection("tasks").document().set(data).addOnFailureListener {
                progressView.visibility = View.GONE
                saveButtonView.isEnabled = true
                Toast.makeText(this,"Не удалось добавить задачу", Toast.LENGTH_SHORT).show()
            }.addOnSuccessListener {
                progressView.visibility = View.GONE
                Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
                Repository.curentLead = null
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_personal_task)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        Repository.currentPersonalProject ?: finish()
        val projectName = Repository.currentPersonalProject?.name ?: ""
        toolbar.subtitle = projectName

        nameTaskView = findViewById(R.id.name)
        descTaskView = findViewById(R.id.description)
        selectDateView = findViewById(R.id.select_end_date)
        swithSubTaskView = findViewById(R.id.subtask)
        saveButtonView = findViewById(R.id.save)
        progressView = findViewById(R.id.progress)
        attachFileView = findViewById(R.id.attach_file)
        fileNameView = findViewById(R.id.filename)
        attachFileButton = findViewById(R.id.attach_file_btn)
        progressView.visibility = View.GONE

        //обновить интерфейс
        updateUi()
        //задать дату окончания задачи
        setupDate()
        selectDateView.setOnClickListener {
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
                selectDateView.setText(text)
                setTimeToNextDay(year, monthOfYear, dayOfMonth)
            }, year, month, day)
            dpd.show()
        }
        val storage = Firebase.storage
        storageRefFiles = storage.reference.child("tasks/files/" + Repository.user?.uid)
        //выбрать файл и загрузить его в облако
        attachFileButton.setOnClickListener {
            attachFile()
        }

        //сохранить форму в бд
        saveButtonView.setOnClickListener {
            saveWithValidate()
        }

    }
    //загрузить выбранный файл в облако
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_SELECT_FILE && resultCode == Activity.RESULT_OK && null != data) {
            val uri: Uri = data?.data ?: return
            
            val filename = Util.getFilenameFromUri(this, uri)
            val spaceRef = storageRefFiles.child(filename)
            val uploadTask = spaceRef.putFile(uri)
            progressView.visibility = View.VISIBLE

            uploadTask.addOnFailureListener {
                progressView.visibility = View.GONE
                Toast.makeText(this,"Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
            }
            val tastUrl = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        Toast.makeText(this,"Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                        progressView.visibility = View.GONE
                    }
                }
                spaceRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    filePath = downloadUri.toString()
                    Toast.makeText(this,"Файл загружен", Toast.LENGTH_SHORT).show()
                    progressView.visibility = View.GONE
                    attachFileButton.visibility = View.GONE
                    fileNameView.text = filename
                    fileName = filename
                    attachFileView.visibility = View.VISIBLE
                    fileNameView.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this,"Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                    progressView.visibility = View.GONE
                }
            }
        }

        updateUi()
    }
    //вызвать стандартную активити выбора файла
    private fun attachFile(){
        Dexter.withContext(this)
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }


                    startActivityForResult(intent, RESULT_SELECT_FILE)
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {}

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {}
            }).check()
    }
    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    //обновить интерфейс
    private fun updateUi(){
        if(fileName.isEmpty()){
            attachFileView.visibility = View.GONE
            fileNameView.visibility = View.GONE
            attachFileButton.visibility = View.VISIBLE
        } else {
            attachFileView.visibility = View.VISIBLE
            fileNameView.visibility = View.VISIBLE
            attachFileButton.visibility = View.GONE
            fileNameView.text = fileName
        }
    }

    //задать начальную установку даты окончания задачи
    private fun setupDate(){
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH) + 1
        var monthSt = month.toString()
        if(month < 10) monthSt = "0" + monthSt
        val day = c.get(Calendar.DAY_OF_MONTH)
        val text = "" + day + "." + monthSt + "." + year
        selectDateView.text = text
        selectDateTimestamp = c.timeInMillis
    }

    private fun setTimeToNextDay(year: Int, month: Int, day: Int){
        val currentDate = Calendar.getInstance()
        currentDate[year, month] = day
        currentDate.add(Calendar.DAY_OF_MONTH, 1)
        selectDateTimestamp = currentDate.timeInMillis
    }


}

