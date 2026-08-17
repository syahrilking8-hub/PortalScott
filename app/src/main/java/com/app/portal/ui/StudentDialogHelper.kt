package com.app.portal.ui

import android.app.AlertDialog
import android.content.Context
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
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.app.portal.R
import com.app.portal.Student

object StudentDialogHelper {

    fun showDetailDialog(context: Context, student: Student, baseUrl: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_student_detail, null)[span_118](start_span)[span_118](end_span)

        val cardDialog = dialogView.findViewById<View>(R.id.cardDialogDetail)[span_119](start_span)[span_119](end_span)
        val subcardBio = dialogView.findViewById<View>(R.id.subcardBiodata)[span_120](start_span)[span_120](end_span)
        val vAvatarGlow = dialogView.findViewById<View>(R.id.vAvatarGlow)[span_121](start_span)[span_121](end_span)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)[span_122](start_span)[span_122](end_span)

        cardDialog.background = StudentStyleHelper.getStyleDrawable(context, "#330F172A", "#F59E0B", 2, 20f)[span_123](start_span)[span_123](end_span)
        subcardBio.background = StudentStyleHelper.getStyleDrawable(context, "#40090D16", "#2238BDF8", 1, 12f)[span_124](start_span)[span_124](end_span)
        vAvatarGlow.background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 0f, isGradientOrange = true, isCircle = true)[span_125](start_span)[span_125](end_span)
        btnClose.background = StudentStyleHelper.getStyleDrawable(context, "", null, 0, 8f, isGradientOrange = true)[span_126](start_span)[span_126](end_span)

        val ivFoto = dialogView.findViewById<ImageView>(R.id.ivDetailFoto)[span_127](start_span)[span_127](end_span)
        val tvNama = dialogView.findViewById<TextView>(R.id.tvDetailNama)[span_128](start_span)[span_128](end_span)
        val tvNis = dialogView.findViewById<TextView>(R.id.tvDetailNis)[span_129](start_span)[span_129](end_span)
        val tvAlamat = dialogView.findViewById<TextView>(R.id.tvDetailAlamat)[span_130](start_span)[span_130](end_span)
        val tvTtl = dialogView.findViewById<TextView>(R.id.tvDetailTtl)[span_131](start_span)[span_131](end_span)
        val tvHobi = dialogView.findViewById<TextView>(R.id.tvDetailHobi)[span_132](start_span)[span_132](end_span)
        val tvCitaCita = dialogView.findViewById<TextView>(R.id.tvDetailCitaCita)[span_133](start_span)[span_133](end_span)

        tvNis.text = ": ${student.nis}[span_134](start_span)"[span_134](end_span)
        tvNama.text = ": ${student.nama}[span_135](start_span)"[span_135](end_span)
        tvAlamat.text = ": ${student.alamat}[span_136](start_span)"[span_136](end_span)
        tvTtl.text = ": ${student.tempatLahir ?: "-"}, ${student.tanggalLahir ?: "-"}[span_137](start_span)"[span_137](end_span)
        tvHobi.text = ": ${if (!student.hobi.isNullOrEmpty()) student.hobi else "-"}[span_138](start_span)"[span_138](end_span)
        tvCitaCita.text = ": ${if (!student.citaCita.isNullOrEmpty()) student.citaCita else "-"}[span_139](start_span)"[span_139](end_span)

        val rawPath = student.foto ?: "[span_140](start_span)"[span_140](end_span)
        if (rawPath.isNotBlank()) {
            val cleanBase = if (!baseUrl.startsWith("http")) "https://$baseUrl" else baseUrl[span_141](start_span)[span_141](end_span)
            val fileName = rawPath.trim().substringAfterLast('/')[span_142](start_span)[span_142](end_span)
            val finalUrl = "${cleanBase.trimEnd('/')}/public/uploads/$fileName[span_143](start_span)"[span_143](end_span)

            val customImageLoader = ImageLoader.Builder(context)
                .allowHardware(false)[span_144](start_span)[span_144](end_span)
                .build()

            // Optimasi Dekompresi Agresif Agar Fast Load Tanpa Lag
            ivFoto.load(finalUrl, customImageLoader) {
                crossfade(true)
                size(80, 80)
                precision(Precision.INEXACT)
                allowHardware(false)[span_145](start_span)[span_145](end_span)
                transformations(CircleCropTransformation())[span_146](start_span)[span_146](end_span)
                placeholder(android.R.drawable.ic_menu_gallery)[span_147](start_span)[span_147](end_span)
                error(android.R.drawable.ic_menu_report_image)[span_148](start_span)[span_148](end_span)
            }
        } else {
            ivFoto.setImageResource(android.R.drawable.ic_menu_gallery)[span_149](start_span)[span_149](end_span)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()[span_150](start_span)[span_150](end_span)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)[span_151](start_span)[span_151](end_span)
            setGravity(Gravity.CENTER)[span_152](start_span)[span_152](end_span)
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)[span_153](start_span)[span_153](end_span)
            setDimAmount(0.5f)[span_154](start_span)[span_154](end_span)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)[span_155](start_span)[span_155](end_span)
                attributes.blurBehindRadius = 30[span_156](start_span)[span_156](end_span)
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()[span_157](start_span)[span_157](end_span)
        }

        dialog.show()[span_158](start_span)[span_158](end_span)
    }
}
