package com.chplalex.test360kt.internet

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class SourceData(val title: String? = null, val url: String? = null) : Parcelable