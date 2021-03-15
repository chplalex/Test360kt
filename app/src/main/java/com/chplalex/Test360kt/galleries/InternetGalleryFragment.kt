package com.chplalex.Test360kt.galleries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.chplalex.Test360kt.R
import com.google.android.material.button.MaterialButton

class InternetGalleryFragment : Fragment(R.layout.fragment_internet_gallery) {

    private lateinit var adapter: ThumbsAdapter
/*
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
        ),
        SourceData(
            "Cylindrical pano",
            "http://localhost/pano.jpg"
        )
    )
*/

    private val sourceList = listOf(
//        SourceData(
//            "Pano-01",
//            "https://cdn-p.cian.site/images/45/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745491-1.jpg"
//        ),
//        SourceData(
//            "Pano-02",
//            "https://cdn-p.cian.site/images/45/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745449-1.jpg"
//        ),
//        SourceData(
//            "Pano-03",
//            "https://cdn-p.cian.site/images/45/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745499-1.jpg"
//        ),
//        SourceData(
//            "Pano-04",
//            "https://cdn-p.cian.site/images/55/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745509-1.jpg"
//        ),
//        SourceData(
//            "Pano-05",
//            "https://cdn-p.cian.site/images/55/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745515-1.jpg"
//        ),
//        SourceData(
//            "Pano-06",
//            "https://cdn-p.cian.site/images/55/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745522-1.jpg"
//        ),
//        SourceData(
//            "Pano-07",
//            "https://cdn-p.cian.site/images/55/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745536-1.jpg"
//        ),
        SourceData(
            "Pano-08",
            "https://cdn-p.cian.site/images/55/472/001/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1002745560-1.jpg"
        ),
//        SourceData(
//            "Pano-09",
//            "https://cdn-p.cian.site/images/58/183/401/kvartira-moskva-6y-novopodmoskovnyy-pereulok-1043818573-1.jpg"
//        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.label_gallery_internet)
        adapter = ThumbsAdapter(sourceList)
        view.findViewById<RecyclerView>(R.id.rvThumbs)?.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        adapter.onPause()
    }

    override fun onResume() {
        super.onResume()
        adapter.onResume()
    }
}