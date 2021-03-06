package com.chplalex.Test360kt.galleries

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SourceData(val title: String? = null, val url: String? = null) : Parcelable