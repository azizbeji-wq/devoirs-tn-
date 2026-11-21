package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant une matière scolaire (ex. Mathématiques, Physique-Chimie, Arabe)
 * stockée localement dans la table `subjects`.
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val subtitle: String,
    val iconKey: String = "school",
    val displayOrder: Int = 0,
    val isCustom: Boolean = false
)
