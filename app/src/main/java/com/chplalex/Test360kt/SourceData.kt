package com.chplalex.Test360kt

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class SourceData(val title: String? = null, val url: String? = null) : Parcelable