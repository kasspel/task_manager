package com.example.taskmanager.ui

import android.Manifest
import android.app.Activity
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

class TaskPersonalActivity : AppCompatActivity() {
    private val RESULT_SELECT_FILE = 7755
    private lateinit var toolbar: Toolbar

    private lateinit var nameTaskView: TextView
    private lateinit var descTaskView: TextView
    private lateinit var selectDateView: TextView
    private lateinit var progressView: ProgressBar
    private lateinit var attachFileView: ImageView
    private lateinit var fileNameView: TextView
    private lateinit var fileContainer: View
    private lateinit var statusView: TextView

    private lateinit var statusContainerView: View
    private lateinit var startButtonView: Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_personal)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        Repository.currentPersonalProject ?: finish()
        val taskName = Repository.currentPersonalTask?.name ?: ""
        toolbar.subtitle = taskName
        Repository.currentPersonalTask ?: finish()

        statusContainerView = findViewById(R.id.status_container)
        startButtonView = findViewById(R.id.start_button)
        acceptButtonView = findViewById(R.id.accept_button)

        resultContainerView = findViewById(R.id.result_container)
        resultFileNameView = findViewById(R.id.result_filename)
        resultImageView = findViewById(R.id.result_attach_file)
        resultSendView = findViewById(R.id.result_attach_file_btn)
        resultText = findViewById(R.id.text_result)

        nameTaskView = findViewById(R.id.name)
        descTaskView = findViewById(R.id.description)
        selectDateView = findViewById(R.id.select_end_date)
        progressView = findViewById(R.id.progress)
        attachFileView = findViewById(R.id.attach_file)
        fileNameView = findViewById(R.id.filename)
        fileContainer = findViewById(R.id.file_container)
        statusView = findViewById(R.id.status)
        progressView.visibility = View.GONE

        val storage = Firebase.storage
        storageRefFiles = storage.reference.child("tasks/files/" + Repository.user?.uid)
        updateUi()
        setupStatusButton()
        setupResultButton()
        setupDownloadButtons()

    }
    private fun updateUi(){
        val photo = Repository.currentPersonalTask?.leadPhoto ?: return
        val nameLead = Repository.currentPersonalTask?.leadName ?: return
        val name = Repository.currentPersonalTask?.name ?: return
        val desc = Repository.currentPersonalTask?.desc ?: return
        val date = Repository.currentPersonalTask?.dateOfEnd ?: return
        val filename = Repository.currentPersonalTask?.fileName ?: return
        fileName = filename
        filePath = Repository.currentPersonalTask?.filePath ?: return
        
        nameTaskView.text = name
        descTaskView.text = desc
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
        val oid = Repository.currentPersonalTask?.oid ?: return
        val uid = Repository.user?.uid ?: return
        val status = Repository.currentPersonalTask?.status ?: return
        statusView.text = Util.getTextStatus(status, oid, uid)
        Util.setTextStyle(status,oid,uid,statusView)
        statusContainerView.visibility = View.VISIBLE
        when(status){
            0L -> {
                startButtonView.visibility = View.VISIBLE
                acceptButtonView.visibility = View.GONE
            }
            4L -> {
                startButtonView.visibility = View.GONE
                acceptButtonView.visibility = View.GONE
            }
            else -> {
                startButtonView.visibility = View.GONE
                acceptButtonView.visibility = View.VISIBLE
            }
        }
    }
    private fun setupStatusButton(){
        startButtonView.setOnClickListener {
            if(Repository.currentPersonalTask != null) {
                updateStatusDB(1L)
            }
        }
        acceptButtonView.setOnClickListener {
            if(Repository.currentPersonalTask != null) {
                updateStatusDB(4L)
            }
        }
    }
    private fun updateStatusDB(status: Long){
        Repository.currentPersonalTask?.status ?: return
        val tid = Repository.currentPersonalTask?.tid ?: return
        val db = Firebase.firestore
        val data = hashMapOf(
            "status" to status,
        )
        progressView.visibility = View.VISIBLE
        db.collection("tasks").document(tid).update(data as Map<String, Any>)
            .addOnCompleteListener { result ->
                if(result.isSuccessful){
                    Repository.currentPersonalTask?.status = status
                    updateStatus()
                    progressView.visibility = View.GONE
                    Toast.makeText(this, "Статус задачи обновлен", Toast.LENGTH_SHORT).show()
                } else {
                    progressView.visibility = View.GONE
                    val error = result.exception?.localizedMessage
                    Toast.makeText(this, "Не удалось обновить статус задачи", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun updateResult(){
        val resultFileName = Repository.currentPersonalTask?.resultFileName ?: return
        val resultFilePath = Repository.currentPersonalTask?.resultFilePath ?: return
        val status = Repository.currentPersonalTask?.status ?: return
        resultText.visibility = View.VISIBLE
        resultContainerView.visibility = View.VISIBLE
        if(resultFilePath.isNotEmpty()){
            resultSendView.visibility = View.GONE
            resultImageView.visibility = View.VISIBLE
            resultFileNameView.visibility = View.VISIBLE
            resultFileNameView.text = resultFileName
        } else {
                resultSendView.visibility = View.VISIBLE
                resultImageView.visibility = View.GONE
                resultFileNameView.visibility = View.GONE
        }

    }

    private fun setupResultButton(){
        Repository.currentPersonalTask ?: return
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
    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Repository.currentPersonalTask ?: return
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
                    Repository.currentPersonalTask!!.resultFilePath = path
                    Repository.currentPersonalTask!!.resultFileName = filename
                    val dataToUpdate = hashMapOf(
                        "result_file_name" to filename,
                        "result_file_path" to path
                    )
                    val tid = Repository.currentPersonalTask!!.tid
                    val db = Firebase.firestore
                    db.collection("tasks").document(tid).update(dataToUpdate as Map<String, Any>)
                        .addOnCompleteListener { result ->
                            if(result.isSuccessful){
                                progressView.visibility = View.GONE
                                updateResult()
                            } else {
                                progressView.visibility = View.GONE
                                Toast.makeText(this, "Не удалось обновить задачу", Toast.LENGTH_SHORT).show()
                                Repository.currentPersonalTask!!.resultFilePath = ""
                                Repository.currentPersonalTask!!.resultFileName = ""
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
        Repository.currentPersonalTask ?: return
        attachFileView.setOnClickListener {
            val path = Repository.currentPersonalTask!!.filePath
            val name = Repository.currentPersonalTask!!.fileName
            val desc = "Файл задания"
            downloadFile(name, desc, path)
        }
        resultImageView.setOnClickListener {
            val path = Repository.currentPersonalTask!!.resultFilePath
            val name = Repository.currentPersonalTask!!.resultFileName
            val desc = "Файл результата"
            downloadFile(name, desc, path)
        }
    }

    private fun downloadFile(filename: String, desc: String, path: String){
        if(path.isEmpty()) {
            Toast.makeText(this,"Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
            return
        }
        Util.downloadFileFromPath(this,filename, desc, path)
    }


}