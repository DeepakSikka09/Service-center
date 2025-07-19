package com.servicecenter.l2validation.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.location.LocationManager
import android.media.AudioManager
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowInsets
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt


object CommonUtils {
    fun convertToByteArray(image: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        image.compress(
            Bitmap.CompressFormat.JPEG,
            60,
            baos
        )
        var options = 90
        while (baos.toByteArray().size / 1024 > 400) {
            baos.reset()
            image.compress(
                Bitmap.CompressFormat.JPEG,
                options,
                baos
            )
            options -= 10
        }
        return baos.toByteArray()
    }



    fun compressImages(tempFile: File, activity: Activity): String? {
        val filePath: String = tempFile.path
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()
        /*by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
      you try the use the bitmap here, you will get null.*/options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(filePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth
        /*max Height and width values of the compressed image is taken as 1123x794*/
        val maxHeight = 1123.0f
        val maxWidth = 794.0f
        var imgRatio = (actualWidth / actualHeight).toFloat()
        val maxRatio = maxWidth / maxHeight
        /*width and height values are set maintaining the aspect ratio of the image*/
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight
                actualWidth = (imgRatio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth
                actualHeight = (imgRatio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()
            } else {
                actualHeight = maxHeight.toInt()
                actualWidth = maxWidth.toInt()
            }
        }
        else {
            /*   val displayMetrics = DisplayMetrics()
               activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
           */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                actualWidth =  windowMetrics.bounds.width() - insets.left - insets.right
                actualHeight =  windowMetrics.bounds.height() - insets.bottom - insets.top
            }
            else {
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                //  displayMetrics.widthPixels
                actualHeight = displayMetrics.heightPixels
                actualWidth = displayMetrics.widthPixels
            }

        }
        /*setting inSampleSize value allows to load a scaled down version of the original image*/options.inSampleSize =
            calculateInSampleSize(options, actualWidth, actualHeight)
        /*inJustDecodeBounds set to false to load the actual bitmap*/
        options.inJustDecodeBounds = false
        /*this options allow android to claim the bitmap memory if it runs low on memory*/
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)
        try {
            /*load the bitmap from its path*/
            bmp = BitmapFactory.decodeFile(filePath, options)
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        assert(scaledBitmap != null)
        val canvas = Canvas(scaledBitmap!!)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bmp,
            middleX - bmp.width / 2,
            middleY - bmp.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        //apply water mark on scaled bitmap. This image will synced to server.
        //No way to find employee id and location when location and employee id is not null uncomment below line.
        //Water mark will show on image.
        //scaledBitmap = applyWaterMark(scaledBitmap, empCode);
        //apply water mark on scaled bitmap. This image will synced to server.
        /*check the rotation of the image and display it properly*/
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            //  Log.d("EXIF", "Exif: " + orientation);
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90f)
                //    Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180f)
                //    Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270f)
                //    Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                matrix,
                true
            )
            scaledBitmap = scaledBitmap
        } catch (e: IOException) {
            //SathiLogger.e(e.getMessage());
            e.printStackTrace()
        }
        val out: FileOutputStream

        val file_size: Int = java.lang.String.valueOf(tempFile.length() / 1024).toInt()
        Log.d("check size",file_size.toString())

        val filename: String = tempFile.path
        try {
            out = FileOutputStream(filename)
            /*write the compressed bitmap at the destination specified by filename.*/scaledBitmap!!.compress(
                Bitmap.CompressFormat.JPEG, 85, out
            )
        } catch (e: FileNotFoundException) {
            // SathiLogger.e(e.getMessage());
            e.printStackTrace()
        }
        return filename
    }



    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        var isOnline = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities: NetworkCapabilities? =
                connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork())
            isOnline =
                capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            val activeNetworkInfo: NetworkInfo? = connectivityManager.getActiveNetworkInfo()
            isOnline = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting

        }
        return true
    }
    fun checkGpsStatus(mContext: Context): Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        return isGpsEnabled
    }


    fun calculateImageBlurryOrNot(
        bitmap: Bitmap,
        laplacianVarianceThreshold: Double,
        sobelMagnitudeThreshold: Double
    ): Boolean {
        // Calculate Laplacian variance
        val laplacianVariance = calculateLaplacianVariance(bitmap)
        // Calculate Sobel edge detection
        val sobelMagnitude = sobelEdgeDetection(bitmap)
        // Determine if the image is blurred based on thresholds

        return !(laplacianVarianceThreshold < laplacianVariance && sobelMagnitudeThreshold < sobelMagnitude)
    }

    // Laplacian Variance Calculation
    private fun calculateLaplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale
        val grayscalePixels = IntArray(width * height)
        var pixelSum = 0
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val gray = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
            grayscalePixels[i] = gray
            pixelSum += gray
        }
        val avgGray = pixelSum / (width * height) // Average grayscale value
        var sumLaplacian = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val centerIndex = y * width + x
                val centerPixel = grayscalePixels[centerIndex] - avgGray
                val topPixel = grayscalePixels[centerIndex - width] - avgGray
                val bottomPixel = grayscalePixels[centerIndex + width] - avgGray
                val leftPixel = grayscalePixels[centerIndex - 1] - avgGray
                val rightPixel = grayscalePixels[centerIndex + 1] - avgGray

                // Calculate Laplacian
                val laplacian = 4 * centerPixel - topPixel - bottomPixel - leftPixel - rightPixel
                sumLaplacian += laplacian.toDouble().pow(2)
                count++
            }
        }
        // Average variance
        return if (count > 0) sumLaplacian / count else 0.0
    }

    // Sobel Edge Detection
    private fun sobelEdgeDetection(bitmap: Bitmap): Double {
        val gx = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        val gy = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sumGradient = 0.0
        var count = 0
        val grayscalePixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val gray = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
            grayscalePixels[i] = gray
        }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gradientX = 0
                var gradientY = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = grayscalePixels[(y + ky) * width + (x + kx)]
                        gradientX += pixel * gx[ky + 1][kx + 1]
                        gradientY += pixel * gy[ky + 1][kx + 1]
                    }
                }
                val gradientMagnitude = sqrt((gradientX * gradientX + gradientY * gradientY).toDouble())
                sumGradient += gradientMagnitude
                count++
            }
        }
        return if (count > 0) sumGradient / count else 0.0
    }


    fun loadJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun isCallActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.mode == AudioManager.MODE_IN_CALL
    }
}