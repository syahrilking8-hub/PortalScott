package com.app.portal

import com.google.gson.annotations.SerializedName

data class Student(
    @SerializedName("id") val id: String,
    @SerializedName("nis") val nis: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("foto") val foto: String? = ""
)

// Pembungkus JSON response dari get_students.php
data class StudentResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data") val data: List<Student>? = null
)

data class ApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)
