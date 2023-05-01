package com.example.taskmanager.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.R
import com.example.taskmanager.models.Project
import com.example.taskmanager.models.User
import com.example.taskmanager.repo.Repository
import com.example.taskmanager.repo.Util
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList

//экран с информацией по проекту
class InfoProjectActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var currentProject: Project
    //кнопка выбора даты начала проекта
    private lateinit var selectStartDate: Button
    //кнопка выбора даты окончания проекта
    private lateinit var selectEndDate: Button
    //кнопка сохранения формы
    private lateinit var saveView: Button
    //поле ввода названия проекта
    private lateinit var nameView: EditText
    //поле ввода описания проекта
    private lateinit var descriptionView: EditText
    //список участников проекта
    private lateinit var recyclerView: RecyclerView
    private val adapter = UserViewAdapter()
    //кнопка добавления учасника
    private lateinit var addMemberView: FloatingActionButton

    //установка интерфейса в зависимости от статуса участника
    private fun setupUiWithStats(){
        val userStatus = Util.getUserStatus()
        if(userStatus != 2){
            saveView.visibility = View.GONE
            selectEndDate.isEnabled = false
            selectStartDate.isEnabled = false
            nameView.isEnabled = false
            descriptionView.isEnabled = false
            addMemberView.visibility = View.GONE
        }
    }

    //обновить данные проекта в базе данных с предварительной проверкой
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
        val pid = Repository.currentProject?.pid ?: return
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
        )
        db.collection("projects").document(pid).update(data as Map<String, Any>).addOnSuccessListener {
            //add owner
            val owner = Repository.user!!
            //Repository.selectedUsersList.add(owner)

            //add members
            for(user in Repository.selectedUsersList){
                val memberData = hashMapOf(
                    "project_id" to pid,
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
                val mid = user.mid
                if(mid.isEmpty()){
                    db.collection("members").document().set(memberData)
                } else {
                    db.collection("members").document(mid).update(memberData as Map<String, Any>)
                }
                Util.setAlarmDedline(this,project.endDate, pid, project.name)
            }
            Toast.makeText(this, "Проект обновлен", Toast.LENGTH_SHORT).show()
            Repository.selectedUsersList.clear()
            finish()
        }

    }



    override fun onDestroy() {
        super.onDestroy()
        Repository.selectedUsersList.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_project)
        if (Repository.currentProject == null) {
            finish()
            return
        }
        currentProject = Repository.currentProject ?: return
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.subtitle = currentProject.name

        selectStartDate = findViewById(R.id.pick_start_date)
        selectEndDate = findViewById(R.id.pick_end_date)
        saveView = findViewById(R.id.save)
        nameView = findViewById(R.id.name)
        descriptionView = findViewById(R.id.description)
        addMemberView = findViewById(R.id.add_member)
        recyclerView = findViewById(R.id.recycler)
        recyclerView.adapter = adapter
            //настройка интерфейса для архивных проектов
        if(Repository.isArchive){
            selectEndDate.isEnabled = false
            selectStartDate.isEnabled = false
            saveView.visibility = View.GONE
            nameView.isEnabled = false
            descriptionView.isEnabled = false
            addMemberView.isEnabled = false

        }
        //загрузка информации из базы данных
        loadData()
        //настройка интерфейса
        setupUi()

        //обработчик нажания на кнопку сохранения формы
        saveView.setOnClickListener {
            updateWithValidate()
        }

        //выбор даты начала проекта
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
            //выбор даты окончания проекта
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
        //показать профиль пользователя по нажатию на фото
        adapter.setUpdateFun { user ->
            //здесь переход на страницу с информацией о пользователе
            Repository.userInfo = user
            val intent = Intent(this, ProfileInfoActivity::class.java)
            startActivity(intent)
        }

        adapter.setDeleteFun { user ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Вы уверены")
            builder.setMessage("Удалить участника?")
            builder.setPositiveButton(
                "Да"
            ) { dialog, which -> // Do nothing but close the dialog
                dialog.dismiss()
                //удалить участника из базы
                val db = Firebase.firestore
                db.collection("members").document(user.mid).delete()
                //удалить участника из списка
                val list = ArrayList<User>()
                for(u in Repository.selectedUsersList){
                    if(u.uid.equals(user.uid)) continue
                    list.add(u)
                }
                Repository.selectedUsersList.clear()
                Repository.selectedUsersList.addAll(list)
                updateUi()
            }
            builder.setNegativeButton(
                "Нет"
            ) { dialog, which -> // Do nothing
                dialog.dismiss()
            }
            val alert: AlertDialog = builder.create()
            alert.show()
        }

        //кнопка добавить участника
        addMemberView.setOnClickListener {
            val intent = Intent(this, SelectUserActivity::class.java)
            startActivityForResult(intent, 4455)
        }
    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupUi() {
        nameView.setText(Repository.currentProject?.name ?: "")
        descriptionView.setText(Repository.currentProject?.desc ?: "")
        selectStartDate.text = Repository.currentProject?.startDate ?: ""
        selectEndDate.text = Repository.currentProject?.endDate ?: ""
        setupUiWithStats()
    }

    private fun loadData() {
        val db = Firebase.firestore
        val list = ArrayList<User>()
        //Repository.selectedUsersList.clear()
        db.collection("members")
            .whereEqualTo("project_id", Repository.currentProject?.pid ?: "")
            .get()
            .addOnCompleteListener {
                val documents = it.result.documents
                list.clear()
                for (doc in documents) {
                    val uid = doc["user_uid"] as String? ?: ""
                    val fcm_token = doc["user_fcm_token"] as String? ?: ""
                    val name = doc["user_name"] as String? ?: ""
                    val email = doc["user_email"] as String? ?: ""
                    val phone = doc["user_phone"] as String? ?: ""
                    val photo = doc["user_photo"] as String? ?: ""
                    val mid = doc.id
                    val user = User(
                        uid = uid,
                        name = name,
                        email = email,
                        phone = phone,
                        photo = photo,
                        mid = mid,
                        fcm_token = fcm_token,
                    )
                    list.add(user)
                }
                Repository.selectedUsersList.clear()
                Repository.selectedUsersList.addAll(list)
                adapter.submitList(list.clone() as List<User>)
            }
    }

    private fun updateUi() {
        adapter.submitList(Repository.selectedUsersList.clone() as List<User>)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateUi()
    }
}

//адаптер для списка участников
class UserViewAdapter : ListAdapter<User, UserViewAdapter.UserViewHolder>(UsersComparator()) {
    lateinit var update: (User) -> Unit
    lateinit var delete: (User) -> Unit


    fun setUpdateFun(u: (User) -> Unit) {
        update = u
    }
    fun setDeleteFun(u: (User) -> Unit) {
        delete = u
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, this)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.name)
        private val photoView: ImageView = itemView.findViewById(R.id.photo)
        private val userOwner: View = itemView.findViewById(R.id.owner)
        private val deleteView: View = itemView.findViewById(R.id.delete)

        fun bind(current: User, adapter: UserViewAdapter) {
            val name = current.name
            val email = current.email
            val photo = current.photo
            nameView.text = if (name.isNotEmpty()) name else email
            if (photo.isNotEmpty()) Picasso.get().load(photo).into(photoView)
            photoView.setOnClickListener {
                adapter.update(current)
            }
            val oid = Repository.currentProject?.oid ?: return
            val uid = current.uid
            userOwner.visibility = if (oid.equals(uid)) View.VISIBLE else View.GONE
            deleteView.visibility = if (oid.equals(uid)) View.GONE else View.VISIBLE
            deleteView.setOnClickListener {
                adapter.delete(current)
            }
        }

        companion object {
            fun create(parent: ViewGroup): UserViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_member_view, parent, false)
                return UserViewHolder(view)
            }
        }
    }

    class UsersComparator : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }
    }
}
