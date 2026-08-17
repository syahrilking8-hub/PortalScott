package com.app.portal.ui

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
import com.app.portal.DynamicRetrofitClient
import com.app.portal.R
import com.app.portal.Student
import com.app.portal.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private var selectedDate: String = ""
    private var baseUrl: String = ""
    private var onImagePickedListener: ((Uri) -> Unit)? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            onImagePickedListener?.invoke(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        baseUrl = prefs.getString("base_url", "") ?: ""

        val userRole = prefs.getString("user_role", "USER") ?: "USER"
        binding.tvRole.text = userRole.uppercase()

        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "URL Server belum diset! Silakan login ulang.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        applyStyles()

        binding.btnLogout.setOnClickListener {
            prefs.edit().remove("user_role").apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Memperbarui data...", Toast.LENGTH_SHORT).show()
            fetchDataStudents()
        }

        binding.btnAddStudent.setOnClickListener {
            showFormDialog(null)
        }

        fetchDataStudents()
    }

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
            } else if (bgColor.isNotEmpty()) {
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

        val cardDrawable = getStyleDrawable("#800A101F", "#99F59E0B", 2, 20f)
        binding.cardTable.background = cardDrawable
        binding.layoutTableBorder.background = getStyleDrawable("#00000000", "#33F59E0B", 1, 8f)

        val glowAnim = ValueAnimator.ofFloat(0.4f, 0.9f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val alphaVal = (anim.animatedValue as Float * 255).toInt()
                val colorWithAlpha = Color.argb(alphaVal, 245, 158, 11)
                cardDrawable.setStroke((2 * resources.displayMetrics.density).toInt(), colorWithAlpha)
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

        students.forEachIndexed { index, student ->
            val tableRow = TableRow(this).apply {
                setPadding(0, 0, 0, 0)
            }

            tableRow.addView(createTableCell((index + 1).toString(), false, isCenter = true))
            tableRow.addView(createTableCell(student.nama, false))
            tableRow.addView(createTableCell(student.alamat, false))

            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(12, 10, 12, 10)
                background = getStyleDrawable("#00000000", "#1E293B", 1, 0f)
            }

            val btnDetail = Button(this).apply {
                text = "Detail"
                textSize = 11f
                setTextColor(Color.parseColor("#38BDF8"))
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((60 * resources.displayMetrics.density).toInt(), (32 * resources.displayMetrics.density).toInt())
                background = getStyleDrawable("#1E293B", "#38BDF8", 2, 6f)
                setOnClickListener { showDetailDialog(student) }
            }

            val btnEdit = Button(this).apply {
                text = "Edit"
                textSize = 11f
                setTextColor(Color.parseColor("#020617"))
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((52 * resources.displayMetrics.density).toInt(), (32 * resources.displayMetrics.density).toInt()).apply {
                    setMargins(6, 0, 6, 0)
                }
                background = getStyleDrawable("", null, 0, 6f, isGradientOrange = true)
                setOnClickListener { showFormDialog(student) }
            }

            val btnDelete = Button(this).apply {
                text = "Hapus"
                textSize = 11f
                setTextColor(Color.parseColor("#EF4444"))
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((56 * resources.displayMetrics.density).toInt(), (32 * resources.displayMetrics.density).toInt())
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

    private fun createTableCell(text: String, isHeader: Boolean, isCenter: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(if (isHeader) Color.parseColor("#F59E0B") else Color.parseColor("#F8FAFC"))
            textSize = 13f
            gravity = if (isCenter) Gravity.CENTER else Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            background = getStyleDrawable("#00000000", "#1E293B", 1, 0f)
        }
    }

    private fun showFormDialog(student: Student?) {
        selectedImageUri = null
        selectedDate = student?.tanggalLahir ?: ""

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_form, null)

        dialogView.findViewById<View>(R.id.cardDialogForm).background = getStyleDrawable("#800F172A", "#F59E0B", 2, 20f)
        dialogView.findViewById<View>(R.id.etNis).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etNama).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etTempatLahir).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.tvDatePicker).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etAlamat).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etHobi).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etCitaCita).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.containerInputFile).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.btnPickFile).background = getStyleDrawable("", null, 0, 6f, isGradientOrange = true)
        dialogView.findViewById<View>(R.id.btnCancel).background = getStyleDrawable("#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.btnSubmit).background = getStyleDrawable("", null, 0, 8f, isGradientOrange = true)

        val tvFormTitle = dialogView.findViewById<TextView>(R.id.tvFormTitle)
        val etNis = dialogView.findViewById<EditText>(R.id.etNis)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etTempatLahir = dialogView.findViewById<EditText>(R.id.etTempatLahir)
        val tvDatePicker = dialogView.findViewById<TextView>(R.id.tvDatePicker)
        val etAlamat = dialogView.findViewById<EditText>(R.id.etAlamat)
        val etHobi = dialogView.findViewById<EditText>(R.id.etHobi)
        val etCitaCita = dialogView.findViewById<EditText>(R.id.etCitaCita)
        val btnPickFile = dialogView.findViewById<Button>(R.id.btnPickFile)
        val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        onImagePickedListener = { uri ->
            tvFileName.text = getFileName(uri)
        }

        if (student != null) {
            tvFormTitle.text = "Edit Data Siswa"
            etNis.setText(student.nis)
            etNama.setText(student.nama)
            etTempatLahir.setText(student.tempatLahir)
            tvDatePicker.text = if (selectedDate.isNotEmpty()) selectedDate else "Pilih Tanggal Lahir"
            etAlamat.setText(student.alamat)
            etHobi.setText(student.hobi)
            etCitaCita.setText(student.citaCita)
        } else {
            tvFormTitle.text = "Tambah Data Siswa"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.6f)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        tvDatePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                val formattedMonth = String.format(Locale.US, "%02d", month + 1)
                val formattedDay = String.format(Locale.US, "%02d", day)
                selectedDate = "$year-$formattedMonth-$formattedDay"
                tvDatePicker.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickFile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            val nisStr = etNis.text.toString()
            val namaStr = etNama.text.toString()
            val tempatLahirStr = etTempatLahir.text.toString()
            val alamatStr = etAlamat.text.toString()
            val hobiStr = etHobi.text.toString()
            val citaCitaStr = etCitaCita.text.toString()

            if (nisStr.isEmpty() || namaStr.isEmpty()) {
                Toast.makeText(this, "NIS dan Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveData(student?.id, nisStr, namaStr, tempatLahirStr, selectedDate, alamatStr, hobiStr, citaCitaStr, dialog)
        }

        dialog.show()
    }

    private fun saveData(
        id: String?,
        nis: String,
        nama: String,
        tempatLahir: String,
        tglLahir: String,
        alamat: String,
        hobi: String,
        citaCita: String,
        dialog: AlertDialog
    ) {
        val textMediaType = "text/plain".toMediaTypeOrNull()
        val rbNis = nis.toRequestBody(textMediaType)
        val rbNama = nama.toRequestBody(textMediaType)
        val rbTempatLahir = tempatLahir.toRequestBody(textMediaType)
        val rbTgl = tglLahir.toRequestBody(textMediaType)
        val rbAlamat = alamat.toRequestBody(textMediaType)
        val rbHobi = hobi.toRequestBody(textMediaType)
        val rbCitaCita = citaCita.toRequestBody(textMediaType)

        var photoPart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)
            if (file != null && file.exists()) {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                photoPart = MultipartBody.Part.createFormData("foto", file.name, reqFile)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)
                val response = if (id == null) {
                    api.addStudent(rbNis, rbNama, rbTempatLahir, rbTgl, rbAlamat, rbHobi, rbCitaCita, photoPart)
                } else {
                    val rbId = id.toRequestBody(textMediaType)
                    api.updateStudent(rbId, rbNis, rbNama, rbTempatLahir, rbTgl, rbAlamat, rbHobi, rbCitaCita, photoPart)
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

        val cardDialog = dialogView.findViewById<View>(R.id.cardDialogDetail)
        val subcardBio = dialogView.findViewById<View>(R.id.subcardBiodata)
        val vAvatarGlow = dialogView.findViewById<View>(R.id.vAvatarGlow)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)

        cardDialog.background = getStyleDrawable("#0F172A", "#F59E0B", 2, 20f)
        subcardBio.background = getStyleDrawable("#090D16", "#1E293B", 1, 12f)
        vAvatarGlow.background = getStyleDrawable("", null, 0, 0f, isGradientOrange = true, isCircle = true)
        btnClose.background = getStyleDrawable("", null, 0, 10f, isGradientOrange = true)

        val ivFoto = dialogView.findViewById<ImageView>(R.id.ivDetailFoto)
        val tvNama = dialogView.findViewById<TextView>(R.id.tvDetailNama)
        val tvNis = dialogView.findViewById<TextView>(R.id.tvDetailNis)
        val tvAlamat = dialogView.findViewById<TextView>(R.id.tvDetailAlamat)
        val tvTtl = dialogView.findViewById<TextView>(R.id.tvDetailTtl)
        val tvHobi = dialogView.findViewById<TextView>(R.id.tvDetailHobi)
        val tvCitaCita = dialogView.findViewById<TextView>(R.id.tvDetailCitaCita)

        tvNis.text = ": ${student.nis}"
        tvNama.text = ": ${student.nama}"
        tvAlamat.text = ": ${student.alamat}"
        tvTtl.text = ": ${student.tempatLahir ?: "-"}, ${student.tanggalLahir ?: "-"}"
        tvHobi.text = ": ${if (!student.hobi.isNullOrEmpty()) student.hobi else "-"}"
        tvCitaCita.text = ": ${if (!student.citaCita.isNullOrEmpty()) student.citaCita else "-"}"

        val cleanBase = if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            "https://$baseUrl"
        } else {
            baseUrl
        }.trimEnd('/')

        val rawPath = student.foto ?: ""

        if (rawPath.isNotBlank()) {
            val trimmedPath = rawPath.trim()
            val finalUrl = when {
                trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://") -> trimmedPath
                else -> {
                    val fileName = trimmedPath.substringAfterLast('/')
                    "$cleanBase/uploads/${Uri.encode(fileName)}"
                }
            }

            ivFoto.load(finalUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            ivFoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.6f)
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
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
            tempFile.deleteOnExit()
            val out = FileOutputStream(tempFile)
            inputStream?.copyTo(out)
            inputStream?.close()
            out.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
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