package jp.ginyolith.kamen_rider_matome

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.widget.Toast

fun Activity.toast(msg : String) {
    Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
}

fun Activity.dialog(title : String, msg : String) {
    AlertDialog.Builder(this).apply {
        setTitle(title)
        setMessage(msg)
        setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
        })
        setNegativeButton("Cancel", null)
        show()
    }
}

fun Activity.runOnUiThread(exec : () -> Unit) {
    this.runOnUiThread(java.lang.Runnable(exec))
}