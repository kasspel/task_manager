package com.example.taskmanager.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.taskmanager.R
import com.example.taskmanager.models.User
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.squareup.picasso.Picasso
import java.io.File
import androidx.lifecycle.ViewModelProviders
import com.example.taskmanager.repo.Repository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

//import com.nabinbhandari.android.permissions.PermissionHandler
//import com.nabinbhandari.android.permissions.Permissions

//экран профиля пользователя
class ProfileFragment : Fragment() {
    val RESULT_LOAD_IMAGE = 1122
    //фотография пользователя
    private lateinit var photoView: ImageView
    //кнопка задания фотографии пользователя
    private lateinit var changePhotoView: ImageView
    private lateinit var viewModel: ProfileViewModel
    //индикатор загрузки фотографии пользователя
    private lateinit var progressLoadingPhoto: ProgressBar

    //имя пользователя
    private lateinit var nameView: EditText
    //почта пользователя
    private lateinit var emailView: EditText
    //телефон пользователя
    private lateinit var phoneView: EditText

    //кнопка сохранения профиля
    private lateinit var saveView: Button
    //индикатор сохранения
    private lateinit var progressSaveBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_profile_new, container, false)
        photoView = root.findViewById(R.id.photo)
        changePhotoView = root.findViewById(R.id.change_photo)
        progressLoadingPhoto = root.findViewById(R.id.progressBar)
        viewModel = ViewModelProviders.of(this).get(ProfileViewModel::class.java)

        nameView = root.findViewById(R.id.name)
        emailView = root.findViewById(R.id.email)
        phoneView = root.findViewById(R.id.phone)
        saveView = root.findViewById(R.id.save)
        progressSaveBar = root.findViewById(R.id.progress_save)

        nameView.setText(Repository.user?.name?:"")
        emailView.setText(Repository.user?.email?:"")
        phoneView.setText(Repository.user?.phone?:"")
        val photoPath = Repository.user?.photo?: ""
        if(photoPath.isNotEmpty()){
            Picasso.get().load(photoPath).into(photoView)
        }
        val fcm_token = Repository.user?.fcm_token ?: ""
        if(fcm_token.isEmpty()) saveFcmToken()
        saveView.setOnClickListener {
            val user = User(
                uid = "",
                name = nameView.text.toString(),
                phone = phoneView.text.toString(),
                email = emailView.text.toString(),
                photo = "",
                fcm_token = Repository.user?.fcm_token ?: ""
            )
            viewModel.save(user)
        }

//обработчик смены фотографии
        changePhotoView.setOnClickListener {
            Dexter.withContext(activity)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(i, RESULT_LOAD_IMAGE)
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {}

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {}
                }).check()
        }
        //показать индикатор загрузки фото
        viewModel.showLoadingAvatar.observe(
            viewLifecycleOwner
        ) { progressLoadingPhoto.visibility = if (it) View.VISIBLE else View.GONE }
//показать процесс загрузки фото
        viewModel.progress.observe(
            viewLifecycleOwner
        ) { progressSaveBar.visibility = if (it) View.VISIBLE else View.GONE }

        viewModel.persentLoadingAvatar.observe(
            viewLifecycleOwner
        ) {
            val persent = it.toInt()
            progressLoadingPhoto.progress = persent
        }

        return root
    }
    //загрузить фото пользователя в облако после его выбора
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            val selectedImage: Uri? = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? =
                activity?.contentResolver?.query(selectedImage!!, filePathColumn, null, null, null)
            if(cursor == null) return
            cursor.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            val picturePath: String = cursor.getString(columnIndex)
            cursor.close()
            //image.setImageBitmap(BitmapFactory.decodeFile(picturePath))
            Log.v("AH", picturePath)
            Picasso.get().load(File(picturePath)).into(photoView)
            viewModel.saveAvatar(picturePath)
        }
    }
    //сохранить токен для пушей
    private fun saveFcmToken(){
        val db = Firebase.firestore
        val uid = Repository.user?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("mikhael", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            val data = hashMapOf(
                "fcm_token" to token,
            )
            db.collection("users").document(uid).update(data as Map<String, Any>)
            // Log and toast
            //Toast.makeText(requireActivity(), "fcm token получен", Toast.LENGTH_SHORT).show()
        })
    }
}