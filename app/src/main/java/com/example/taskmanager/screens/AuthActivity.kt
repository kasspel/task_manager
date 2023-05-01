package com.example.taskmanager.screens

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmanager.R
import com.example.taskmanager.TaskManagerActivity
import com.example.taskmanager.databinding.ActivityAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initFields()
        initFuns()
    }

    private fun initFields() {
        checkAuth()
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { it1 -> firebaseAuthWithGoogle(it1) }
            } catch (e: ApiException) {
                Toast.makeText(this, "auth === ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initFuns() {
        binding.authBtn.setOnClickListener {
            signInWithGoogle()
        }
        val termView = binding.term
        val termText = getString(R.string.terms_use)
        setTextViewHTML(termView, termText)
    }

    private fun getClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInClient = getClient()
        launcher.launch(signInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(token: String) {
        val credential = GoogleAuthProvider.getCredential(token, null)
        Firebase.auth.signInWithCredential(credential).addOnSuccessListener {
            setUserInToFirebase()
            startActivity(Intent(this, TaskManagerActivity::class.java))
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAuth() {
        if (Firebase.auth.currentUser != null) {
            setUserInToFirebase()
            startActivity(Intent(this, TaskManagerActivity::class.java))
            finish()
        }
    }

    private fun setUserInToFirebase(){
        val a = Firebase.auth ?: return
        val db = Firebase.firestore
        val email = a.currentUser?.email?: return
        val uid = a.currentUser?.uid?: return
        val data = hashMapOf(
            "email" to email,
        )
        db.collection("users").document(uid).set(data, SetOptions.merge())
    }

    protected fun makeLinkClickable(strBuilder: SpannableStringBuilder, span: URLSpan?) {
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val clickable: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                // Do something with span.getURL() to handle the link click...
                Log.d("anna", span?.url?:"null")
                val url = when(span?.url?:"null"){
                    "#1" -> "file:///android_res/raw/polz.html"
                    "#2" -> "file:///android_res/raw/polit.html"
                    else -> ""
                }
                if(url.isNotEmpty()) {
                    val intent = Intent(this@AuthActivity, WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)
                }
            }
        }
        strBuilder.setSpan(clickable, start, end, flags)
        strBuilder.removeSpan(span)
    }
    protected fun setTextViewHTML(text: TextView, html: String?) {
        val sequence: CharSequence = Html.fromHtml(html)
        val strBuilder = SpannableStringBuilder(sequence)
        val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
        for (span in urls) {
            makeLinkClickable(strBuilder, span)
        }
        text.text = strBuilder
        text.movementMethod = LinkMovementMethod.getInstance()
    }
 }