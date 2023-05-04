package com.example.taskmanager.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.R
import com.example.taskmanager.models.Message
import com.example.taskmanager.models.Project
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.repo.Util
import com.example.taskmanager.sendpush.ApiClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
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

//экран чата
class ChatProjectActivity : AppCompatActivity() {
    val RESULT_SELECT_FILE = 1133
    private lateinit var toolbar: Toolbar
    private lateinit var currentProject: Project

    //список сообщений
    private lateinit var recyclerView: RecyclerView
    private val adapter = ChatAdapter()

    //поле ввода сообщения
    private lateinit var messageView: EditText

    //кнопка отправить сообщение
    private lateinit var sendView: View

    //индикатор выполнения
    private lateinit var progressView: View

    //элемент для показа прекрепляемого файла
    private lateinit var fileContainerView: View
    private lateinit var fileSelectView: View //скрепка
    private lateinit var filePathView: TextView
    private var currentFileFullPath = ""
    private var currentFileName = ""
    private val storage: FirebaseStorage = Firebase.storage
    private lateinit var storageRefFiles: StorageReference
    private val fcmTokens = ArrayList<String>() //для отправки пушей

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_project)
        if (Repository.currentProject == null) {
            finish()
            return
        }
        fileContainerView = findViewById(R.id.file_container)
        fileSelectView = findViewById(R.id.file_select)
        filePathView = findViewById(R.id.file_path)
        filePathView.text = ""
        fileContainerView.visibility = View.GONE
        fileSelectView.visibility = View.VISIBLE

        currentProject = Repository.currentProject ?: return
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.subtitle = currentProject.name


        //настройка списка сообщений
        recyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
        }
        recyclerView.adapter = adapter

        messageView = findViewById(R.id.text)
        sendView = findViewById(R.id.send)
        progressView = findViewById(R.id.progress)
        progressView.visibility = View.GONE

        //путь в облаке для хранения прикрепленных файлов
        storageRefFiles = storage.reference.child("users/files/" + Repository.user?.uid)

        //выбор прикрепляемого файла
        fileSelectView.setOnClickListener {
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
                    ) {
                    }
                }).check()
        }

        val db = Firebase.firestore
        val uid = Firebase.auth.currentUser?.uid ?: return
        val pid = Repository.currentProject?.pid ?: return

        //получить токены для отправки пушей участникам проекта
        initFcmTokens()

        //кнопка отправить сообщение
        sendView.setOnClickListener {
            val text = messageView.text.toString()
            val cal = Calendar.getInstance()
            val timestamp = cal.timeInMillis
            if (text.isEmpty() && currentFileFullPath.isEmpty()) {
                Toast.makeText(
                    this,
                    "Введите текст сообщения или прикрепите файл",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //скрыть клавиатуру
                this.currentFocus?.let { view ->
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                }
                val photo = Repository.user?.photo ?: ""
                val email = Repository.user?.email ?: ""
                val name = Repository.user?.name ?: ""
                val name2 = Repository.currentProject?.name ?: ""
                val userName = if (name.isNotEmpty()) name else email

                val messageData = hashMapOf(
                    "pid" to pid,
                    "uid" to uid,
                    "text" to text,
                    "file" to currentFileFullPath,
                    "filename" to currentFileName,
                    "photo" to photo,
                    "timestamp" to timestamp,
                    "name" to userName,
                    "name2" to name2,
                )

                currentFileFullPath = ""
                currentFileName = ""
                filePathView.text = ""
                fileContainerView.visibility = View.GONE
                fileSelectView.visibility = View.VISIBLE

                progressView.visibility = View.VISIBLE
                db.collection("messages").document().set(messageData).addOnCompleteListener {
                    Util.setAlarmDedline2(this, pid, name2)
                    sendPush(userName, text)
                    progressView.visibility = View.GONE
                    messageView.setText("")
                }
                    .addOnFailureListener {
                        Toast.makeText(this, "Не удалось отправить сообщение", Toast.LENGTH_SHORT)
                            .show()
                        progressView.visibility = View.GONE
                        messageView.setText("")
                    }
            }

        }

        //загрузить список сообщений из базы данных
        db.collection("messages")
            .whereEqualTo("pid", pid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error == null && value != null) {
                    val documents = value.documents
                    val list = ArrayList<Message>()
                    for (doc in documents) {
                        val _pid = doc["pid"] as String? ?: ""
                        val _uid = doc["uid"] as String? ?: ""
                        val _name = doc["name"] as String? ?: ""
                        val _name2 = doc["name2"] as String? ?: ""
                        val _photo = doc["photo"] as String? ?: ""
                        val _file = doc["file"] as String? ?: ""
                        val _timestamp = doc["timestamp"] as Long? ?: 0L
                        val _text = doc["text"] as String? ?: ""
                        val _filename = doc["filename"] as String? ?: "unknown.file"
                        val message = Message(
                            pid = _pid,
                            uid = _uid,
                            name = _name,
                            name2 = _name2,
                            photo = _photo,
                            file = _file,
                            timestamp = _timestamp,
                            text = _text,
                            filename = _filename,
                        )
                        list.add(message)
                    }
                    adapter.submitList(list)
                }
            }
        //загрузить файл из сообщения пользователя
        adapter.setUpdateFun { message ->
            val filepath = message.file
            if (filepath.isNotEmpty()) {
                Util.downloadFileFromPath(this,message.filename,message.text,filepath)
            }
        }

    }

    //загрузить выбранный файл в облако
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_SELECT_FILE && resultCode == Activity.RESULT_OK && null != data) {
            val uri: Uri = data?.data ?: return
            
            val filename = Util.getFilenameFromUri(this, uri)
            currentFileName = filename
            val spaceRef = storageRefFiles.child(filename)
            val uploadTask = spaceRef.putFile(uri)
            progressView.visibility = View.VISIBLE
            fileContainerView.visibility = View.GONE
            filePathView.text = ""
            fileSelectView.visibility = View.VISIBLE

            uploadTask.addOnFailureListener {
                progressView.visibility = View.GONE
                Toast.makeText(this, "Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                fileContainerView.visibility = View.GONE
                filePathView.text = ""
                fileSelectView.visibility = View.VISIBLE
            }
            val tastUrl = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        Toast.makeText(this, "Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                        progressView.visibility = View.GONE
                        fileContainerView.visibility = View.GONE
                        filePathView.text = ""
                        fileSelectView.visibility = View.VISIBLE
                    }
                }
                spaceRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    currentFileFullPath = downloadUri.toString()
                    Toast.makeText(this, "Файл загружен", Toast.LENGTH_SHORT).show()
                    progressView.visibility = View.GONE
                    fileContainerView.visibility = View.VISIBLE
                    filePathView.text = filename
                    fileSelectView.visibility = View.GONE
                } else {
                    Toast.makeText(this, "Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
                    progressView.visibility = View.GONE
                    fileContainerView.visibility = View.GONE
                    filePathView.text = ""
                    fileSelectView.visibility = View.VISIBLE
                }
            }
        }
    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    //получить список токенов участников проекта для отправки пушей
    private fun initFcmTokens() {
        val pid = Repository.currentProject?.pid ?: return
        //получить всех участников проекта
        val uid = Repository.user?.uid ?: return
        fcmTokens.clear()
        val db = Firebase.firestore
        db.collection("members")
            .whereEqualTo("project_id", pid)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result.documents
                    for (doc in documents) {
                        val fcm_token = doc["user_fcm_token"] as String? ?: ""
                        val user_uid = doc["user_uid"] as String? ?: ""
                        if (fcm_token.isEmpty()) continue
                        if (user_uid.equals(uid)) continue
                        fcmTokens.add(fcm_token)
                    }
                    Log.d("anna", "i am here")
                }

            }
    }

    //отправить пуш всем участкикам проекта
    private fun sendPush(title: String, text: String) {
        if (fcmTokens.isEmpty()) return
        for (fcmToken in fcmTokens) {
            sendPush(fcmToken, title, text)
        }
    }

    //отправить пуш конкретному участнику
    private fun sendPush(token: String, title: String, text: String) {
        Log.d("anna", "send push")
        ApiClient.sendPush(token, title, text)
    }
}

//адаптер для списка сообщений
class ChatAdapter : ListAdapter<Message, ChatAdapter.ChatViewHolder>(ChatComparator()) {
    lateinit var update: (Message) -> Unit

    fun setUpdateFun(u: (Message) -> Unit) {
        update = u
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return ChatViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, this)
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //        private val nameView: TextView = itemView.findViewById(R.id.name)
//        private val photoView: ImageView = itemView.findViewById(R.id.photo)
//        private val userSelectBtn: View = itemView.findViewById(R.id.select_user)
        private val sendContainer: View = itemView.findViewById(R.id.send_container)
        private val reciveContainer: View = itemView.findViewById(R.id.recive_container)
        private val sendPhoto: ImageView = itemView.findViewById(R.id.send_photo)
        private val recivePhoto: ImageView = itemView.findViewById(R.id.recive_photo)
        private val sendName: TextView = itemView.findViewById(R.id.send_name)
        private val reciveName: TextView = itemView.findViewById(R.id.recive_name)
        private val sendText: TextView = itemView.findViewById(R.id.send_text)
        private val reciveText: TextView = itemView.findViewById(R.id.recive_text)
        private val sendAttachFile: View = itemView.findViewById(R.id.attach_file)
        private val reciveAttachFile: View = itemView.findViewById(R.id.attach_recive_file)

        fun bind(current: Message, adapter: ChatAdapter) {
            val uid = Repository.user?.uid ?: return
            //является ли пользователь отравителем или получателем сообщения
            if (uid.equals(current.uid)) {
                //this send message
                sendContainer.visibility = View.VISIBLE
                reciveContainer.visibility = View.GONE
                if (current.photo.isNotEmpty()) {
                    Picasso.get().load(current.photo).into(sendPhoto)
                } else {
                    //Picasso.get().load(R.drawable.ic_profile).into(sendPhoto)
                    sendPhoto.setImageResource(R.drawable.ic_profile)
                }
                sendName.text = current.name
                sendText.text = current.text
                if (current.file.isNotEmpty()) {
                    sendAttachFile.visibility = View.VISIBLE
                    sendAttachFile.setOnClickListener { adapter.update(current) }
                } else {
                    sendAttachFile.visibility = View.GONE
                    sendAttachFile.setOnClickListener { }
                }
            } else {
                //this recive message
                sendContainer.visibility = View.GONE
                reciveContainer.visibility = View.VISIBLE
                if (current.photo.isNotEmpty()) {
                    Picasso.get().load(current.photo).into(recivePhoto)
                } else {
                    //Picasso.get().load(R.drawable.ic_profile).into(recivePhoto)
                    recivePhoto.setImageResource(R.drawable.ic_profile)
                }
                reciveName.text = current.name
                reciveText.text = current.text
                if (current.file.isNotEmpty()) {
                    reciveAttachFile.visibility = View.VISIBLE
                    reciveAttachFile.setOnClickListener { adapter.update(current) }
                } else {
                    reciveAttachFile.visibility = View.GONE
                    reciveAttachFile.setOnClickListener { }
                }
            }
        }

        companion object {
            fun create(parent: ViewGroup): ChatViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_message, parent, false)
                return ChatViewHolder(view)
            }
        }
    }

    class ChatComparator : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.uid == newItem.uid
        }
    }
}
