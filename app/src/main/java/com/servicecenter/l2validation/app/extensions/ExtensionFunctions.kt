package com.servicecenter.l2validation.app.extensions

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.app.base.BaseActivity
import com.servicecenter.l2validation.application.L2Application
import com.servicecenter.l2validation.utils.APIResultState
import com.servicecenter.l2validation.utils.Constants
import com.servicecenter.l2validation.utils.Constants.CAMERA
import com.servicecenter.l2validation.utils.Constants.LOCATION_COARSE
import com.servicecenter.l2validation.utils.Constants.LOCATION_FINE
import com.servicecenter.l2validation.utils.GenericKeyEvent
import com.servicecenter.l2validation.utils.GenericOTPTextWatcher
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


private const val PERMISSION_REQUEST_CODE = 123
private const val REQUEST_CHECK_SETTINGS = 200
private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100

val permission_arr = arrayOf(CAMERA, LOCATION_FINE, LOCATION_COARSE)

fun AppCompatActivity.hideKeyboard(activity: Activity) {
    val view = activity.currentFocus
    val methodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (view != null) {
        methodManager.hideSoftInputFromWindow(
            view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}
/*fun Fragment.hideKeyboard() {
    val view = requireActivity().currentFocus
    val methodManager =
        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (view != null) {
        methodManager.hideSoftInputFromWindow(
            view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}*/
fun AppCompatActivity.startScreen(activity: AppCompatActivity, extras: Bundle? = null) {
    val intent = Intent(this, activity::class.java)
    extras?.let { intent.putExtras(it) }
    startActivity(intent)
}

fun Fragment.startScreen(activity: AppCompatActivity, extras: Bundle? = null) {
    val intent = Intent(requireContext(), activity::class.java)
    extras?.let { intent.putExtras(it) }
    startActivity(intent)
}


fun AppCompatActivity.checkL2Permissions(permissions: Array<String>): Boolean {
    val permissionsToRequest = mutableListOf<String>()

    for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(permission)
        }
    }

    if (permissionsToRequest.isNotEmpty()) {
        val permissionsArray = permissionsToRequest.toTypedArray()
        ActivityCompat.requestPermissions(this, permissionsArray, PERMISSION_REQUEST_CODE)

        for (permission in permissionsArray) {
            val shouldShowRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            if (shouldShowRationale) {
                showPermissionSettingsDialog()
            }
        }

        return false
    } else {
        return true
    }
}

private fun AppCompatActivity.showPermissionSettingsDialog() {
    AlertDialog.Builder(this)
        .setTitle("Alert!")
        .setMessage(getString(R.string.you_have_denied_the_required_permission_please_grant_to_proceed_further))
        .setPositiveButton(getString(R.string.settings)) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            openAppSettings()
        }
    /*.setCancelable(false)
    .show()*/
}

private fun AppCompatActivity.openAppSettings() {
    val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val packageUri = Uri.fromParts("package", packageName, null)
    appSettingsIntent.data = packageUri
    startActivity(appSettingsIntent)
}

fun Context.readFileFromAssets(filePath: String): String {
    return resources.assets.open(filePath).bufferedReader().use {
        it.readText()
    }
}

@SuppressLint("MissingInflatedId")
fun AppCompatActivity.showToast(message: String, status: Boolean) {
    val customToastLayout = layoutInflater.inflate(R.layout.toast_layout, null)
    val customToast = Toast(this)
    customToast.view = customToastLayout
    customToastLayout.findViewById<TextView>(R.id.tv_status).text = message
    customToast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 10)
    customToast.duration = Toast.LENGTH_SHORT
    if (status) {
        customToastLayout.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.container)
            .setBackgroundResource(R.drawable.success_toast_bg)
        customToastLayout.findViewById<TextView>(R.id.tv_status)
            .setCompoundDrawablesWithIntrinsicBounds(R.drawable.success_toast_icon, 0, 0, 0)
    } else {
        customToastLayout.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.container)
            .setBackgroundResource(R.drawable.error_toast_bg)
        customToastLayout.findViewById<TextView>(R.id.tv_status)
            .setCompoundDrawablesWithIntrinsicBounds(R.drawable.error_toast_icon, 0, 0, 0)

    }
    customToast.show()
}

fun Fragment.showToast(message: String, status: Boolean,showLongToast:Int=Toast.LENGTH_SHORT) {
    val customToastLayout = layoutInflater.inflate(R.layout.toast_layout, null)
    val customToast = Toast(requireContext())
    customToast.view = customToastLayout
    customToastLayout.findViewById<TextView>(R.id.tv_status).text = message
    customToast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 10)
    customToast.duration = showLongToast
    if (status) {
        customToastLayout.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.container)
            .setBackgroundResource(R.drawable.success_toast_bg)
        customToastLayout.findViewById<TextView>(R.id.tv_status)
            .setCompoundDrawablesWithIntrinsicBounds(R.drawable.success_toast_icon, 0, 0, 0)
    } else {
        customToastLayout.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.container)
            .setBackgroundResource(R.drawable.error_toast_bg)
        customToastLayout.findViewById<TextView>(R.id.tv_status)
            .setCompoundDrawablesWithIntrinsicBounds(R.drawable.error_toast_icon, 0, 0, 0)

    }
    customToast.show()
}


fun AppCompatActivity.analyticsToAllScreen(value: String) {
    val bundle = Bundle()
    bundle.putString(Constants.SCREEN_KEY, value)
    FirebaseAnalytics.getInstance(this).logEvent(value, bundle)
}

fun AppCompatActivity.analyticsToAllButton(eventName: String, key: String, value: String) {
    val bundle = Bundle()
    bundle.putString(key, value)
    FirebaseAnalytics.getInstance(this).logEvent(eventName, bundle)
}

fun Activity.checkLocationSettingsAndRequestUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback,
    requestCode: Int
) {
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)

    val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

    task.addOnSuccessListener {
        // All location settings are satisfied. You can initialize location requests here.
        startLocationUpdates(fusedLocationClient, locationRequest, locationCallback)
    }

    task.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            // Location settings are not satisfied, but this can be fixed by showing the user a dialog
            try {
                exception.startResolutionForResult(this, requestCode)
            } catch (sendEx: IntentSender.SendIntentException) {
                // Handle the error
                sendEx.printStackTrace()
            }
        }
    }
}

fun Activity.startLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    if (hasLocationPermission()) {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    } else {
        requestLocationPermission(PERMISSION_REQUEST_ACCESS_LOCATION)
    }
}

fun Activity.hasLocationPermission(): Boolean {
    return ActivityCompat.checkSelfPermission(
        this,
        ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED
}

fun Activity.requestLocationPermission(requestCode: Int) {
    val permissionsToRequest = mutableListOf<String>()

    if (ActivityCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        permissionsToRequest.add(ACCESS_FINE_LOCATION)
    }
    if (ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(CAMERA)
    }

    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), requestCode)
    }
}

fun Activity.handleLocationSettingsResult(
    requestCode: Int,
    resultCode: Int,
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    if (requestCode == REQUEST_CHECK_SETTINGS) {
        if (resultCode == Activity.RESULT_OK) {
            startLocationUpdates(fusedLocationClient, locationRequest, locationCallback)
        } else {
            // The user didn't enable location services
            // Toast.makeText(this, "Location services are required for this feature", Toast.LENGTH_SHORT).show()
        }
    }
}

fun View.visible(){
    visibility = View.VISIBLE
}
fun View.gone(){
    visibility = View.GONE
}
fun String.maskMobileNumber(): String {
    return if (this.length == 10) {
        val maskedPart = this.substring(3, 7).replace(Regex("\\d"), "X")
        this.replaceRange(3, 7, maskedPart)
    } else {
        this
    }
}

fun AppCompatActivity.navigateToActivity(targetActivity: Class<*>) {
    val intent = Intent(this, targetActivity)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    startActivity(intent)
}
fun Fragment.navigateToActivity(targetActivity: Class<*>) {
    val intent = Intent(requireContext(), targetActivity)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    startActivity(intent)
}


fun setOtpWatcher(
    edOtp1: EditText, edOtp2: EditText, edOtp3: EditText, edOtp4: EditText,
    edOtp5: EditText, edOtp6: EditText
) {
    edOtp1.addTextChangedListener(GenericOTPTextWatcher(edOtp1, edOtp2))
    edOtp2.addTextChangedListener(GenericOTPTextWatcher(edOtp2, edOtp3))
    edOtp3.addTextChangedListener(GenericOTPTextWatcher(edOtp3, edOtp4))
    edOtp4.addTextChangedListener(GenericOTPTextWatcher(edOtp4, edOtp5))
    edOtp5.addTextChangedListener(GenericOTPTextWatcher(edOtp5, edOtp6))
    edOtp6.addTextChangedListener(GenericOTPTextWatcher(edOtp6, null))

    edOtp1.setOnKeyListener(GenericKeyEvent(edOtp1, null))
    edOtp2.setOnKeyListener(GenericKeyEvent(edOtp2, edOtp1))
    edOtp3.setOnKeyListener(GenericKeyEvent(edOtp3, edOtp2))
    edOtp4.setOnKeyListener(GenericKeyEvent(edOtp4, edOtp3))
    edOtp5.setOnKeyListener(GenericKeyEvent(edOtp5, edOtp4))
    edOtp6.setOnKeyListener(GenericKeyEvent(edOtp6, edOtp5))
}


fun Context.setBeepSound() {
    val mediaPlayer = MediaPlayer.create(this, R.raw.beep)
    if (mediaPlayer != null && !mediaPlayer.isPlaying) {
        mediaPlayer.setVolume(100f, 100f)
        mediaPlayer.start()
    }
}

fun String.isDateTomorrow(): Boolean {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateParsed = dateFormat.parse(this)
        val calendar = Calendar.getInstance()
        calendar.time = Date() // Current date
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDate = calendar.time
        dateFormat.format(selectedDateParsed) == dateFormat.format(tomorrowDate)
    } catch (e: Exception) {
        false
    }
}
fun Long.toFormattedDate(): String {
    val date = Date(this)
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val day = dayFormat.format(date).toInt()

    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    val dateFormat = SimpleDateFormat("MMM hh:mm a", Locale.getDefault())
    return "${day}$suffix ${dateFormat.format(date)}"
}

fun calculateDistance(feLat: Double, feLong: Double, consigneeLat: Double, consigneeLong: Double): Double {
    val earthRadius = 6371.0
    // Convert degrees to radians
    val latDistance = Math.toRadians(consigneeLat - feLat)
    val longDistance = Math.toRadians(consigneeLong - feLong)

    val a = sin(latDistance / 2).pow(2) + cos(Math.toRadians(feLat)) * cos(Math.toRadians(consigneeLat)) * sin(longDistance / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c // Distance in kilometers
}

fun formatTime(millis: Int): String {
    val minutes = millis / 1000 / 60
    val seconds = millis / 1000 % 60
    return String.format("%d:%02d", minutes, seconds)
}


