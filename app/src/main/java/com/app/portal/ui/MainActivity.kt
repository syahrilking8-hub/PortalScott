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

        // Fitur Search Real-time
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
        binding.btnLogout.background = StudentStyleHelper.getStyleDrawable(this, "#33020617", "#73F59E0B", 2, 8f)[span_1](start_span)[span_1](end_span)
        binding.btnAddStudent.background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 8f, isGradientOrange = true)[span_2](start_span)[span_2](end_span)

        // Search Bar Transparan 45% (#730F172A)
        binding.etSearch.background = StudentStyleHelper.getStyleDrawable(this, "#730F172A", "#3338BDF8", 1, 8f)

        // Outer Card & Inner Border Glow Emas
        val cardDrawable = StudentStyleHelper.getStyleDrawable(this, "#990F172A", "#F59E0B", 2, 14f)[span_3](start_span)[span_3](end_span)
        binding.cardInnerContainer.background = cardDrawable
        StudentStyleHelper.applyGlowAnimation(this, cardDrawable)[span_4](start_span)[span_4](end_span)
    }

    private fun fetchDataStudents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)
                val response = api.getStudents()
                withContext(Dispatchers.Main) {
                    masterStudentList = response.data ?: emptyList()
                    filterTableData(binding.etSearch.text.toString())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()[span_5](start_span)[span_5](end_span)
                }
            }
        }
    }

    private fun filterTableData(query: String) {
        val filteredList = if (query.isEmpty()) {
            masterStudentList
        } else {
            masterStudentList.filter {
                it.nama.contains(query, ignoreCase = true) || it.nis.contains(query, ignoreCase = true)
            }
        }
        populateTable(filteredList)
    }

    private fun populateTable(students: List<Student>) {
        val childCount = binding.tableStudents.childCount
        if (childCount > 1) {
            binding.tableStudents.removeViews(1, childCount - 1)[span_6](start_span)[span_6](end_span)
        }

        students.forEachIndexed { index, student ->
            val tableRow = TableRow(this)

            tableRow.addView(createTableCell((index + 1).toString(), false, 55, isCenter = true))
            tableRow.addView(createTableCell(student.nis, false, 130, isCenter = true))
            tableRow.addView(createTableCell(student.nama, false, 200))
            tableRow.addView(createTableCell(student.alamat, false, 220))

            val density = resources.displayMetrics.density
            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding((4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#990B0F19", "#3338BDF8", 1, 0f)[span_7](start_span)[span_7](end_span)
                layoutParams = TableRow.LayoutParams((220 * density).toInt(), TableRow.LayoutParams.MATCH_PARENT)
            }

            val btnDetail = Button(this).apply {
                text = "Detail"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#38BDF8"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((62 * density).toInt(), (32 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#1E293B", "#38BDF8", 1, 6f)[span_8](start_span)[span_8](end_span)
                setOnClickListener { StudentDialogHelper.showDetailDialog(this@MainActivity, student, baseUrl) }[span_9](start_span)[span_9](end_span)
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
                background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 6f, isGradientOrange = true)[span_10](start_span)[span_10](end_span)
                setOnClickListener { showFormDialog(student) }[span_11](start_span)[span_11](end_span)
            }

            val btnDelete = Button(this).apply {
                text = "Hapus"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#EF4444"))
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams((58 * density).toInt(), (32 * density).toInt())
                background = StudentStyleHelper.getStyleDrawable(context, "#1E293B", "#EF4444", 1, 6f)[span_12](start_span)[span_12](end_span)
                setOnClickListener { deleteData(student.id) }[span_13](start_span)[span_13](end_span)
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
            setTextColor(if (isHeader) Color.parseColor("#F59E0B") else Color.parseColor("#FFFFFF"))[span_14](start_span)[span_14](end_span)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)[span_15](start_span)[span_15](end_span)
            gravity = if (isCenter) Gravity.CENTER else Gravity.CENTER_VERTICAL
            setPadding((10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
            background = StudentStyleHelper.getStyleDrawable(context, "#990B0F19", "#3338BDF8", 1, 0f)[span_16](start_span)[span_16](end_span)
            layoutParams = TableRow.LayoutParams((widthDp * density).toInt(), TableRow.LayoutParams.MATCH_PARENT)
        }
    }

    private fun showFormDialog(student: Student?) {
        selectedImageUri = null
        selectedDate = student?.tanggalLahir ?: "[span_17](start_span)"[span_17](end_span)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_form, null)[span_18](start_span)[span_18](end_span)

        // Form Dialog Opacity 60% (#990F172A)
        dialogView.findViewById<View>(R.id.cardDialogForm).background = StudentStyleHelper.getStyleDrawable(this, "#990F172A", "#F59E0B", 2, 20f)[span_19](start_span)[span_19](end_span)
        dialogView.findViewById<View>(R.id.etNis).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_20](start_span)[span_20](end_span)
        dialogView.findViewById<View>(R.id.etNama).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_21](start_span)[span_21](end_span)
        dialogView.findViewById<View>(R.id.etTempatLahir).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_22](start_span)[span_22](end_span)
        dialogView.findViewById<View>(R.id.tvDatePicker).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_23](start_span)[span_23](end_span)
        dialogView.findViewById<View>(R.id.etAlamat).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_24](start_span)[span_24](end_span)
        dialogView.findViewById<View>(R.id.etHobi).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_25](start_span)[span_25](end_span)
        dialogView.findViewById<View>(R.id.etCitaCita).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_26](start_span)[span_26](end_span)
        dialogView.findViewById<View>(R.id.containerInputFile).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_27](start_span)[span_27](end_span)
        dialogView.findViewById<View>(R.id.btnPickFile).background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 6f, isGradientOrange = true)[span_28](start_span)[span_28](end_span)
        dialogView.findViewById<View>(R.id.btnCancel).background = StudentStyleHelper.getStyleDrawable(this, "#0B132B", "#334155", 1, 8f)[span_29](start_span)[span_29](end_span)
        dialogView.findViewById<View>(R.id.btnSubmit).background = StudentStyleHelper.getStyleDrawable(this, "", null, 0, 8f, isGradientOrange = true)[span_30](start_span)[span_30](end_span)

        val tvFormTitle = dialogView.findViewById<TextView>(R.id.tvFormTitle)[span_31](start_span)[span_31](end_span)
        val etNis = dialogView.findViewById<EditText>(R.id.etNis)[span_32](start_span)[span_32](end_span)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)[span_33](start_span)[span_33](end_span)
        val etTempatLahir = dialogView.findViewById<EditText>(R.id.etTempatLahir)[span_34](start_span)[span_34](end_span)
        val tvDatePicker = dialogView.findViewById<TextView>(R.id.tvDatePicker)[span_35](start_span)[span_35](end_span)
        val etAlamat = dialogView.findViewById<EditText>(R.id.etAlamat)[span_36](start_span)[span_36](end_span)
        val etHobi = dialogView.findViewById<EditText>(R.id.etHobi)[span_37](start_span)[span_37](end_span)
        val etCitaCita = dialogView.findViewById<EditText>(R.id.etCitaCita)[span_38](start_span)[span_38](end_span)
        val btnPickFile = dialogView.findViewById<Button>(R.id.btnPickFile)[span_39](start_span)[span_39](end_span)
        val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)[span_40](start_span)[span_40](end_span)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)[span_41](start_span)[span_41](end_span)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)[span_42](start_span)[span_42](end_span)

        onImagePickedListener = { uri ->
            tvFileName.text = getFileName(uri)[span_43](start_span)[span_43](end_span)
        }

        if (student != null) {
            tvFormTitle.text = "Edit Data Siswa[span_44](start_span)"[span_44](end_span)
            etNis.setText(student.nis)[span_45](start_span)[span_45](end_span)
            etNama.setText(student.nama)[span_46](start_span)[span_46](end_span)
            etTempatLahir.setText(student.tempatLahir)[span_47](start_span)[span_47](end_span)
            tvDatePicker.text = if (selectedDate.isNotEmpty()) selectedDate else "Pilih Tanggal Lahir[span_48](start_span)"[span_48](end_span)
            etAlamat.setText(student.alamat)[span_49](start_span)[span_49](end_span)
            etHobi.setText(student.hobi)[span_50](start_span)[span_50](end_span)
            etCitaCita.setText(student.citaCita)[span_51](start_span)[span_51](end_span)
        } else {
            tvFormTitle.text = "Tambah Data Siswa[span_52](start_span)"[span_52](end_span)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()[span_53](start_span)[span_53](end_span)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)[span_54](start_span)[span_54](end_span)
            setGravity(Gravity.CENTER)[span_55](start_span)[span_55](end_span)
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)[span_56](start_span)[span_56](end_span)
            setDimAmount(0.6f)[span_57](start_span)[span_57](end_span)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }[span_58](start_span)[span_58](end_span)

        tvDatePicker.setOnClickListener {
            val cal = Calendar.getInstance()[span_59](start_span)[span_59](end_span)
            android.app.DatePickerDialog(this, { _, year, month, day ->
                val formattedMonth = String.format(Locale.US, "%02d", month + 1)[span_60](start_span)[span_60](end_span)
                val formattedDay = String.format(Locale.US, "%02d", day)[span_61](start_span)[span_61](end_span)
                selectedDate = "$year-$formattedMonth-$formattedDay[span_62](start_span)"[span_62](end_span)
                tvDatePicker.text = selectedDate[span_63](start_span)[span_63](end_span)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()[span_64](start_span)[span_64](end_span)
        }

        btnPickFile.setOnClickListener {
            pickImageLauncher.launch("image/*")[span_65](start_span)[span_65](end_span)
        }

        btnSubmit.setOnClickListener {
            val nisStr = etNis.text.toString()[span_66](start_span)[span_66](end_span)
            val namaStr = etNama.text.toString()[span_67](start_span)[span_67](end_span)

            if (nisStr.isEmpty() || namaStr.isEmpty()) {
                Toast.makeText(this, "NIS dan Nama wajib diisi!", Toast.LENGTH_SHORT).show()[span_68](start_span)[span_68](end_span)
                return@setOnClickListener
            }

            saveData(student?.id, nisStr, namaStr, etTempatLahir.text.toString(), selectedDate, etAlamat.text.toString(), etHobi.text.toString(), etCitaCita.text.toString(), dialog)[span_69](start_span)[span_69](end_span)
        }

        dialog.show()[span_70](start_span)[span_70](end_span)
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
        val textMediaType = "text/plain".toMediaTypeOrNull()[span_71](start_span)[span_71](end_span)
        val rbNis = nis.toRequestBody(textMediaType)[span_72](start_span)[span_72](end_span)
        val rbNama = nama.toRequestBody(textMediaType)[span_73](start_span)[span_73](end_span)
        val rbTempatLahir = tempatLahir.toRequestBody(textMediaType)[span_74](start_span)[span_74](end_span)
        val rbTgl = tglLahir.toRequestBody(textMediaType)[span_75](start_span)[span_75](end_span)
        val rbAlamat = alamat.toRequestBody(textMediaType)[span_76](start_span)[span_76](end_span)
        val rbHobiObj = hobi.toRequestBody(textMediaType)[span_77](start_span)[span_77](end_span)
        val rbCitaCita = citaCita.toRequestBody(textMediaType)[span_78](start_span)[span_78](end_span)

        var photoPart: MultipartBody.Part? = null[span_79](start_span)[span_79](end_span)
        selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)[span_80](start_span)[span_80](end_span)
            if (file != null && file.exists()) {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg[span_81](start_span)"[span_81](end_span)
                val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())[span_82](start_span)[span_82](end_span)
                photoPart = MultipartBody.Part.createFormData("foto", file.name, reqFile)[span_83](start_span)[span_83](end_span)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = DynamicRetrofitClient.getService(baseUrl)[span_84](start_span)[span_84](end_span)
                val response = if (id == null) {
                    api.addStudent(rbNis, rbNama, rbTempatLahir, rbTgl, rbAlamat, rbHobiObj, rbCitaCita, photoPart)[span_85](start_span)[span_85](end_span)
                } else {
                    val rbId = id.toRequestBody(textMediaType)[span_86](start_span)[span_86](end_span)
                    api.updateStudent(rbId, rbNis, rbNama, rbTempatLahir, rbTgl, rbAlamat, rbHobiObj, rbCitaCita, photoPart)[span_87](start_span)[span_87](end_span)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()[span_88](start_span)[span_88](end_span)
                    dialog.dismiss()[span_89](start_span)[span_89](end_span)
                    fetchDataStudents()[span_90](start_span)[span_90](end_span)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()[span_91](start_span)[span_91](end_span)
                }
            }
        }
    }

    private fun deleteData(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")[span_92](start_span)[span_92](end_span)
            .setMessage("Apakah Anda yakin ingin menghapus data siswa ini?")[span_93](start_span)[span_93](end_span)
            .setPositiveButton("Hapus") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val api = DynamicRetrofitClient.getService(baseUrl)[span_94](start_span)[span_94](end_span)
                        val response = api.deleteStudent(id)[span_95](start_span)[span_95](end_span)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()[span_96](start_span)[span_96](end_span)
                            fetchDataStudents()[span_97](start_span)[span_97](end_span)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Gagal hapus: ${e.message}", Toast.LENGTH_SHORT).show()[span_98](start_span)[span_98](end_span)
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)[span_99](start_span)[span_99](end_span)
            .show()[span_100](start_span)[span_100](end_span)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)[span_101](start_span)[span_101](end_span)
            val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)[span_102](start_span)[span_102](end_span)
            tempFile.deleteOnExit()[span_103](start_span)[span_103](end_span)
            val out = FileOutputStream(tempFile)[span_104](start_span)[span_104](end_span)
            inputStream?.copyTo(out)[span_105](start_span)[span_105](end_span)
            inputStream?.close()[span_106](start_span)[span_106](end_span)
            out.close()[span_107](start_span)[span_107](end_span)
            tempFile[span_108](start_span)[span_108](end_span)
        } catch (e: Exception) {
            e.printStackTrace()[span_109](start_span)[span_109](end_span)
            null[span_110](start_span)[span_110](end_span)
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null[span_111](start_span)[span_111](end_span)
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)[span_112](start_span)[span_112](end_span)
                    if (index != -1) result = cursor.getString(index)[span_113](start_span)[span_113](end_span)
                }
            }
        }
        if (result == null) {
            result = uri.path[span_114](start_span)[span_114](end_span)
            val cut = result?.lastIndexOf('/') ?: -1[span_115](start_span)[span_115](end_span)
            if (cut != -1) result = result?.substring(cut + 1)[span_116](start_span)[span_116](end_span)
        }
        return result[span_117](start_span)[span_117](end_span)
    }
}