package com.example.taskmanager.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
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

//экран личный целей
class PersonalProjectsFragment : Fragment() {
    //кнопка добавить личную цель
    private lateinit var addProjectView: FloatingActionButton

    //список личных целей
    private lateinit var recyclerView: RecyclerView
    private val adapter = PersonalProjectsAdapter()

    //поле ввода для поиска личных целей
    private lateinit var searchView: EditText
    private val searchList = ArrayList<Project>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_personal_projects, container, false)
        addProjectView = root.findViewById(R.id.add_project)
        addProjectView.setOnClickListener {
            val intent = Intent(requireActivity(), AddPersonalProjectActivity::class.java)
            startActivity(intent)
        }
        recyclerView = root.findViewById(R.id.recycler)
        recyclerView.adapter = adapter
        loadProjects()
        searchView = root.findViewById(R.id.search)
        //поиск по списку личных целей если что то введено в поле
        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val text = searchView.text.toString().lowercase(Locale.ROOT)
                if (text.length > 0) {
                    //search
                    searchList.clear()
                    for (project in Repository.currentPersonalProjectList) {
                        val name = project.name
                        if (name.lowercase(Locale.ROOT).startsWith(text)) searchList.add(project)
                    }
                    adapter.submitList(searchList.clone() as List<Project>)
                } else {
                    adapter.submitList(Repository.currentPersonalProjectList.clone() as List<Project>)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //Log.d("mikhael", "i am here")

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //Log.d("mikhael", "i am here")

            }
        })
        //перейти на экран задач в личной цели по нажатию на личную цель
        adapter.setUpdateFun { project ->
            Repository.currentPersonalProject = project
            val intent = Intent(requireActivity(), TaskListPersonalActivity::class.java)
            startActivity(intent)
        }
        if (Repository.isArchive) addProjectView.visibility = View.GONE


        return root
    }

    private fun loadProjects() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore
        val list = ArrayList<Project>()
        db.collection("projects")
            .whereEqualTo("oid", uid)
            .whereEqualTo("is_archive", Repository.isArchive)
            .whereEqualTo("is_personal", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error == null && value != null) {
                    list.clear()
                    for (doc in value.documents) {
                        val project = Project(
                            oid = doc["oid"] as String? ?: "",
                            name = doc["name"] as String? ?: "",
                            desc = doc["desc"] as String? ?: "",
                            startDate = doc["start_date"] as String? ?: "",
                            endDate = doc["end_date"] as String? ?: "",
                            timestamp = doc["timestamp"] as Long? ?: 0L,
                            pid = doc.id,
                        )
                        list.add(project)
                    }
                    Repository.currentPersonalProjectList.clear()
                    Repository.currentPersonalProjectList.addAll(list)
                    adapter.submitList(list.clone() as List<Project>)
                }
            }
    }

}

//адаптер для списка личных целей
class PersonalProjectsAdapter :
    ListAdapter<Project, PersonalProjectsAdapter.ProjectViewHolder>(ProjectComparator()) {
    lateinit var update: (Project) -> Unit

    fun setUpdateFun(u: (Project) -> Unit) {
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

        fun bind(current: Project, adapter: PersonalProjectsAdapter) {
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

