package com.example.noteapplication

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.noteapp.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNoteScreen(
    navController: NavController,
    noteId: String,
    initialTitle: String,
    initialContent: String,
    initialImageUrl: String
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var imageUrl by remember { mutableStateOf(initialImageUrl) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }

    val selectImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        newImageUri = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chỉnh sửa ghi chú", fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = colorResource(id = R.color.green))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        deleteNoteFromFirestore(db, noteId, context)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xóa", tint = Color.Red)
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
                Text("Chọn ảnh mới", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val previewImage = newImageUri?.toString() ?: imageUrl
            if (previewImage.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(previewImage),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(top = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tiêu đề và nội dung", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (newImageUri != null) {
                        uploadImageToImgur(
                            context = context,
                            imageUri = newImageUri!!,
                            clientId = "caada26be7c7f95",
                            onSuccess = { uploadedUrl ->
                                updateNoteInFirestore(db, noteId, title, content, uploadedUrl, context)
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        updateNoteInFirestore(db, noteId, title, content, imageUrl, context)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.green))
            ) {
                Text("Lưu thay đổi", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}


fun updateNoteInFirestore(
    db: FirebaseFirestore,
    noteId: String,
    title: String,
    content: String,
    imageUrl: String,
    context: Context
) {
    db.collection("notes").document(noteId)
        .update(
            mapOf(
                "title" to title,
                "content" to content,
                "imagePath" to imageUrl
            )
        )
        .addOnSuccessListener {
            Toast.makeText(context, "Đã cập nhật ghi chú!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Lỗi cập nhật: ${it.message}", Toast.LENGTH_LONG).show()
        }
}

fun deleteNoteFromFirestore(
    db: FirebaseFirestore,
    noteId: String,
    context: Context
) {
    db.collection("notes").document(noteId)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Lỗi khi xóa: ${it.message}", Toast.LENGTH_LONG).show()
        }
}
