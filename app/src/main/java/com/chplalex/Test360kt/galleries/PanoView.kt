package com.chplalex.Test360kt.galleries

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.chplalex.Test360kt.R
import com.dermandar.viewerlibrary.DMDViewer


enum class PanoType(
    val key: String,
    val id: Int
) {
    CYLINDRICAL(key = "c", id = 0),
    SPHERICAL(key = "s", id = 1);

    companion object {
        fun createById(id: Int): PanoType = when (id) {
            0 -> CYLINDRICAL
            1 -> SPHERICAL
            else -> throw IllegalArgumentException("Unknown id ($id) for PanoType class")
        }
    }
}

class PanoView : RelativeLayout {
    private var viewer = DMDViewer()
    private var defaultFov = 150
    private var defaultType = PanoType.CYLINDRICAL
    private val callbacksDef = object : DMDViewer.DMDViewerCallBacks {
        override fun onFinishLoading(success: Boolean) {}
        override fun onReleased() {}
        override fun afterPanoLoad() {}
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs) {
        initAttrs(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(attrs)
    }

    private fun init(
        path: String,
        fov: Int = defaultFov,
        type: PanoType = defaultType,
        callbacks: DMDViewer.DMDViewerCallBacks = callbacksDef
    ) {
        viewer.init(context, path, fov, type.key, callbacks)
    }

    private fun initAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PanoView, 0, 0)

        defaultType = PanoType.createById(typedArray.getInt(R.styleable.PanoView_type, defaultType.id))

        if (defaultType == PanoType.SPHERICAL) {
            defaultFov = 360
        } else {
            defaultFov = typedArray.getInt(R.styleable.PanoView_fov, defaultFov)
        }

        typedArray.recycle()
    }
}