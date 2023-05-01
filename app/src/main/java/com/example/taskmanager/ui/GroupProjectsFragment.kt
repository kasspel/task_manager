package com.example.taskmanager.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.R
import com.example.taskmanager.models.Project
import com.example.taskmanager.repo.Repository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

//экран списка проектов с поиском
class GroupProjectsFragment : Fragment() {

    //кнопка добавления проекта
    private lateinit var addProjectView: FloatingActionButton
    //список проектов
    private lateinit var recyclerView: RecyclerView
    private val adapter = ProjectsNewAdapter()
    //поле ввода для поиска
    private lateinit var searchView: EditText
    private val searchList = ArrayList<Project>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_group_projects, container, false)
        addProjectView = root.findViewById(R.id.add_project)
        addProjectView.setOnClickListener {
            val intent = Intent(requireActivity(), AddProjectActivity::class.java)
            startActivity(intent)
        }
        recyclerView = root.findViewById(R.id.recycler)
        recyclerView.adapter = adapter

        //загрузить список проектов из базы данных
        loadProjects()

        searchView = root.findViewById(R.id.search)
        //поиск по списку проектов если в поле поиска что то введено
        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val text = searchView.text.toString().lowercase(Locale.ROOT)
                if(text.length > 0){
                    //search
                    searchList.clear()
                    for(project in Repository.currentProjectList){
                        val name = project.name
                        if(name.lowercase(Locale.ROOT).startsWith(text)) searchList.add(project)
                    }
                    adapter.submitList(searchList.clone() as List<Project>)
                } else {
                    adapter.submitList(Repository.currentProjectList.clone() as List<Project>)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //Log.d("anna", "i am here")

            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //Log.d("anna", "i am here")

            }
        })

        //обработка нажания на проект в списке
        adapter.setUpdateFun { project ->
            Repository.currentProject = project
            val intent = Intent(requireActivity(), TaskListGroupActivity::class.java)
            startActivity(intent)
        }
        if(Repository.isArchive) addProjectView.visibility = View.GONE

        return root
    }
    private fun loadProjects(){
        val uid = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore
        val list = ArrayList<Project>()
        db.collection("members")
            .whereEqualTo("user_uid",uid)
            .whereEqualTo("is_archive", Repository.isArchive)
            .orderBy("project_timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if(error == null && value!= null){
                    list.clear()
                    for(doc in value.documents){
                        val project = Project(
                            oid = doc["project_oid"] as String? ?: "",
                            name = doc["project_name"] as String? ?: "",
                            desc = doc["project_desc"] as String? ?: "",
                            startDate = doc["project_start_date"] as String? ?: "",
                            endDate = doc["project_end_date"] as String? ?: "",
                            timestamp = doc["project_timestamp"] as Long? ?: 0L,
                            pid = doc["project_id"] as String? ?: "",
                        )
                        list.add(project)
                    }
                    Repository.currentProjectList.clear()
                    Repository.currentProjectList.addAll(list)
                    adapter.submitList(list.clone() as List<Project>)
                }
            }
    }
}

//адаптер для отображения списка проектов
class ProjectsNewAdapter: ListAdapter<Project, ProjectsNewAdapter.ProjectViewHolder>(ProjectComparator()) {
    lateinit var update:(Project) -> Unit

    fun setUpdateFun(u:(Project) -> Unit ) {
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
        private val startDateView: TextView = itemView.findViewById(R.id.start_date)
        private val endDateView: TextView = itemView.findViewById(R.id.end_date)
        private val rootView: View = itemView.findViewById(R.id.root)

        fun bind(current: Project, adapter: ProjectsNewAdapter) {
            val name = current.name
            val desc = current.desc
            val startDate = current.startDate
            val endDate = current.endDate
            nameView.text = name
            descView.text = desc
            startDateView.text = startDate
            endDateView.text = endDate
            rootView.setOnClickListener {
                adapter.update(current)
            }
        }

        companion object {
            fun create(parent: ViewGroup): ProjectViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_project_new, parent, false)
                return ProjectViewHolder(view)
            }
        }
    }
    class ProjectComparator : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
    }


}
