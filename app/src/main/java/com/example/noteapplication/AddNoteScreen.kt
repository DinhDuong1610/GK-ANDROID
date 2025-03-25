package com.example.noteapplication

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.noteapp.R
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.InputStream
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val selectImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tạo ghi chú mới", fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Trở về", tint = colorResource(id = R.color.green))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = colorResource(id = R.color.light_gray)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tiêu đề") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.green),
                    unfocusedBorderColor = colorResource(id = R.color.green_forcus)
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Nội dung") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.green),
                    unfocusedBorderColor = colorResource(id = R.color.green_forcus)
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { selectImageLauncher.launch(arrayOf("image/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.green))
            ) {
                Text("Chọn ảnh", color = Color.White)
            }

            imageUri?.let {
                Spacer(modifier = Modifier.height(16.dp))
                val imageName = it.lastPathSegment?.let { segment -> segment.substringAfterLast("/") }
                Text("Đã chọn: $imageName", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        if (imageUri != null) {
                            uploadImageToImgur(
                                context = context,
                                imageUri = imageUri!!,
                                clientId = "caada26be7c7f95",
                                onSuccess = { imageUrl ->
                                    saveNoteToFirestore(db, title, content, imageUrl, navController, context)
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            saveNoteToFirestore(db, title, content, "", navController, context)
                        }
                    } else {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.green))
            ) {
                Text("Tạo ghi chú", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

fun uploadImageToImgur(
    context: Context,
    imageUri: Uri,
    clientId: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
    val imageBytes = inputStream?.readBytes()
    inputStream?.close()

    if (imageBytes == null) {
        onError("Không thể đọc ảnh")
        return
    }

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("image", Base64.encodeToString(imageBytes, Base64.DEFAULT))
        .build()

    val request = Request.Builder()
        .url("https://api.imgur.com/3/image")
        .addHeader("Authorization", "Client-ID $clientId")
        .post(requestBody)
        .build()

    val mainHandler = android.os.Handler(context.mainLooper)

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            mainHandler.post {
                onError("Upload thất bại: ${e.message}")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { json ->
                try {
                    val link = JSONObject(json).getJSONObject("data").getString("link")
                    mainHandler.post {
                        onSuccess(link)
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        onError("Lỗi xử lý kết quả: ${e.message}")
                    }
                }
            } ?: mainHandler.post {
                onError("Không nhận được phản hồi từ server")
            }
        }
    })
}

fun saveNoteToFirestore(
    db: FirebaseFirestore,
    title: String,
    content: String,
    imageUrl: String,
    navController: NavController,
    context: Context
) {
    val note = hashMapOf(
        "title" to title,
        "content" to content,
        "imagePath" to imageUrl
    )

    db.collection("notes")
        .add(note)
        .addOnSuccessListener {
            Toast.makeText(context, "Đã lưu ghi chú!", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.AddNote.route) { inclusive = true }
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Lỗi khi lưu ghi chú: ${it.message}", Toast.LENGTH_LONG).show()
        }
}
