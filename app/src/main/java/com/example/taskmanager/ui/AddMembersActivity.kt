package com.example.taskmanager.ui

import android.content.Intent
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso

//экран добавления участников в проект
class AddMembersActivity : AppCompatActivity() {
    //тулбар
    private lateinit var toolbar: Toolbar
    //кнопка добавить участника
    private lateinit var addMemberView: FloatingActionButton
    //список уже добавленных участников
    private lateinit var recyclerView: RecyclerView
    //адаптер к списку
    private val adapter = MemberSelectAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_members)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        addMemberView = findViewById(R.id.add_member)
        //обработка нажания на кнопку добавить участника из списка всех пользователей
        addMemberView.setOnClickListener {
            val intent = Intent(this, SelectUserActivity::class.java)
            startActivityForResult(intent, 2222)
        }

        recyclerView = findViewById(R.id.recycler)
        recyclerView.adapter = adapter
        //обработка нажатия на кнопку удалить участника из списка
        adapter.setUpdateFun { user ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            builder.setTitle("Вы уверены")
            builder.setMessage("Удалить участника?")

            builder.setPositiveButton(
                "Да"
            ) { dialog, which -> // Do nothing but close the dialog
                dialog.dismiss()
                val uid = user.uid
                val list = ArrayList<User>()
                for(u in Repository.selectedUsersList){
                    if(u.uid.equals(uid)) continue
                    list.add(u)
                }
                //обновить общие переменные
                Repository.selectedUsersList.clear()
                Repository.selectedUsersList.addAll(list)
                //обновить интерфейс
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
        updateUi()
    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    //после добавления участника из списка пользователей обновить интерфейс
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateUi()
    }

    //метод обновления интерфейса
    private fun updateUi(){
        adapter.submitList(Repository.selectedUsersList.clone() as List<User>)
    }
}

//класс адаптера к списку участников проекта
class MemberSelectAdapter: ListAdapter<User, MemberSelectAdapter.MemberViewHolder>(MembersComparator()) {
    lateinit var update:(User) -> Unit

    fun setUpdateFun(u:(User) -> Unit ) {
        update = u
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder.create(parent)
    }
    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, this)
    }
    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.name)
        private val photoView: ImageView = itemView.findViewById(R.id.photo)
        private val userSelectBtn: View = itemView.findViewById(R.id.delete_member)

        fun bind(current: User, adapter: MemberSelectAdapter) {
            val name = current.name
            val email = current.email
            val photo = current.photo
            nameView.text = if(name.isNotEmpty()) name else email
            if(photo.isNotEmpty()) Picasso.get().load(photo).into(photoView)
            userSelectBtn.setOnClickListener {
                adapter.update(current)
            }
        }

        companion object {
            fun create(parent: ViewGroup): MemberViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_member_select, parent, false)
                return MemberViewHolder(view)
            }
        }
    }
    class MembersComparator : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }
    }


}
