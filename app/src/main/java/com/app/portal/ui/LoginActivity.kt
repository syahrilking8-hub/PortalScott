package com.app.portal.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.portal.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var currentBaseUrl: String = ""
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        currentBaseUrl = prefs.getString("base_url", "") ?: ""

        val tvIntro = findViewById<TextView>(R.id.tvIntro)
        val layoutMain = findViewById<View>(R.id.layoutMain)
        val cardLogin = findViewById<View>(R.id.cardLogin)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnLogoutLink = findViewById<Button>(R.id.btnLogoutLink)
        val layoutIndicator = findViewById<View>(R.id.layoutIndicator)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        // Terapkan Styling Background Programmatic
        cardLogin.background = getStyleDrawable("#0F172A", "#F59E0B", 2, 24f)
        etUsername.background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        etPassword.background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        btnLogin.background = getStyleDrawable("", "#F59E0B", 0, 10f, isGradientOrange = true)
        btnLogoutLink.background = getStyleDrawable("#0F172A", "#F59E0B", 1, 8f)

        updateUrlState(currentBaseUrl.isNotEmpty())

        layoutIndicator.setOnClickListener { showUrlDialog() }
        tvToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogoutLink.setOnClickListener {
            currentBaseUrl = ""
            prefs.edit().remove("base_url").apply()
            updateUrlState(false)
            Toast.makeText(this, "Link Server dihapus", Toast.LENGTH_SHORT).show()
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentBaseUrl.isEmpty()) {
                Toast.makeText(this, "Harap set URL Server terlebih dahulu!", Toast.LENGTH_SHORT).show()
                showUrlDialog()
                return@setOnClickListener
            }

            doLogin(username, password)
        }

        // Intro Animation
        tvIntro.postDelayed({
            val fadeOutIntro = ObjectAnimator.ofFloat(tvIntro, View.ALPHA, 1f, 0f).apply { duration = 400 }
            val fadeInMain = ObjectAnimator.ofFloat(layoutMain, View.ALPHA, 0f, 1f).apply { duration = 500 }
            
            fadeOutIntro.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvIntro.visibility = View.GONE
                    layoutMain.visibility = View.VISIBLE
                }
            })

            AnimatorSet().apply {
                play(fadeOutIntro).before(fadeInMain)
                start()
            }
        }, 1200)
    }

    private fun updateUrlState(hasUrl: Boolean) {
        val dotStatus = findViewById<View>(R.id.dotStatus)
        val tvStatusLink = findViewById<TextView>(R.id.tvStatusLink)
        val btnLogoutLink = findViewById<Button>(R.id.btnLogoutLink)

        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(if (hasUrl) "#22c55e" else "#ef4444"))
        }
        dotStatus.background = dotDrawable

        if (hasUrl) {
            tvStatusLink.text = "link terhubung"
            tvStatusLink.setTextColor(Color.parseColor("#22c55e"))
            btnLogoutLink.visibility = View.VISIBLE
        } else {
            tvStatusLink.text = "link belum terisi"
            tvStatusLink.setTextColor(Color.parseColor("#ef4444"))
            btnLogoutLink.visibility = View.GONE
        }
    }

    private fun showUrlDialog() {
    // Root container transparan agar overlay dialog tidak berwarna solid
    val rootLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(40, 0, 40, 0)
    }

    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(48, 48, 48, 48)
        background = getStyleDrawable("#0F172A", "#F59E0B", 2, 20f)
    }

    val tvTitle = TextView(this).apply {
        text = "Set URL Server"
        setTextColor(Color.parseColor("#F8FAFC"))
        textSize = 18f
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
    }
    container.addView(tvTitle)

    val tvSub = TextView(this).apply {
        text = "isi link pada kolom di bawah ini"
        setTextColor(Color.parseColor("#F59E0B"))
        textSize = 12f
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(0, 6, 0, 20)
    }
    container.addView(tvSub)

    val input = EditText(this).apply {
        hint = "https://namadomain.com"
        setTextColor(Color.WHITE)
        setHintTextColor(Color.parseColor("#475569"))
        setText(currentBaseUrl)
        setSingleLine(true)
        setPadding(30, 24, 30, 24)
        background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
    }
    container.addView(input)

    val buttonLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 24, 0, 0)
    }

    val btnCancel = Button(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, 120, 1f).apply { setMargins(0, 0, 12, 0) }
        text = "Batal"
        setTextColor(Color.parseColor("#F8FAFC"))
        textSize = 13f
        setTypeface(null, android.graphics.Typeface.BOLD)
        isAllCaps = false
        background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
    }

    val btnSubmit = Button(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, 120, 1f).apply { setMargins(12, 0, 0, 0) }
        text = "Simpan"
        setTextColor(Color.parseColor("#020617"))
        textSize = 13f
        setTypeface(null, android.graphics.Typeface.BOLD)
        isAllCaps = false
        background = getStyleDrawable("", "#F59E0B", 0, 8f, isGradientOrange = true)
    }

    buttonLayout.addView(btnCancel)
    buttonLayout.addView(btnSubmit)
    container.addView(buttonLayout)
    rootLayout.addView(container)

    val dialog = AlertDialog.Builder(this)
        .setView(rootLayout)
        .create()

    dialog.window?.apply {
        setBackgroundDrawableResource(android.R.color.transparent)
        // Hilangkan background dim bawaan Android agar transparan seperti web
        setDimAmount(0.4f)
    }

    btnSubmit.setOnClickListener {
        val text = input.text.toString().trim()
        if (text.isNotEmpty()) {
            currentBaseUrl = sanitizeUrl(text)
            // Simpan secara sinkron menggunakan commit()
            prefs.edit().putString("base_url", currentBaseUrl).commit()
            updateUrlState(true)
            Toast.makeText(this, "Link server tersimpan!", Toast.LENGTH_SHORT).show()
        } else {
            currentBaseUrl = ""
            prefs.edit().remove("base_url").commit()
            updateUrlState(false)
            Toast.makeText(this, "Link server dikosongkan!", Toast.LENGTH_SHORT).show()
        }
        dialog.dismiss()
    }

    btnCancel.setOnClickListener { dialog.dismiss() }
    dialog.show()
}

    private fun doLogin(u: String, p: String) {
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.isEnabled = false
        btnLogin.text = "Memuat..."

        val endpoint = "${currentBaseUrl}/login"
        val jsonPayload = JSONObject()
        jsonPayload.put("username", u as Any)
        jsonPayload.put("password", p as Any)

        val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(endpoint).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    Toast.makeText(this@LoginActivity, "Gagal terhubung ke server!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resString = response.body?.string() ?: ""
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    if (response.isSuccessful) {
                        Toast.makeText(this@LoginActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Username/Password Salah!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun sanitizeUrl(url: String): String {
        var formatted = url.lowercase()
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "https://$formatted"
        }
        return formatted.trimEnd('/')
    }

    private fun getStyleDrawable(
        bgColorHex: String,
        borderColorHex: String,
        borderWidthDp: Int,
        cornerRadiusDp: Float,
        isGradientOrange: Boolean = false
    ): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density

            if (isGradientOrange) {
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(Color.parseColor("#F59E0B"), Color.parseColor("#D97706"))
            } else {
                setColor(Color.parseColor(bgColorHex))
            }

            if (borderColorHex.isNotEmpty() && borderWidthDp > 0) {
                setStroke((borderWidthDp * density).toInt(), Color.parseColor(borderColorHex))
            }
        }
    }
}
