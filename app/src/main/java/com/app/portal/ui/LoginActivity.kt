package com.app.portal.ui

import com.app.portal.R
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
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
    private lateinit var dotStatus: View
    private lateinit var tvStatusLink: TextView
    private lateinit var layoutIndicator: LinearLayout
    private lateinit var cardLogin: LinearLayout
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var formContainer: LinearLayout
    private lateinit var tvToRegister: TextView
    private lateinit var btnLogoutLink: Button

    private val prefs by lazy { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    private var currentBaseUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupEdgeToEdge()

        tvIntro = findViewById(R.id.tvIntro)
        layoutMain = findViewById(R.id.layoutMain)
        dotStatus = findViewById(R.id.dotStatus)
        tvStatusLink = findViewById(R.id.tvStatusLink)
        layoutIndicator = findViewById(R.id.layoutIndicator)
        cardLogin = findViewById(R.id.cardLogin)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        formContainer = findViewById(R.id.formContainer)
        tvToRegister = findViewById(R.id.tvToRegister)
        btnLogoutLink = findViewById(R.id.btnLogoutLink)

        applyStyles()
        startIntroAnimation()

        // Load URL yang tersimpan
        currentBaseUrl = prefs.getString("base_url", "") ?: ""
        updateUrlState(currentBaseUrl.isNotEmpty())

        layoutIndicator.setOnClickListener {
            showUrlDialog()
        }

        // Action Klik Tombol Logout Link (Kiri Atas)
        btnLogoutLink.setOnClickListener {
            currentBaseUrl = ""
            prefs.edit().remove("base_url").apply()
            updateUrlState(false)
            Toast.makeText(this, "Link server berhasil dikosongkan!", Toast.LENGTH_SHORT).show()
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
        val cardDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#730A101F"))
            setStroke(3, Color.parseColor("#99F59E0B"))
            cornerRadius = 28f * resources.displayMetrics.density
        }
        cardLogin.background = cardDrawable

        val inputBg = {
            GradientDrawable().apply {
                setColor(Color.parseColor("#80020617"))
                setStroke(2, Color.parseColor("#73F59E0B"))
                cornerRadius = 12f * resources.displayMetrics.density
            }
        }
        etUsername.background = inputBg()
        etPassword.background = inputBg()

        val btnDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#f59e0b"), Color.parseColor("#d97706"))
        ).apply {
            cornerRadius = 12f * resources.displayMetrics.density
        }
        btnLogin.background = btnDrawable

        // Style Tombol Logout Link Kiri Atas
        val btnLogoutDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#80020617"))
            setStroke(2, Color.parseColor("#73F59E0B"))
            cornerRadius = 8f * resources.displayMetrics.density
        }
        btnLogoutLink.background = btnLogoutDrawable

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

    private fun showUrlDialog() {
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

    // Action Buttons Container (50:50 Width)
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

    val dialog = AlertDialog.Builder(this)
        .setView(container)
        .create()

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    btnSubmit.setOnClickListener {
        val text = input.text.toString().trim()
        if (text.isNotEmpty()) {
            currentBaseUrl = sanitizeUrl(text)
            prefs.edit().putString("base_url", currentBaseUrl).apply()
            updateUrlState(true)
            Toast.makeText(this, "Link server tersimpan!", Toast.LENGTH_SHORT).show()
        } else {
            currentBaseUrl = ""
            prefs.edit().remove("base_url").apply()
            updateUrlState(false)
            Toast.makeText(this, "Link server berhasil dikosongkan!", Toast.LENGTH_SHORT).show()
        }
        dialog.dismiss()
    }

    btnCancel.setOnClickListener { dialog.dismiss() }
    dialog.show()
}

        dialog.show()
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
            // Tampilkan tombol Logout Link jika link terisi
            btnLogoutLink.visibility = View.VISIBLE

            tvStatusLink.text = "link terisi"
            tvStatusLink.setTextColor(Color.parseColor("#10b981"))
            tvStatusLink.alpha = 1.0f
            formContainer.alpha = 1.0f
            etUsername.isEnabled = true
            etPassword.isEnabled = true
            btnLogin.isEnabled = true
        } else {
            // Sembunyikan tombol Logout Link jika link belum terisi
            btnLogoutLink.visibility = View.GONE

            tvStatusLink.text = "link belum terisi"
            tvStatusLink.setTextColor(Color.parseColor("#ef4444"))
            tvStatusLink.alpha = 0.5f
            formContainer.alpha = 0.4f
            etUsername.isEnabled = false
            etPassword.isEnabled = false
            btnLogin.isEnabled = false
        }
    }

    private fun doLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (currentBaseUrl.isEmpty()) {
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
                val targetUrl = if (currentBaseUrl.endsWith("/api")) "$currentBaseUrl/login_app.php" else "$currentBaseUrl/api/login_app.php"
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
                    put("role", "user")
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
                        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal Login ($code): $responseText", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    Toast.makeText(this, "Error Koneksi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
