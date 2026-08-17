package com.app.portal.ui

import com.app.portal.R
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
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

class RegisterActivity : Activity() {

    private lateinit var cardRegister: LinearLayout
    private lateinit var etRegUsername: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var spRegRole: Spinner
    private lateinit var btnCancel: Button
    private lateinit var btnRegister: Button
    private lateinit var tvToLogin: TextView

    private val prefs by lazy { getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setupEdgeToEdge()

        cardRegister = findViewById(R.id.cardRegister)
        etRegUsername = findViewById(R.id.etRegUsername)
        etRegPassword = findViewById(R.id.etRegPassword)
        spRegRole = findViewById(R.id.spRegRole)
        btnCancel = findViewById(R.id.btnCancel)
        btnRegister = findViewById(R.id.btnRegister)
        tvToLogin = findViewById(R.id.tvToLogin)

        applyStyles()
        setupSpinner()

        btnCancel.setOnClickListener { finish() }
        tvToLogin.setOnClickListener { finish() }
        btnRegister.setOnClickListener { doRegister() }
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

    private fun applyStyles() {
        val cardDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#730A101F"))
            setStroke(3, Color.parseColor("#99F59E0B"))
            cornerRadius = 28f * resources.displayMetrics.density
        }
        cardRegister.background = cardDrawable

        val inputBg = {
            GradientDrawable().apply {
                setColor(Color.parseColor("#80020617"))
                setStroke(2, Color.parseColor("#73F59E0B"))
                cornerRadius = 12f * resources.displayMetrics.density
            }
        }
        etRegUsername.background = inputBg()
        etRegPassword.background = inputBg()
        spRegRole.background = inputBg()

        btnRegister.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#f59e0b"), Color.parseColor("#d97706"))
        ).apply {
            cornerRadius = 12f * resources.displayMetrics.density
        }

        btnCancel.background = GradientDrawable().apply {
            setColor(Color.parseColor("#B3020617"))
            setStroke(2, Color.parseColor("#73F59E0B"))
            cornerRadius = 12f * resources.displayMetrics.density
        }

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
        val roles = arrayOf("User (Read Only)", "Admin (Full Control)")
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
        spRegRole.adapter = adapter
    }

    private fun doRegister() {
        val baseUrl = prefs.getString("base_url", "") ?: ""
        val username = etRegUsername.text.toString().trim()
        val password = etRegPassword.text.toString().trim()
        val selectedRoleText = spRegRole.selectedItem.toString()

        // Ambil string murni "ADMIN" / "USER" untuk dikirim ke API
        val role = if (selectedRoleText.contains("Admin", ignoreCase = true)) "ADMIN" else "USER"

        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "URL Base Server belum disetting di halaman Login!", Toast.LENGTH_SHORT).show()
            return
        }
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "PROSES..."

        thread {
            try {
                val targetUrl = if (baseUrl.endsWith("/api")) "$baseUrl/register.php" else "$baseUrl/api/register.php"
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
                    put("role", role)
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
                    btnRegister.isEnabled = true
                    btnRegister.text = "DAFTAR"
                    if (code == 200 || code == 201) {
                        Toast.makeText(this, "Pendaftaran Berhasil! Silakan Login.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal Daftar ($code): $responseText", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "DAFTAR"
                    Toast.makeText(this, "Error Koneksi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
