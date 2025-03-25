package com.example.noteapplication

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.noteapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var notes by remember { mutableStateOf(listOf<Pair<String, Map<String, String>>>()) }

    DisposableEffect(Unit) {
        val listener = db.collection("notes")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    notes = snapshot.documents.map { doc ->
                        doc.id to mapOf(
                            "title" to (doc["title"] as? String ?: "Không tiêu đề"),
                            "content" to (doc["content"] as? String ?: "Không có nội dung"),
                            "imagePath" to (doc["imagePath"] as? String ?: "")
                        )
                    }
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.duolingo_title_png),
                        contentDescription = "Logo",
                        modifier = Modifier.size(170.dp)
                    )
                },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate(Screen.Signin.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Đăng xuất", tint = colorResource(id = R.color.green))
                    }
                }

            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AddNote.route)
//                    Toast.makeText(context, "Thêm ghi chú mới", Toast.LENGTH_SHORT).show()
                },
                containerColor = colorResource(id = R.color.green),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm", tint = colorResource(id = R.color.white))
            }
        },
        containerColor = colorResource(id = R.color.light_gray)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (notes.isEmpty()) {
                Text("Chưa có ghi chú nào", color = Color.Black)
            }

            notes.forEach { (id, note) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate(
                                Screen.UpdateNote.createRoute(
                                    id,
                                    note["title"] ?: "",
                                    note["content"] ?: "",
                                    note["imagePath"] ?: ""
                                )
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.green_75))
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        val imagePath = note["imagePath"]
                        if (!imagePath.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(Uri.parse(imagePath)),
                                contentDescription = "Hình ảnh ghi chú",
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column {
                            Text(note["title"] ?: "Không tiêu đề", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note["content"] ?: "", color = Color.White, fontSize = 16.sp, maxLines = 2,overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

