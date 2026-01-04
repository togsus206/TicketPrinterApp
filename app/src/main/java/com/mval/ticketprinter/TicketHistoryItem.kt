package com.mval.ticketprinter

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName 

@Keep
data class TicketHistoryItem(
    @SerializedName("id") val id: Long,
    @SerializedName("date") val date: Long,
    @SerializedName("total") val total: Double,
    @SerializedName("productCount") val productCount: Int,
    @SerializedName("description") val description: String, // <--- Esto asegura que el texto se guarde y lea bien
    
    // Este campo es interno de la UI, no necesita SerializedName
    var isExpanded: Boolean = false 
)
