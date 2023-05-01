package com.example.taskmanager.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.example.taskmanager.R
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.repo.Util
import com.google.firebase.auth.ktx.auth
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
import com.squareup.picasso.Picasso
import java.util.*

class TaskActivity : AppCompatActivity() {
    private val RESULT_SELECT_FILE = 7755
    private lateinit var toolbar: Toolbar

    private lateinit var nameTaskView: EditText
    private lateinit var descTaskView: EditText
    private lateinit var photoMemberView: ImageView
    private lateinit var nameMemberView: TextView
    private lateinit var selectDateView: Button
    private lateinit var saveButtonView: Button
    private lateinit var progressView: ProgressBar
    private lateinit var attachFileView: ImageView
    private lateinit var fileNameView: TextView
    private lateinit var fileContainer: View
    private lateinit var statusView: TextView

    private lateinit var statusContainerView: View
    private lateinit var startButtonView: Button
    private lateinit var putButtonView: Button
    private lateinit var returnButtonView: Button
    private lateinit var acceptButtonView: Button

    private lateinit var resultContainerView: View
    private lateinit var resultSendView: Button
    private lateinit var resultFileNameView: TextView
    private lateinit var resultImageView: View
    private lateinit var resultText: View

    private var fileName = ""
    private var filePath = ""
    private var selectDateTimestamp: Long = 0L

    private lateinit var storageRefFiles: StorageReference
    //установка интерфейса в зависимости от статуса участника
    private fun setupUiWithStats(){
        val userStatus = Util.getUserStatus()
        if(userStatus != 2){
            saveButtonView.visibility = View.GONE
            selectDateView.isEnabled = false
            nameTaskView.isEnabled = false
            descTaskView.isEnabled = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        Repository.currentProject ?: finish()
        val taskName = Repository.currentTask?.name ?: ""
        toolbar.subtitle = taskName
        Repository.currentTask ?: finish()

        statusContainerView = findViewById(R.id.status_container)
        startButtonView = findViewById(R.id.start_button)
        putButtonView = findViewById(R.id.put_button)
        returnButtonView = findViewById(R.id.return_button)
        acceptButtonView = findViewById(R.id.accept_button)

        resultContainerView = findViewById(R.id.result_container)
        resultFileNameView = findViewById(R.id.result_filename)
        resultImageView = findViewById(R.id.result_attach_file)
        resultSendView = findViewById(R.id.result_attach_file_btn)
        resultText = findViewById(R.id.text_result)

        nameTaskView = findViewById(R.id.name)
        descTaskView = findViewById(R.id.description)
        photoMemberView = findViewById(R.id.photo_member)
        nameMemberView = findViewById(R.id.name_member)
        selectDateView = findViewById(R.id.select_end_date)
        saveButtonView = findViewById(R.id.save)
        progressView = findViewById(R.id.progress)
        attachFileView = findViewById(R.id.attach_file)
        fileNameView = findViewById(R.id.filename)
        fileContainer = findViewById(R.id.file_container)
        statusView = findViewById(R.id.status)
        progressView.visibility = View.GONE

        val storage = Firebase.storage
        storageRefFiles = storage.reference.child("tasks/files/" + Repository.user?.uid) //результат задачи?

        updateUi()
        setupDate()
        setupStatusButton()
        setupResultButton()
        setupDownloadButtons()
        setupUiWithStats()
        updateResult()
        //выбор даты окончания задачи
        selectDateView.setOnClickListener {
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
                selectDateView.setText(text)
                setTimeToNextDay(year, monthOfYear, dayOfMonth)
            }, year, month, day)
            dpd.show()
        }

        saveButtonView.setOnClickListener {
            updateTask()
        }
    }
    private fun setTimeToNextDay(year: Int, month: Int, day: Int) {
        val currentDate = Calendar.getInstance()
        currentDate[year, month] = day
        //currentDate.add(Calendar.DAY_OF_MONTH, 1)
        currentDate.set(Calendar.HOUR_OF_DAY, 23)
        currentDate.set(Calendar.MINUTE, 59)
        currentDate.set(Calendar.SECOND, 59)
        selectDateTimestamp = currentDate.timeInMillis
    }
    private fun setupDate() {
//        val c = Calendar.getInstance()
//        val year = c.get(Calendar.YEAR)
//        val month = c.get(Calendar.MONTH) + 1
//        var monthSt = month.toString()
//        if (month < 10) monthSt = "0" + monthSt
//        val day = c.get(Calendar.DAY_OF_MONTH)
//        val text = "" + day + "." + monthSt + "." + year
//        selectDateView.text = text
//        selectDateTimestamp = c.timeInMillis
    }

    private fun updateTask(){
        val name = nameTaskView.text.toString()
        val desc = descTaskView.text.toString()
        val date = selectDateView.text
        val tid = Repository.currentTask?.tid ?: return
        val leadFcm = Repository.currentTask?.leadFcm ?: return
        progressView.visibility = View.VISIBLE
        saveButtonView.isEnabled = false
        val db = Firebase.firestore
        val data = hashMapOf(
            "name" to name,
            "desc" to desc,
            "date_of_end" to date,
            "date_of_end_timestamp" to selectDateTimestamp,
        )
        db.collection("tasks").document(tid).update(data)
            .addOnCompleteListener { result ->
                if(result.isSuccessful){
                    Toast.makeText(this,"Задача обновлена", Toast.LENGTH_SHORT).show()
                    Util.checkTaskStatusTimeout()
                    Util.setAlarmTaskDeadline(
                        context = this,
                        date = date.toString(),
                        name = name,
                        lead_fcm = leadFcm,
                    )

                    finish()
                } else {
                    Toast.makeText(this,"Не удалось обновить задачу", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUi(){
        val photo = Repository.currentTask?.leadPhoto ?: return
        val nameLead = Repository.currentTask?.leadName ?: return
        val name = Repository.currentTask?.name ?: return
        val desc = Repository.currentTask?.desc ?: return
        val date = Repository.currentTask?.dateOfEnd ?: return
        val filename = Repository.currentTask?.fileName ?: return
        fileName = filename
        filePath = Repository.currentTask?.filePath ?: return
        nameMemberView.text = nameLead
        if(photo.isNotEmpty()){
            Picasso.get().load(photo).into(photoMemberView)
        } else {
            photoMemberView.setImageResource(R.drawable.ic_profile)
        }
        nameTaskView.setText(name)
        descTaskView.setText(desc)
        selectDateView.text = date
        fileNameView.text = filename
        if(filename.isEmpty()){
            fileContainer.visibility = View.GONE
        } else {
            fileContainer.visibility = View.VISIBLE
        }
        updateStatus()
    }
    private fun updateStatus(){
        val oid = Repository.currentTask?.oid ?: return
        val uid = Repository.user?.uid ?: return
        val status = Repository.currentTask?.status ?: return
        statusView.text = Util.getTextStatus(status, oid, uid)
        Util.setTextStyle(status,oid,uid,statusView)
        when(Util.getUserStatus()){
            0 -> {
                statusContainerView.visibility = View.GONE
            }
            1 -> {
                statusContainerView.visibility = View.VISIBLE
                returnButtonView.visibility = View.GONE
                acceptButtonView.visibility = View.GONE
                when(status){
                    0L -> {
                      startButtonView.visibility = View.VISIBLE
                      putButtonView.visibility = View.GONE
                    }
                    1L -> {
                        startButtonView.visibility = View.GONE
                        putButtonView.visibility = View.VISIBLE
                    }
                    else -> {
                        statusContainerView.visibility = View.GONE
                    }
                }
            }
            2 -> {
                statusContainerView.visibility = View.VISIBLE
                startButtonView.visibility = View.GONE
                putButtonView.visibility = View.GONE
                when(status){
                    2L -> {
                        returnButtonView.visibility = View.VISIBLE
                        acceptButtonView.visibility = View.VISIBLE
                    }
                    else -> {
                        statusContainerView.visibility = View.GONE
                    }
                }

            }
        }
    }
    private fun setupStatusButton(){
        startButtonView.setOnClickListener {
            if(Repository.currentTask != null) {
                updateStatusDB(1L)
                updateResult()
            }
        }
        putButtonView.setOnClickListener {
            if(Repository.currentTask != null) {
                updateStatusDB(2L)
                updateResult()
            }
        }
        returnButtonView.setOnClickListener {
            if(Repository.currentTask != null) {
                val resultFileName = Repository.currentTask?.resultFileName ?: ""
                val resultFilePath = Repository.currentTask?.resultFilePath ?: ""
                if(resultFileName.isNotEmpty()){
                    Repository.currentTask!!.resultFileName = ""
                }
                if(resultFilePath.isNotEmpty()){
                    Repository.currentTask!!.resultFilePath = ""
                }
                updateStatusDB(1L)
                removeResultFromDB()
                updateResult()
            }
        }
        acceptButtonView.setOnClickListener {
            if(Repository.currentTask != null) {
                updateStatusDB(4L)
            }
        }
    }
    private fun removeResultFromDB(){
        val tid = Repository.currentTask?.tid ?: return
        val db = Firebase.firestore
        val dataToUpdate = hashMapOf(
            "result_file_name" to "",
            "result_file_path" to ""
        )
        db.collection("tasks").document(tid).update(dataToUpdate as Map<String, Any>)

    }
    private fun updateStatusDB(status: Long){
        Repository.currentTask?.status ?: return
        val tid = Repository.currentTask?.tid ?: return
        val db = Firebase.firestore
        val data = hashMapOf(
            "status" to status,
        )
        val oldStatus = Repository.currentTask?.status
        Repository.currentTask?.status = status
        progressView.visibility = View.VISIBLE
        db.collection("tasks").document(tid).update(data as Map<String, Any>)
            .addOnCompleteListener { result ->
                if(result.isSuccessful){
                    Repository.currentTask?.status = status
                    updateStatus()
                    progressView.visibility = View.GONE
                    Toast.makeText(this, "Статус задачи обновлен", Toast.LENGTH_SHORT).show()
                } else {
                    Repository.currentTask?.status = oldStatus!!
                    progressView.visibility = View.GONE
                    val error = result.exception?.localizedMessage
                    Toast.makeText(this, "Не удалось обновить статус задачи", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateResult(){
        val resultFileName = Repository.currentTask?.resultFileName ?: return
        val resultFilePath = Repository.currentTask?.resultFilePath ?: return
        val userStatus = Util.getUserStatus()
        val status = Repository.currentTask?.status ?: return
        when(userStatus){
            1 -> {
                resultText.visibility = View.VISIBLE
                resultContainerView.visibility = View.VISIBLE
                if(resultFilePath.isNotEmpty()){
                    resultSendView.visibility = View.GONE
                    resultImageView.visibility = View.VISIBLE
                    resultFileNameView.visibility = View.VISIBLE
                    resultFileNameView.text = resultFileName
                } else {
                    if(status == 1L) {
                        resultSendView.visibility = View.VISIBLE
                        resultImageView.visibility = View.GONE
                        resultFileNameView.visibility = View.GONE
                    } else {
                        resultText.visibility = View.GONE
                        resultContainerView.visibility = View.GONE
                    }
                }
            }
            else -> {
                if(resultFilePath.isNotEmpty() && status != 1L){
                    resultText.visibility = View.VISIBLE
                    resultContainerView.visibility = View.VISIBLE
                    resultSendView.visibility = View.GONE
                    resultImageView.visibility = View.VISIBLE
                    resultFileNameView.visibility = View.VISIBLE
                    resultFileNameView.text = resultFileName
                } else {
                    resultText.visibility = View.GONE
                    resultContainerView.visibility = View.GONE
                }
            }
        }

    }

    private fun setupResultButton(){
        Repository.currentTask ?: return
        updateResult()
        resultSendView.setOnClickListener {
            attachFile()
        }

    }
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

    //удалить задачу
    private fun deleteTask(){
        Repository.currentTask ?: return
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Вы уверены")
        builder.setMessage("Удалить задачу?")
        builder.setPositiveButton(
            "Да"
        ) { dialog, which -> // Do nothing but close the dialog
            //здесь надо удалить задачу из базы
            val db = Firebase.firestore
            db.collection("tasks").document(Repository.currentTask!!.tid).delete()
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(this, "Задача удалена", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        finish()
                    } else {
                        Toast.makeText(this, "Не удалось удалить задачу", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
        }
        builder.setNegativeButton(
            "Нет"
        ) { dialog, which -> // Do nothing
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()

    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete_task -> deleteTask()
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Repository.currentTask ?: return
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
                    val path = downloadUri.toString()
                    Toast.makeText(this,"Файл загружен", Toast.LENGTH_SHORT).show()
                    Repository.currentTask!!.resultFilePath = path
                    Repository.currentTask!!.resultFileName = filename
                    val dataToUpdate = hashMapOf(
                        "result_file_name" to filename,
                        "result_file_path" to path
                    )
                    val tid = Repository.currentTask!!.tid
                    val db = Firebase.firestore
                    db.collection("tasks").document(tid).update(dataToUpdate as Map<String, Any>)
                        .addOnCompleteListener { result ->
                            if(result.isSuccessful){
                                progressView.visibility = View.GONE
                                updateResult()
                            } else {
                                progressView.visibility = View.GONE
                                Toast.makeText(this, "Не удалось обновить задачу", Toast.LENGTH_SHORT).show()
                                Repository.currentTask!!.resultFilePath = ""
                                Repository.currentTask!!.resultFileName = ""
                                updateResult()
                            }
                        }
                } else {
                    Toast.makeText(this,"Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                    progressView.visibility = View.GONE
                }
            }
        }

    }

    private fun setupDownloadButtons(){
        Repository.currentTask ?: return
        attachFileView.setOnClickListener {
            val path = Repository.currentTask!!.filePath
            val name = Repository.currentTask!!.fileName
            val desc = "Файл задания"
            downloadFile(name, desc, path)
        }
        resultImageView.setOnClickListener {
            val path = Repository.currentTask!!.resultFilePath
            val name = Repository.currentTask!!.resultFileName
            val desc = "Файл результата"
            downloadFile(name, desc, path)
        }
    }

    private fun downloadFile(filename: String, desc: String, path: String){
        if(path.isEmpty()) {
            Toast.makeText(this,"Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
            return
        }
        Util.downloadFileFromPath(this,filename,desc,path)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_menu, menu)
        Repository.currentProject ?: return true
        val uid = Firebase.auth.currentUser?.uid ?: ""
        if(!uid.equals(Repository.currentProject!!.oid)){
            menu?.findItem(R.id.delete_task)?.isVisible = false
        }
        if(Repository.isArchive){
            menu?.findItem(R.id.delete_task)?.isVisible = false
        }
        return true
    }

}