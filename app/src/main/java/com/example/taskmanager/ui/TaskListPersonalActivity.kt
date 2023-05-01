package com.example.taskmanager.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.R
import com.example.taskmanager.models.Project
import com.example.taskmanager.models.Task
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.repo.Util
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TaskListPersonalActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var currentPersonalProject: Project
    private lateinit var addTaskView: FloatingActionButton

    private lateinit var recycler: RecyclerView
    private val adapter = TaskPersonalAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list_personal)
        if(Repository.currentPersonalProject == null) {
            finish()
            return
        }
        currentPersonalProject = Repository.currentPersonalProject ?: return
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.subtitle = currentPersonalProject.name
        recycler = findViewById(R.id.recycler)
        recycler.adapter = adapter
        loadTasks()
        adapter.setUpdateFun { task ->
            Repository.currentPersonalTask = task
            val intent = Intent(this, TaskPersonalActivity::class.java)
            startActivity(intent)
        }
        addTaskView = findViewById(R.id.add_task)

        addTaskView.setOnClickListener {
            val intent = Intent(this, AddPersonalTaskActivity::class.java)
            startActivity(intent)
        }
       if(Repository.isArchive) addTaskView.visibility = View.GONE

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.personal_menu, menu)
        return true
    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete_project -> deleteProject()
            R.id.archive_project -> archiveProject()
            R.id.about_project -> infoProject()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteProject(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Вы уверены")
        builder.setMessage("Удалить проект?")
        builder.setPositiveButton(
            "Да"
        ) { dialog, which -> // Do nothing but close the dialog
            //здесь надо удалить проект из базы
            val pid = Repository.currentPersonalProject?.pid ?: ""
            if(pid.isNotEmpty()){
                val db = Firebase.firestore
                db.collection("projects").document(pid).delete()
                Toast.makeText(this, "Проект удален", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Не удалось удалить проект", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton(
            "Нет"
        ) { dialog, which -> // Do nothing
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
    private fun archiveProject(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Вы уверены")
        builder.setMessage("Отправить проект в архив?")
        builder.setPositiveButton(
            "Да"
        ) { dialog, which -> // Do nothing but close the dialog
            //здесь надо отправить проект в архив
            val pid = Repository.currentPersonalProject?.pid ?: ""
            if(pid.isNotEmpty()){
                val db = Firebase.firestore
                val data = hashMapOf(
                    "is_archive" to true,
                    "is_personal" to true,
                )
                db.collection("projects").document(pid).update(data as Map<String, Any>)
                Toast.makeText(this, "Проект отправлен в архив", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Не удалось отправить проект в архив", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton(
            "Нет"
        ) { dialog, which -> // Do nothing
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
    private fun infoProject(){
        val intent = Intent(this, InfoPersonalProjectActivity::class.java)
        startActivity(intent)
    }

    private fun loadTasks(){
        val pID = currentPersonalProject.pid
        val db = Firebase.firestore
        db.collection("tasks")
            .whereEqualTo("pid", pID)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if(error == null && value != null){
                    val documents = value.documents
                    val list = ArrayList<Task>()
                    for(doc in documents){
                        val task = Task(
                            pid = doc["pid"] as String? ?:  "",
                            oid = doc["oid"] as String? ?:  "",
                            name = doc["name"] as String? ?:  "",
                            desc = doc["desc"] as String? ?:  "",
                            leadName = doc["lead_name"] as String? ?:  "",
                            leadPhoto = doc["lead_photo"] as String? ?:  "",
                            leadUid = doc["lead_uid"] as String? ?:  "",
                            leadMid = doc["lead_mid"] as String? ?:  "",
                            leadFcm = doc["lead_fcm"] as String? ?:  "",
                            leadEmail = doc["lead_email"] as String? ?:  "",
                            leadPhone = doc["lead_phone"] as String? ?:  "",
                            dateOfEnd = doc["date_of_end"] as String? ?:  "",
                            dateOfEndTimestamp = doc["date_of_end_timestamp"] as Long? ?: 0L,
                            fileName = doc["file_name"] as String? ?:  "",
                            filePath = doc["file_path"] as String? ?:  "",
                            isSubTask = doc["is_subtask"] as Boolean? ?: false,
                            timestampTask = doc["timestamp"] as Long? ?: 0L,
                            status = doc["status"] as Long? ?: 0L,
                            tid = doc.id,
                            resultFileName = doc["result_file_name"] as String? ?: "",
                            resultFilePath = doc["result_file_path"] as String? ?: "",
                        )
                        list.add(task)
                    }
                    adapter.submitList(list)
                }

            }
    }

}
class TaskPersonalAdapter: ListAdapter<Task, TaskPersonalAdapter.ProjectViewHolder>(TaskComparator()) {
    lateinit var update:(Task) -> Unit

    fun setUpdateFun(u:(Task) -> Unit ) {
        update = u
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        return ProjectViewHolder.create(parent)
    }
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, this)
    }
    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.name)
        private val descView: TextView = itemView.findViewById(R.id.desc)
        private val dateView: TextView = itemView.findViewById(R.id.start_date)
        private val statusView: TextView = itemView.findViewById(R.id.status)
        private val rootView: View = itemView.findViewById(R.id.root)

        fun bind(current: Task, adapter: TaskPersonalAdapter) {
            val name = current.name
            val desc = current.desc
            val date = current.dateOfEnd
            val oid = Repository.currentPersonalProject?.oid ?:""
            val uid = Repository.user?.uid ?: ""
            val status = Util.getTextStatus(current.status, oid, uid)
            Util.setTextStyle(current.status,oid,uid,statusView)
            val isSubtask = current.isSubTask

            nameView.text = name
            descView.text = desc
            dateView.text = date
            statusView.text = status

            rootView.setOnClickListener {
                adapter.update(current)
            }
            if(isSubtask){
                //make start margin
                val param = rootView.layoutParams as RecyclerView.LayoutParams
                param.setMargins(30, 0, 0, 10)
                rootView.layoutParams = param
            } else {
                val param = rootView.layoutParams as RecyclerView.LayoutParams
                param.setMargins(0, 0, 0, 10)
                rootView.layoutParams = param
            }
        }

        companion object {
            fun create(parent: ViewGroup): ProjectViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task_personal, parent, false)
                return ProjectViewHolder(view)
            }
        }
    }
    class TaskComparator : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.timestampTask == newItem.timestampTask
        }
    }

}
