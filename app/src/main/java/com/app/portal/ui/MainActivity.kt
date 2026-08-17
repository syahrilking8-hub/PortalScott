package com.app.portal.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
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
    private var masterStudentList: List<Student> = emptyList()
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

        binding.tvRole.text = "• Selamat Datang di Panel Data Siswa"

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

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTableData(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchDataStudents()
    }

    private fun applyStyles() {
        binding.btnLogout.background = StudentStyleHelper.getStyleDrawable(this, "#220F172A", "#F59E0B", 2, 8f)
        binding.btnAddStudent.background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 8f, isGradientOrange = true)

        binding.etSearch.background = StudentStyleHelper.getStyleDrawable(this, "#730F172A", "#3338BDF8", 1, 8f)

        val cardDrawable = StudentStyleHelper.getStyleDrawable(this, "#990F172A", "#F59E0B", 2, 14f)
        binding.cardInnerContainer.background = cardDrawable
        StudentStyleHelper.applyGlowAnimation(this, cardDrawable)
    }

    private fun populateTable(students: List<Student>) {
        val childCount = binding.tableStudents.childCount
        if (childCount > 1) {
            binding.tableStudents.removeViews(1, childCount - 1)
        }

        students.forEachIndexed { index, student ->
            val tableRow = TableRow(this)

            tableRow.addView(createTableCell((index + 1).toString(), false, 55, isCenter = true))
            tableRow.addView(createTableCell(student.nis, false, 130, isCenter = true))
            tableRow.addView(createTableCell(student.nama, false, 200, isCenter = true))
            tableRow.addView(createTableCell(if (student.alamat.isNullOrEmpty()) "-" else student.alamat, false, 220, isCenter = true))

            val density = resources.displayMetrics.density
            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding((4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#990B0F19", "#3338BDF8", 1, 0f)
                layoutParams = TableRow.LayoutParams((220 * density).toInt(), TableRow.LayoutParams.MATCH_PARENT)
            }

            val btnDetail = Button(this).apply {
                text = "Detail"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#38BDF8"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((62 * density).toInt(), (32 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#1E293B", "#38BDF8", 1, 6f)
                setOnClickListener { StudentDialogHelper.showDetailDialog(this@MainActivity, student, baseUrl) }
            }

            val btnEdit = Button(this).apply {
                text = "Edit"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#020617"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((54 * density).toInt(), (32 * density).toInt()).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 6f, isGradientOrange = true)
                setOnClickListener { showFormDialog(student) }
            }

            val btnDelete = Button(this).apply {
                text = "Hapus"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#EF4444"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((58 * density).toInt(), (32 * density).toInt())
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
            setTextColor(if (isHeader) Color.parseColor("#F59E0B") else Color.parseColor("#FFFFFF"))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = if (isCenter) Gravity.CENTER else Gravity.CENTER_VERTICAL
            setPadding((10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
            background = StudentStyleHelper.getStyleDrawable(context, "#990B0F19", "#3338BDF8", 1, 0f)
            layoutParams = TableRow.LayoutParams((widthDp * density).toInt(), TableRow.LayoutParams.MATCH_PARENT)
        }
    }

    private fun showFormDialog(student: Student?) {
        selectedImageUri = null
        selectedDate = student?.tanggalLahir ?: ""

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_form, null)

        dialogView.findViewById<View>(R.id.cardDialogForm).background = StudentStyleHelper.getStyleDrawable(this, "#990F172A", "#F59E0B", 2, 20f)
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
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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

            private fun fetchDataStudents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)
                val response = api.getStudents() // Mengembalikan StudentResponse langsung
                withContext(Dispatchers.Main) {
                    if (response.isSuccess) {
                        masterStudentList = response.data ?: emptyList()
                        populateTable(masterStudentList)
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterTableData(query: String) {
        val filteredList = if (query.isEmpty()) {
            masterStudentList
        } else {
            masterStudentList.filter { student ->
                student.nama.contains(query, ignoreCase = true) || 
                student.nis.contains(query, ignoreCase = true)
            }
        }
        populateTable(filteredList)
    }
}
