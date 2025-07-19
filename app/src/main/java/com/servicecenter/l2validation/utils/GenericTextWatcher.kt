package com.servicecenter.l2validation.utils
// code reviewed
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.servicecenter.l2validation.R
import com.google.android.material.button.MaterialButton

class GenericTextWatcher(
    val context: AppCompatActivity,
    val isValid: () -> Boolean,
    var btnSignIn: MaterialButton,
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (isValid()) {
            btnSignIn.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_color))
            btnSignIn.isEnabled = true
        } else {
            btnSignIn.setBackgroundColor(Color.GRAY)
            btnSignIn.isEnabled = false
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }
}