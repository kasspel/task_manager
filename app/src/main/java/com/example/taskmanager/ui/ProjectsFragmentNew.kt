package com.example.taskmanager.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.taskmanager.R
import com.example.taskmanager.TaskManagerActivity
import com.example.taskmanager.repo.Repository

//фрагмент для переключения между проектами и личными целями
class ProjectsFragment : Fragment() {
    private lateinit var viewPagerView: ViewPager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root =  inflater.inflate(R.layout.fragment_projects_new, container, false)
        viewPagerView = root.findViewById(R.id.view_pager)
        val adapter = ViewPagerFragmentStateAdapter(childFragmentManager)
        viewPagerView.adapter = adapter

        viewPagerView.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
             override fun onPageScrollStateChanged(state: Int) {
             }
             override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
             }
             override fun onPageSelected(position: Int) {
                 val activity = requireActivity() as TaskManagerActivity
                 activity.setTitle(when(position){
                     0 -> if(Repository.isArchive) "Архив проектов" else "Проекты"
                     1 -> if(Repository.isArchive) "Архив личных целей" else "Личные цели"
                     else -> if(Repository.isArchive) "Архив проектов" else "Проекты"
                 })
             }
         })

        viewPagerView.setCurrentItem(0)
        return root
    }

}

class ViewPagerFragmentStateAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        return when(position){
            0 -> GroupProjectsFragment()
            1 -> PersonalProjectsFragment()
            else -> GroupProjectsFragment()
        }
    }
}