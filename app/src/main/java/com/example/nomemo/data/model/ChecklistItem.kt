package com.example.nomemo.data.model

import androidx.annotation.Keep
import java.util.UUID

@Keep
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)