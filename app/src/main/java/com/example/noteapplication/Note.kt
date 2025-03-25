package com.example.noteapplication

import com.google.firebase.firestore.Exclude

data class Note(
    @Exclude var courseID: String? = "",
    var title: String? = "",
    var content: String? = "",
    var image: String? = ""
)
