package com.example.taskmanager.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.taskmanager.R
import com.example.taskmanager.screens.WebViewActivity

//фрагмент для показа информации о приложении
class AboutUsFragment : Fragment() {

    private lateinit var useView: TextView
    private lateinit var politView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_about_us, container, false)

        //ссылка на пользовательское соглашение
        useView = root.findViewById(R.id.use)
        //ссылка на политику конфидециальности
        politView = root.findViewById(R.id.polit)

        useView.setOnClickListener {
            //показать пользовательское соглашение на отдельной активити как веб страницу
            val intent = Intent(requireActivity(), WebViewActivity::class.java)
            intent.putExtra("url", "file:///android_res/raw/polz.html")
            startActivity(intent)

        }

        politView.setOnClickListener {
            //показать политику конфедециальности на отдельной активити как веб страницу
            val intent = Intent(requireActivity(), WebViewActivity::class.java)
            intent.putExtra("url", "file:///android_res/raw/polit.html")
            startActivity(intent)

        }

        return root
    }
}