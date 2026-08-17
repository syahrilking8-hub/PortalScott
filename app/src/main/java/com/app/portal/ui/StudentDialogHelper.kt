package com.app.portal.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import coil.ImageLoader
import coil.load
import coil.transform.CircleCropTransformation
import com.app.portal.R
import com.app.portal.Student

object StudentDialogHelper {

    fun showDetailDialog(context: Context, student: Student, baseUrl: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_student_detail, null)

        val cardDialog = dialogView.findViewById<View>(R.id.cardDialogDetail)
        val subcardBio = dialogView.findViewById<View>(R.id.subcardBiodata)
        val vAvatarGlow = dialogView.findViewById<View>(R.id.vAvatarGlow)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)

        cardDialog.background = StudentStyleHelper.getStyleDrawable(context, "#330F172A", "#F59E0B", 2, 20f)
        subcardBio.background = StudentStyleHelper.getStyleDrawable(context, "#40090D16", "#2238BDF8", 1, 12f)
        vAvatarGlow.background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 0f, isGradientOrange = true, isCircle = true)
        btnClose.background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 8f, isGradientOrange = true)

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

        val rawPath = student.foto ?: ""
        if (rawPath.isNotBlank()) {
            val cleanBase = if (!baseUrl.startsWith("http")) "https://$baseUrl" else baseUrl
            val fileName = rawPath.trim().substringAfterLast('/')
            val finalUrl = "${cleanBase.trimEnd('/')}/public/uploads/$fileName"

            val customImageLoader = ImageLoader.Builder(context)
                .allowHardware(false)
                .build()

            ivFoto.load(finalUrl, customImageLoader) {
                crossfade(true)
                allowHardware(false)
                transformations(CircleCropTransformation())
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            ivFoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialog.window?.apply {
    setBackgroundDrawableResource(android.R.color.transparent)
    setGravity(Gravity.CENTER)
    setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Bebas melar di landscape
    setDimAmount(0.5f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        attributes.blurBehindRadius = 30
    }
}
