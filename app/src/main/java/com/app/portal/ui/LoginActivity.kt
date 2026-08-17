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
import android.view.ViewGroup
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
    private var animatorSet: AnimatorSet? = null

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
        val btnClearLink = findViewById<Button>(R.id.btnClearLink)
        val layoutIndicator = findViewById<View>(R.id.layoutIndicator)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        enforceCardWidth(cardLogin)

        cardLogin.background = getStyleDrawable("#800F172A", "#F59E0B", 2, 24f)
        etUsername.background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        etPassword.background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        btnLogin.background = getStyleDrawable("", "#F59E0B", 0, 10f, isGradientOrange = true)
        btnClearLink.background = getStyleDrawable("#0F172A", "#EF4444", 1, 8f)

        updateUrlState(currentBaseUrl.isNotEmpty())

        layoutIndicator.setOnClickListener { showUrlDialog() }

        tvToRegister.setOnClickListener {
            if (currentBaseUrl.isNotEmpty()) {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }

        btnClearLink.setOnClickListener {
            currentBaseUrl = ""
            prefs.edit().remove("base_url").remove("user_role").apply()
            updateUrlState(false)
            Toast.makeText(this, "Link Server berhasil dikosongkan", Toast.LENGTH_SHORT).show()
        }

        btnLogin.setOnClickListener {
            if (currentBaseUrl.isEmpty()) return@setOnClickListener

            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(username, password)
        }

        tvIntro.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            
            val fadeOutIntro = ObjectAnimator.ofFloat(tvIntro, View.ALPHA, 1f, 0f).apply { duration = 400 }
            val fadeInMain = ObjectAnimator.ofFloat(layoutMain, View.ALPHA, 0f, 1f).apply { duration = 500 }
            
            fadeOutIntro.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvIntro.visibility = View.GONE
                    layoutMain.visibility = View.VISIBLE
                }
            })

            animatorSet = AnimatorSet().apply {
                play(fadeOutIntro).before(fadeInMain)
                start()
            }
        }, 1200)
    }

    private fun enforceCardWidth(cardView: View) {
        val displayMetrics = resources.displayMetrics
        val maxWidthPx = (400 * displayMetrics.density).toInt()
        val screenWidthPx = displayMetrics.widthPixels
        
        val targetWidth = if (screenWidthPx > maxWidthPx) maxWidthPx else ViewGroup.LayoutParams.MATCH_PARENT
        val params = cardView.layoutParams
        params.width = targetWidth
        cardView.layoutParams = params
    }

    override fun onDestroy() {
        super.onDestroy()
        animatorSet?.cancel()
    }

    private fun updateUrlState(hasUrl: Boolean) {
        val dotStatus = findViewById<View>(R.id.dotStatus)
        val tvStatusLink = findViewById<TextView>(R.id.tvStatusLink)
        val btnClearLink = findViewById<Button>(R.id.btnClearLink)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(if (hasUrl) "#22c55e" else "#ef4444"))
        }
        dotStatus.background = dotDrawable

        if (hasUrl) {
            tvStatusLink.text = "link terhubung"
            tvStatusLink.setTextColor(Color.parseColor("#22c55e"))
            btnClearLink.visibility = View.VISIBLE

            etUsername.isEnabled = true
            etPassword.isEnabled = true
            btnLogin.isEnabled = true
            tvToRegister.isEnabled = true

            etUsername.alpha = 1.0f
            etPassword.alpha = 1.0f
            btnLogin.alpha = 1.0f
            tvToRegister.alpha = 1.0f
        } else {
            tvStatusLink.text = "link belum terisi"
            tvStatusLink.setTextColor(Color.parseColor("#ef4444"))
            btnClearLink.visibility = View.GONE

            etUsername.isEnabled = false
            etPassword.isEnabled = false
            btnLogin.isEnabled = false
            tvToRegister.isEnabled = false

            etUsername.alpha = 0.4f
            etPassword.alpha = 0.4f
            btnLogin.alpha = 0.4f
            tvToRegister.alpha = 0.4f
        }
    }

    private fun showUrlDialog() {
        val density = resources.displayMetrics.density

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * density).toInt(), 0, (24 * density).toInt(), 0)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
            background = getStyleDrawable("#800F172A", "#F59E0B", 2, 20f)
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
            setPadding(0, (4 * density).toInt(), 0, (16 * density).toInt())
        }
        container.addView(tvSub)

        val input = EditText(this).apply {
            hint = "https://namadomain.com"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#475569"))
            setText(currentBaseUrl)
            setSingleLine(true)
            setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        }
        container.addView(input)

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (20 * density).toInt(), 0, 0)
        }

        val btnHeight = (42 * density).toInt()

        val btnCancel = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(0, 0, (6 * density).toInt(), 0)
            }
            text = "BATAL"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            isAllCaps = true
            background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        }

        val btnSubmit = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins((6 * density).toInt(), 0, 0, 0)
            }
            text = "SIMPAN"
            setTextColor(Color.parseColor("#020617"))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            isAllCaps = true
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
            setDimAmount(0.3f)
        }

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
        val jsonPayload = JSONObject().apply {
            put("username", u)
            put("password", p)
        }

        val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(endpoint).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    Toast.makeText(this@LoginActivity, "Gagal terhubung ke server!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resString = response.body?.string() ?: ""
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    btnLogin.isEnabled = true
                    btnLogin.text = "MASUK"
                    if (response.isSuccessful) {
                        try {
                            val jsonRes = JSONObject(resString)
                            var rawRole = ""

                            if (jsonRes.has("role")) rawRole = jsonRes.optString("role", "")
                            if (rawRole.isEmpty() && jsonRes.has("user")) {
                                val userObj = jsonRes.optJSONObject("user")
                                rawRole = userObj?.optString("role", userObj.optString("Role", "")) ?: ""
                            }
                            if (rawRole.isEmpty() && jsonRes.has("data")) {
                                val dataObj = jsonRes.optJSONObject("data")
                                rawRole = dataObj?.optString("role", dataObj.optString("Role", "")) ?: ""
                            }

                            val cleanRole = if (rawRole.trim().equals("ADMIN", ignoreCase = true)) "ADMIN" else "USER"

                            prefs.edit()
                                .remove("role")
                                .putString("user_role", cleanRole)
                                .apply()

                        } catch (e: Exception) {
                            prefs.edit().putString("user_role", "USER").apply()
                        }

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
