package com.chplalex.Test360kt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_internet_gallery.*

class InternetGalleryFragment : Fragment() {

    private val sourceList = listOf(
        SourceData(
            "Гостиная",
            "https://pixexid.com/img/xl1n5ms-a-living-room-filled-with-furniture-and-a-fireplace.jpeg"
        ),
        SourceData(
            "Спальная",
            "https://pixexid.com/img/mw194el-modern-bedroom-interior-design.jpeg"
        ),
        SourceData(
            "Ванная",
            "https://pixexid.com/img/th11zct-idea-for-hall-design.jpeg"
        ),
        SourceData(
            "Уборная",
            "https://pixexid.com/img/2n1n0uvh-interior-design.jpeg"
        ),
        SourceData(
            "Постирочная",
            "https://pixexid.com/img/3x0f1ob-modern-dewanya-at-kuwait.jpeg"
        ),
        SourceData(
            "Гостевая",
            "https://pixexid.com/img/xh9x1c3g-interior-design.jpeg"
        ),
        SourceData(
            "Кабинет",
            "https://pixexid.com/img/fo1rzh7-a-hotel-room.jpeg"
        )
    )

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_internet_gallery, container,false)?.also {
        activity?.title = resources.getString(R.string.label_gallery)
        it.findViewById<RecyclerView>(R.id.rvThumbs)?.adapter =  ThumbsAdapter(sourceList)
    }

}