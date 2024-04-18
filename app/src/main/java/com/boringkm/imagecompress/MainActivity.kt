package com.boringkm.imagecompress

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : ComponentActivity() {
  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
      if (isGranted) {
        openImagePicker()
      }
    }

  private val pickImageLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        result.data?.data?.let { uri ->
          imageUriState.value = uri

          val inputStream = contentResolver.openInputStream(uri)
          if (inputStream != null) {
            val bytes = inputStream.available() ?: 0
            originalImageKB.value = bytes / 1024
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val quality = 75

            val outputPath = filesDir.absolutePath + "/output.webp"
            saveBitmapAsWebP(bitmap, outputPath, quality)
            inputStream.close()
          }
        }
      }
    }

  private fun saveBitmapAsWebP(bitmap: Bitmap, outputPath: String, quality: Int) {
    var fos: FileOutputStream? = null
    try {
      val file = File(outputPath)
      if (!file.exists()) {
        file.createNewFile()
      } else {
        file.delete()
      }

      fos = FileOutputStream(file)

      // WebP 형식으로 압축된 이미지를 저장
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, fos)
      } else {
        @Suppress("DEPRECATION")
        bitmap.compress(Bitmap.CompressFormat.WEBP, quality, fos)
      }
      Log.d("MainActivity", "WebP 이미지가 저장되었습니다: $outputPath")
    } catch (e: IOException) {
      e.printStackTrace()
      Log.e("MainActivity", "WebP 이미지 저장 중 오류 발생: " + e.message)
    } finally {
      if (fos != null) {
        try {
          fos.close()

          // get compressed image size
          val bytes = File(outputPath).length().toInt()
          compressedImageKB.intValue = bytes / 1024

          // get uri of compressed image
          compressedImageUriState.value = Uri.parse(outputPath)

        } catch (e: IOException) {
          e.printStackTrace()
        }
      }
    }
  }

  private val originalImageKB = mutableIntStateOf(0)
  private val compressedImageKB = mutableIntStateOf(0)

  private val imageUriState = mutableStateOf<Uri?>(null)
  private val compressedImageUriState = mutableStateOf<Uri?>(null)

  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Scaffold {
        val imageUri by imageUriState
        val compressedImageUri by compressedImageUriState
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (imageUri != null) {
            AsyncImage(
              model = imageUri!!,
              contentDescription = "Selected Image",
              contentScale = ContentScale.Fit,
              modifier = Modifier.height(200.dp)
            )
            AsyncImage(
              model = compressedImageUri!!,
              contentDescription = "Compressed Image",
              contentScale = ContentScale.Fit,
              modifier = Modifier.height(200.dp)
            )
            Text(text = "Original Image Size: ${originalImageKB.intValue} KB")
            Text(text = "Compressed Image Size: ${compressedImageKB.intValue} KB")
          }
          Button(
            onClick = {
              requestPermissionAndOpenImagePicker()
            },
            modifier = Modifier.padding(8.dp)
          ) {
            Text(text = "Select Image")
          }
        }
      }
    }
  }

  private fun openImagePicker() {
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    pickImageLauncher.launch(intent)
  }

  private fun requestPermissionAndOpenImagePicker() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        openImagePicker()
      } else {
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
      }
    } else {
      if (ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        openImagePicker()
      } else {
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
      }
    }
  }
}

