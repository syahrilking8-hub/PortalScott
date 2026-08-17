package com.app.portal.ui

import com.app.portal.R

import com.app.portal.DynamicRetrofitClient
import com.app.portal.Student
import com.app.portal.databinding.ActivityMainBinding

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

import coil.load
import coil.transform.CircleCropTransformation

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private var selectedDate: String = ""
    private var tvFileNameRef: TextView? = null
    private var baseUrl: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            tvFileNameRef?.text = getFileName(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // PERBAIKAN: Samakan nama file prefs dengan LoginActivity ("app_settings")
    val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    baseUrl = prefs.getString("base_url", "") ?: ""

    if (baseUrl.isEmpty()) {
        Toast.makeText(this, "URL Server belum diset! Silakan login ulang.", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        return
    }

        applyStyles()

        binding.btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnAddStudent.setOnClickListener {
            showFormDialog(null)
        }

        fetchDataStudents()
    }

    // Generator Drawable Dinamis (Mengatur Warna, Border, Radius, Gradient secara terpusat)
    private fun getStyleDrawable(
        bgColor: String,
        strokeColor: String? = null,
        strokeWidthDp: Int = 0,
        radiusDp: Float = 8f,
        isGradientOrange: Boolean = false,
        isCircle: Boolean = false
    ): GradientDrawable {
        return GradientDrawable().apply {
            if (isCircle) {
                shape = GradientDrawable.OVAL
            } else {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusDp * resources.displayMetrics.density
            }

            if (isGradientOrange) {
                orientation = GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(Color.parseColor("#F59E0B"), Color.parseColor("#D97706"))
            } else {
                setColor(Color.parseColor(bgColor))
            }

            strokeColor?.let {
                setStroke((strokeWidthDp * resources.displayMetrics.density).toInt(), Color.parseColor(it))
            }
        }
    }

    private fun applyStyles() {
        binding.btnLogout.background = getStyleDrawable("#80020617", "#73F59E0B", 2, 8f)
        binding.btnAddStudent.background = getStyleDrawable("", null, 0, 8f, isGradientOrange = true)

        val cardDrawable = getStyleDrawable("#730A101F", "#99F59E0B", 3, 20f)
        binding.cardTable.background = cardDrawable

        val glowAnim = ValueAnimator.ofFloat(0.4f, 0.9f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val alphaVal = (anim.animatedValue as Float * 255).toInt()
                val colorWithAlpha = Color.argb(alphaVal, 245, 158, 11)
                cardDrawable.setStroke((3 * resources.displayMetrics.density).toInt(), colorWithAlpha)
            }
        }
        glowAnim.start()
    }

    private fun fetchDataStudents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)
                val response = api.getStudents()
                withContext(Dispatchers.Main) {
                    populateTable(response.data ?: emptyList())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateTable(students: List<Student>) {
        val childCount = binding.tableStudents.childCount
        if (childCount > 1) {
            binding.tableStudents.removeViews(1, childCount - 1)
        }

        students.forEach { student ->
            val tableRow = TableRow(this).apply {
                setPadding(0, 0, 0, 0)
            }

            tableRow.addView(createTableCell(student.nama, false))
            tableRow.addView(createTableCell(student.alamat, false))

            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(12, 10, 12, 10)
                background = getStyleDrawable("#00000000", "#1E293B", 1, 0f)
            }

            // Tombol Detail (Transparan Biru Pekat, Stroke Cyan)
            val btnDetail = Button(this).apply {
                text = "Detail"
                textSize = 11f
                setTextColor(Color.parseColor("#38BDF8"))
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((62 * resources.displayMetrics.density).toInt(), (32 * resources.displayMetrics.density).toInt())
                background = getStyleDrawable("#1E293B", "#38BDF8", 2, 6f)
                setOnClickListener { showDetailDialog(student) }
            }

            // Tombol Edit (Kuning Oranye Solid, Teks Hitam Pekat)
            val btnEdit = Button(this).apply {
                text = "Edit"
                textSize = 11f
                setTextColor(Color.parseColor("#020617"))
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((54 * resources.displayMetrics.density).toInt(), (32 * resources.displayMetrics.density).toInt()).apply {
                    setMargins(8, 0, 8, 0)
                }
                background = getStyleDrawable("", null, 0, 6f, isGradientOrange = true)
                setOnClickListener { showFormDialog(student) }
            }

            // Tombol Hapus (Transparan Merah Pekat, Stroke Merah Cerah)
            val btnDelete = Button(this).apply {
                text = "Hapus"
                textSize = 11f
                setTextColor(Color.parseColor("#EF4444"))
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((58 * resources.displayMetrics.density).toInt(), (32 * resources.displayMetrics.density).toInt())
                background = getStyleDrawable("#1E293B", "#EF4444", 2, 6f)
                setOnClickListener { deleteData(student.id) }
            }

            actionLayout.addView(btnDetail)
            actionLayout.addView(btnEdit)
            actionLayout.addView(btnDelete)
            tableRow.addView(actionLayout)

            binding.tableStudents.addView(tableRow)
        }
    }

    private fun createTableCell(text: String, isHeader: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(if (isHeader) Color.parseColor("#F59E0B") else Color.parseColor("#F8FAFC"))
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            background = getStyleDrawable("#00000000", "#1E293B", 1, 0f)
        }
    }

    private fun showFormDialog(student: Student?) {
        selectedImageUri = null
        selectedDate = ""

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_form, null)
        
        // Pemasangan Dynamic Styling Form Dialog
        dialogView.findViewById<View>(R.id.cardDialogForm).background = getStyleDrawable("#0F172A", "#F59E0B", 2, 20f)
        dialogView.findViewById<View>(R.id.etNis).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etNama).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etAlamat).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.tvDatePicker).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.containerInputFile).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.btnPickFile).background = getStyleDrawable("", null, 0, 6f, isGradientOrange = true)
        dialogView.findViewById<View>(R.id.btnCancel).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.btnSubmit).background = getStyleDrawable("", null, 0, 8f, isGradientOrange = true)

        val tvFormTitle = dialogView.findViewById<TextView>(R.id.tvFormTitle)
        val etNis = dialogView.findViewById<EditText>(R.id.etNis)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etAlamat = dialogView.findViewById<EditText>(R.id.etAlamat)
        val tvDatePicker = dialogView.findViewById<TextView>(R.id.tvDatePicker)
        val btnPickFile = dialogView.findViewById<Button>(R.id.btnPickFile)
        val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        tvFileNameRef = tvFileName

        if (student != null) {
            tvFormTitle.text = "Edit Data Siswa"
            etNis.setText(student.nis)
            etNama.setText(student.nama)
            etAlamat.setText(student.alamat)
        } else {
            tvFormTitle.text = "Tambah Data Siswa"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        tvDatePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "$year-${month + 1}-$day"
                tvDatePicker.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickFile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            val nisStr = etNis.text.toString()
            val namaStr = etNama.text.toString()
            val alamatStr = etAlamat.text.toString()

            if (nisStr.isEmpty() || namaStr.isEmpty()) {
                Toast.makeText(this, "NIS dan Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveData(student?.id, nisStr, namaStr, alamatStr, dialog)
        }

        dialog.show()
    }

    private fun saveData(id: String?, nis: String, nama: String, alamat: String, dialog: AlertDialog) {
        val rbNis = RequestBody.create("text/plain".toMediaTypeOrNull(), nis)
        val rbNama = RequestBody.create("text/plain".toMediaTypeOrNull(), nama)
        val rbAlamat = RequestBody.create("text/plain".toMediaTypeOrNull(), alamat)
        val rbTgl = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedDate)

        var photoPart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)
            if (file != null) {
                val reqFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                photoPart = MultipartBody.Part.createFormData("foto", file.name, reqFile)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)
                val response = if (id == null) {
                    api.addStudent(rbNis, rbNama, rbAlamat, rbTgl, photoPart)
                } else {
                    val rbId = RequestBody.create("text/plain".toMediaTypeOrNull(), id)
                    api.updateStudent(rbId, rbNis, rbNama, rbAlamat, rbTgl, photoPart)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchDataStudents()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDetailDialog(student: Student) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_detail, null)

        // Pemasangan Dynamic Styling Detail Dialog
        dialogView.findViewById<View>(R.id.cardDialogDetail).background = getStyleDrawable("#0F172A", "#F59E0B", 2, 20f)
        dialogView.findViewById<View>(R.id.vAvatarGlow).background = getStyleDrawable("#00000000", "#F59E0B", 3, 0f, isCircle = true)
        dialogView.findViewById<View>(R.id.subcardBiodata).background = getStyleDrawable("#0B132B", "#1E293B", 1, 12f)
        dialogView.findViewById<View>(R.id.btnCloseDetail).background = getStyleDrawable("", null, 0, 8f, isGradientOrange = true)

        val ivFoto = dialogView.findViewById<ImageView>(R.id.ivDetailFoto)
        val tvNama = dialogView.findViewById<TextView>(R.id.tvDetailNama)
        val tvNis = dialogView.findViewById<TextView>(R.id.tvDetailNis)
        val tvAlamat = dialogView.findViewById<TextView>(R.id.tvDetailAlamat)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        tvNama.text = ": ${student.nama}"
        tvNis.text = ": ${student.nis}"
        tvAlamat.text = ": ${student.alamat}"

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val photoUrl = "${cleanUrl}public/uploads/${student.foto}"

        ivFoto.load(photoUrl) {
            transformations(CircleCropTransformation())
            error(android.R.drawable.ic_menu_report_image)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun deleteData(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)
                val response = api.deleteStudent(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                    fetchDataStudents()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal hapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val fileName = getFileName(uri) ?: "temp_image.jpg"
        val tempFile = File(cacheDir, fileName)
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }
}
