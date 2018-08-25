package jp.ginyolith.kamen_rider_matome

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast

fun Activity.toast(msg : String)
    = runOnUiThread { Toast.makeText(this,msg,Toast.LENGTH_SHORT).show() }

fun Activity.dialog(title : String, msg : String) {
    runOnUiThread {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(msg)
            setPositiveButton("OK", DialogInterface.OnClickListener { _, _ -> })
            setNegativeButton("Cancel", null)
            show()
        }
    }
}

fun Activity.runOnUiThread(exec : () -> Unit) {
    this.runOnUiThread(java.lang.Runnable(exec))
}

fun RecyclerView.setBorder(enabled : Boolean) {
    val decoration = DividerItemDecoration(context, LinearLayoutManager(context).orientation)

    if (enabled) {
        this.addItemDecoration(decoration)
    } else {
        this.removeItemDecoration(decoration)
    }
}

fun Context.runSharedPreference(lambda : (edit : SharedPreferences.Editor) -> Unit) {
    this.getSharedPreferences("data", Context.MODE_PRIVATE).edit().run {lambda(this)}
}