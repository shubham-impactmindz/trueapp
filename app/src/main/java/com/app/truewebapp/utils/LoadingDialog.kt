package com.app.truewebapp.utils

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.app.truewebapp.R

class LoadingDialog(val context: Activity) {
    private var dialog: AlertDialog? = null
    fun startDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val inflater: LayoutInflater = context.layoutInflater
        builder.setView(inflater.inflate(R.layout.loading_dialog_layout, null))
        builder.setCancelable(true)
        dialog = builder.create()
        dialog?.show();
    }

    fun closeDialog() {
        dialog?.dismiss()
    }
}