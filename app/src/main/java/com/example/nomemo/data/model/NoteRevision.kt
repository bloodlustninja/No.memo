package com.example.nomemo.data.model

data class NoteRevision(
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val body: String = "",
)
