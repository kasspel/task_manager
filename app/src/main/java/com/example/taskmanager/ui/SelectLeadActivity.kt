package com.example.taskmanager.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.R
import com.example.taskmanager.models.User
import com.example.taskmanager.repo.Repository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

//экран выбора ответственного в задаче
class SelectLeadActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    //список участников
    private lateinit var recycler: RecyclerView
    private val adapter = LeadSelectAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_lead)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        Repository.currentProject?: finish()
        
        recycler = findViewById(R.id.recycler)
        recycler.adapter = adapter

        //установка ответственного
        adapter.setUpdateFun { user ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            builder.setTitle("Вы уверены")
            builder.setMessage("Сделать ответственным?")

            builder.setPositiveButton(
                "Да"
            ) { dialog, which -> // Do nothing but close the dialog
                dialog.dismiss()
                Repository.curentLead = user
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

        //load members of project
        val db = Firebase.firestore
        val pid = Repository.currentProject?.pid ?: return
        db.collection("members")
            .whereEqualTo("project_id", pid)
            .get()
            .addOnCompleteListener { data ->
                if(data.isSuccessful){
                    val documents = data.result.documents
                    val list = ArrayList<User>()
                    for(doc in documents){
                        val mid = doc.id
                        val oid = doc["project_oid"] as String? ?: ""
                        val uid = doc["user_uid"] as String? ?: ""
                        if(oid.equals(uid)) continue //owner
                        val name = doc["user_name"] as String? ?: ""
                        val email = doc["user_email"] as String? ?: ""
                        val phone = doc["user_phone"] as String? ?: ""
                        val photo = doc["user_photo"] as String? ?: ""
                        val fcm = doc["user_fcm_token"] as String? ?: ""
                        val user = User(
                            uid = uid,
                            name = if(name.isNotEmpty()) name else email,
                            email = email,
                            phone = phone,
                            photo = photo,
                            mid = mid,
                            fcm_token = fcm,
                        )
                        list.add(user)
                    }
                    adapter.submitList(list)
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
}
//адаптер к списку участников проекта
class LeadSelectAdapter: ListAdapter<User, LeadSelectAdapter.LeadViewHolder>(UsersComparator()) {
    lateinit var update:(User) -> Unit

    fun setUpdateFun(u:(User) -> Unit ) {
        update = u
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        return LeadViewHolder.create(parent)
    }
    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, this)
    }
    class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.name)
        private val photoView: ImageView = itemView.findViewById(R.id.photo)
        private val container: View = itemView.findViewById(R.id.container)

        fun bind(current: User, adapter: LeadSelectAdapter) {
            val name = current.name
            val email = current.email
            val photo = current.photo
            nameView.text = if(name.isNotEmpty()) name else email
            if(photo.isNotEmpty()) Picasso.get().load(photo).into(photoView)
            else photoView.setImageResource(R.drawable.ic_profile)
            container.setOnClickListener {
                adapter.update(current)
            }
        }

        companion object {
            fun create(parent: ViewGroup): LeadViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_lead_select, parent, false)
                return LeadViewHolder(view)
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
