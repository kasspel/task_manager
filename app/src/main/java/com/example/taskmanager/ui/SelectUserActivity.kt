package com.example.taskmanager.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.R
import com.example.taskmanager.models.User
import com.example.taskmanager.repo.Repository
import com.squareup.picasso.Picasso
import java.util.*

//выбор участников среди всех пользователей
class SelectUserActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    //список пользователей
    private lateinit var recyclerView: RecyclerView
    private val adapter = UserSelectAdapter()
    //поле ввода для поиска среди пользователей
    private lateinit var searchView: EditText
    private val searchList = ArrayList<User>()
    private val usersList = ArrayList<User>()

    //подготовить список пользователей для показа
    private fun calculateUsersList(){
        //очистить существующий список
        usersList.clear()
        //пройтись по всем известным пользователям
        for(user in Repository.usersList){
            //исключить руководителя проекта
            if(user.uid.equals(Repository.user?.uid)) continue
            //исключить уже добавленных пользователей
            var cont = false
            for(selectedUser in Repository.selectedUsersList){
                if(selectedUser.uid.equals(user.uid)){
                    cont = true
                    break
                }
            }
            if(cont) continue
            usersList.add(user)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_user)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        recyclerView = findViewById(R.id.recycler)
        recyclerView.adapter = adapter
        calculateUsersList()
        adapter.submitList(usersList)
        //добавить пользователя в участники проекта
        adapter.setUpdateFun { user ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            builder.setTitle("Вы уверены")
            builder.setMessage("Добавить участника?")

            builder.setPositiveButton(
                "Да"
            ) { dialog, which -> // Do nothing but close the dialog
                dialog.dismiss()
                Repository.selectedUsersList.add(user)
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
        searchView = findViewById(R.id.search)
        //поиск по списку пользователей если в поле поиска что то введено
        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val text = searchView.text.toString().lowercase(Locale.ROOT)
                if(text.length > 0){
                    //search
                    searchList.clear()
                    for(user in usersList){
                        val name = if(user.name.isNotEmpty()) user.name else user.email
                        if(name.lowercase(Locale.ROOT).startsWith(text)) searchList.add(user)
                    }
                    adapter.submitList(searchList.clone() as List<User>)
                } else {
                    adapter.submitList(usersList.clone() as List<User>)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })
    }

    //обработка нажатия кнопки назад в заголовке приложения, для тех экранов где она есть
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
//адаптер к списку пользователей
class UserSelectAdapter: ListAdapter<User, UserSelectAdapter.UserViewHolder>(UsersComparator()) {
    lateinit var update:(User) -> Unit

    fun setUpdateFun(u:(User) -> Unit ) {
        update = u
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
        private val userSelectBtn: View = itemView.findViewById(R.id.select_user)

        fun bind(current: User, adapter: UserSelectAdapter) {
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
            fun create(parent: ViewGroup): UserViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_user_select, parent, false)
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


