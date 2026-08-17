package com.app.portal.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.app.portal.DynamicRetrofitClient
import com.app.portal.R
import com.app.portal.Student
import com.app.portal.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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
            Toast.makeText(this, "URL Server belum diset!", Toast.LENGTH_SHORT).show()
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

    private fun applyStyles() {
        binding.btnLogout.background = StudentStyleHelper.getStyleDrawable(this, "#33020617", "#73F59E0B", 2, 8f)
        binding.btnAddStudent.background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 8f, isGradientOrange = true)

        val cardDrawable = StudentStyleHelper.getStyleDrawable(this, "#330A101F", "#99F59E0B", 2, 20f)
        binding.cardTable.background = cardDrawable
        binding.layoutTableBorder.background = StudentStyleHelper.getStyleDrawable(this, "#00000000", "#33F59E0B", 1, 8f)

        StudentStyleHelper.applyGlowAnimation(this, cardDrawable)
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
            val tableRow = TableRow(this)

            tableRow.addView(createTableCell((index + 1).toString(), false, 45, isCenter = true))
            tableRow.addView(createTableCell(student.nama, false, 160))
            tableRow.addView(createTableCell(student.alamat, false, 200))

            val density = resources.displayMetrics.density
            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#00000000", "#3338BDF8", 1, 0f)
                layoutParams = TableRow.LayoutParams((210 * density).toInt(), TableRow.LayoutParams.MATCH_PARENT)
            }

            val btnDetail = Button(this).apply {
                text = "Detail"
                textSize = 10f
                setTextColor(Color.parseColor("#38BDF8"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((56 * density).toInt(), (30 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#1E293B", "#38BDF8", 1, 6f)
                setOnClickListener { StudentDialogHelper.showDetailDialog(this@MainActivity, student, baseUrl) }
            }

            val btnEdit = Button(this).apply {
                text = "Edit"
                textSize = 10f
                setTextColor(Color.parseColor("#020617"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (30 * density).toInt()).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 6f, isGradientOrange = true)
                setOnClickListener { showFormDialog(student) }
            }

            val btnDelete = Button(this).apply {
                text = "Hapus"
                textSize = 10f
                setTextColor(Color.parseColor("#EF4444"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((52 * density).toInt(), (30 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#1E293B", "#EF4444", 1, 6f)
                setOnClickListener { deleteData(student.id) }
            }

            actionLayout.addView(btnDetail)
            actionLayout.addView(btnEdit)
            actionLayout.addView(btnDelete)
            tableRow.addView(actionLayout)

            binding.tableStudents.addView(tableRow)
        }
    }

    private fun createTableCell(text: String, isHeader: Boolean, widthDp: Int, isCenter: Boolean = false): TextView {
        val density = resources.displayMetrics.density
        return TextView(this).apply {
            this.text = text
            setTextColor(if (isHeader) Color.parseColor("#F59E0B") else Color.parseColor("#F8FAFC"))
            textSize = 12f
            gravity = if (isCenter) Gravity.CENTER else Gravity.CENTER_VERTICAL
            setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
            background = StudentStyleHelper.getStyleDrawable(context, "#00000000", "#3338BDF8", 1, 0f)
            layoutParams = TableRow.LayoutParams((widthDp * density).toInt(), TableRow.LayoutParams.MATCH_PARENT)
        }
    }

    private fun showFormDialog(student: Student?) {
        selectedImageUri = null
        selectedDate = student?.tanggalLahir ?: ""

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_form, null)

        dialogView.findViewById<View>(R.id.cardDialogForm).background = StudentStyleHelper.getStyleDrawable(this, "#330F172A", "#F59E0B", 2, 20f)
        dialogView.findViewById<View>(R.id.etNis).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etNama).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etTempatLahir).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.tvDatePicker).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etAlamat).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etHobi).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.etCitaCita).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.containerInputFile).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.btnPickFile).background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 6f, isGradientOrange = true)
        dialogView.findViewById<View>(R.id.btnCancel).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)
        dialogView.findViewById<View>(R.id.btnSubmit).background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 8f, isGradientOrange = true)

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
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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

            if (nisStr.isEmpty() || namaStr.isEmpty()) {
                Toast.makeText(this, "NIS dan Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveData(student?.id, nisStr, namaStr, etTempatLahir.text.toString(), selectedDate, etAlamat.text.toString(), etHobi.text.toString(), etCitaCita.text.toString(), dialog)
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
        val rbHobiObj = hobi.toRequestBody(textMediaType)
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
                    api.addStudent(rbNis, rbNama, rbTempatLahir, rbTgl, rbAlamat, rbHobiObj, rbCitaCita, photoPart)
                } else {
                    val rbId = id.toRequestBody(textMediaType)
                    api.updateStudent(rbId, rbNis, rbNama, rbTempatLahir, rbTgl, rbAlamat, rbHobiObj, rbCitaCita, photoPart)
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

    private fun deleteData(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus data siswa ini?")
            .setPositiveButton("Hapus") { _, _ ->
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
            .setNegativeButton("Batal", null)
            .show()
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
