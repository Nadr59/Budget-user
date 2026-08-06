package com.nadrlab.baitbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    PURCHASE,
    PAYMENT
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Store::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storeId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeId: Long,
    val amount: Double,
    val description: String = "",
    val type: TransactionType,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val senderTag: String = ""
)
