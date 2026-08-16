package com.app.portal.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LoginActivity : Activity() {

    private lateinit var tvIntro: TextView
    private lateinit var layoutMain: View
    private lateinit var btnResetLink: Button
    private lateinit var dotStatus: View
    private lateinit var tvStatusLink: TextView
    private lateinit var cardLogin: LinearLayout
    private lateinit var etBaseUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var spRole: Spinner
    private lateinit var btnLogin: Button
    private lateinit var formContainer: LinearLayout
    private lateinit var tvToRegister: TextView

    private val prefs by lazy { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupEdgeToEdge()

        tvIntro = findViewById(R.id.tvIntro)
        layoutMain = findViewById(R.id.layoutMain)
        btnResetLink = findViewById(R.id.btnResetLink)
        dotStatus = findViewById(R.id.dotStatus)
        tvStatusLink = findViewById(R.id.tvStatusLink)
        cardLogin = findViewById(R.id.cardLogin)
        etBaseUrl = findViewById(R.id.etBaseUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        spRole = findViewById(R.id.spRole)
        btnLogin = findViewById(R.id.btnLogin)
        formContainer = findViewById(R.id.formContainer)
        tvToRegister = findViewById(R.id.tvToRegister)

        applyStyles()
        setupSpinner()
        startIntroAnimation()

        val savedUrl = prefs.getString("base_url", "") ?: ""
        if (savedUrl.isNotEmpty()) {
            etBaseUrl.setText(savedUrl)
            updateUrlState(true)
        } else {
            updateUrlState(false)
        }

        etBaseUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim()
                if (input.isNotEmpty()) {
                    val clean = sanitizeUrl(input)
                    prefs.edit().putString("base_url", clean).apply()
                    updateUrlState(true)
                } else {
                    prefs.edit().remove("base_url").apply()
                    updateUrlState(false)
                }
            }
        })

        btnResetLink.setOnClickListener {
            etBaseUrl.setText("")
            prefs.edit().remove("base_url").apply()
            updateUrlState(false)
        }

        btnLogin.setOnClickListener {
            doLogin()
        }

        tvToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    private fun sanitizeUrl(url: String): String {
        return url.trim().trimEnd('/')
    }

    private fun applyStyles() {
        // Card Glowing Pulse Background
        val cardDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#730A101F")) // rgba(10, 16, 31, 0.45)
            setStroke(3, Color.parseColor("#99F59E0B")) // rgba(245, 158, 11, 0.6)
            cornerRadius = 28f * resources.displayMetrics.density
        }
        cardLogin.background = cardDrawable

        // Input Fields Background Style
        val inputBg = {
            GradientDrawable().apply {
                setColor(Color.parseColor("#80020617"))
                setStroke(2, Color.parseColor("#73F59E0B"))
                cornerRadius = 12f * resources.displayMetrics.density
            }
        }
        etBaseUrl.background = inputBg()
        etUsername.background = inputBg()
        etPassword.background = inputBg()
        spRole.background = inputBg()

        // Primary Button Background
        val btnDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#f59e0b"), Color.parseColor("#d97706"))
        ).apply {
            cornerRadius = 12f * resources.displayMetrics.density
        }
        btnLogin.background = btnDrawable

        // Reset Link Outline Button
        val resetBtnDrawable = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(2, Color.parseColor("#fbbf24"))
            cornerRadius = 8f * resources.displayMetrics.density
        }
        btnResetLink.background = resetBtnDrawable

        // Card Glow Pulse Animation
        val glowAnim = ValueAnimator.ofFloat(0.4f, 0.9f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val alphaVal = (anim.animatedValue as Float * 255).toInt()
                val colorWithAlpha = Color.argb(alphaVal, 245, 158, 11)
                cardDrawable.setStroke(3, colorWithAlpha)
            }
        }
        glowAnim.start()
    }

    private fun setupSpinner() {
        val roles = arrayOf("Admin (Full Control)", "User (Read Only)")
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, roles) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.parseColor("#f8fafc"))
                v.textSize = 14f
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(Color.parseColor("#f8fafc"))
                v.setBackgroundColor(Color.parseColor("#0f172a"))
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = adapter
    }

    private fun startIntroAnimation() {
        tvIntro.alpha = 0f
        tvIntro.animate().alpha(1f).setDuration(1200).withEndAction {
            Handler(Looper.getMainLooper()).postDelayed({
                tvIntro.animate().alpha(0f).setDuration(800).withEndAction {
                    tvIntro.visibility = View.GONE
                    layoutMain.visibility = View.VISIBLE
                    layoutMain.animate().alpha(1f).setDuration(800).start()
                }.start()
            }, 1000)
        }.start()
    }

    private fun updateUrlState(isFilled: Boolean) {
        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (isFilled) Color.parseColor("#10b981") else Color.parseColor("#ef4444"))
        }
        dotStatus.background = dotDrawable

        if (isFilled) {
            tvStatusLink.text = "link terisi"
            tvStatusLink.setTextColor(Color.parseColor("#10b981"))
            btnResetLink.visibility = View.VISIBLE
            formContainer.alpha = 1.0f
            etUsername.isEnabled = true
            etPassword.isEnabled = true
            spRole.isEnabled = true
            btnLogin.isEnabled = true
        } else {
            tvStatusLink.text = "link belum terisi"
            tvStatusLink.setTextColor(Color.parseColor("#ef4444"))
            btnResetLink.visibility = View.GONE
            formContainer.alpha = 0.4f
            etUsername.isEnabled = false
            etPassword.isEnabled = false
            spRole.isEnabled = false
            btnLogin.isEnabled = false
        }
    }

    private fun doLogin() {
        val baseUrl = sanitizeUrl(etBaseUrl.text.toString())
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "URL Base Server belum terisi!", Toast.LENGTH_SHORT).show()
            return
        }
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username & Password wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "MEMPROSES..."

        thread {
            try {
                val targetUrl = if (baseUrl.endsWith("/api")) "$baseUrl/login_app.php" else "$baseUrl/api/login_app.php"
                val url = URL(targetUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()

                val code = conn.responseCode
                val responseText = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    if (code == 200) {
                        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Gagal Login ($code): $responseText", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    Toast.makeText(this, "Error Koneski: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
