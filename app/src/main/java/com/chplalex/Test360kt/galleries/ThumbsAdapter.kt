package com.chplalex.Test360kt.galleries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chplalex.Test360kt.R
import kotlinx.android.synthetic.main.list_item.view.*

class ThumbsAdapter(private val sourceList: List<SourceData>) :
    RecyclerView.Adapter<ThumbsAdapter.ViewHolder>() {

    class ViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(sourceData: SourceData, position: Int) = with(sourceData) {
            with(itemView) {
                txtTitle.text = title
                imgThumb.setOnClickListener { PanoramaActivity.start(context, sourceData) }
                Glide.with(context)
                    .load(url)
                    .circleCrop()
                    .into(imgThumb)
            }
            Unit
        }
    }

    override fun getItemCount() = sourceList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(sourceList[position], position)

}

//private var vrPanoramaView: VrPanoramaView? = null
//
//override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    setContentView(R.layout.activity_main)
//
//    val options = VrPanoramaView.Options().also { it.inputType = VrPanoramaView.Options.TYPE_MONO }
//    vrPanoramaView = findViewById<VrPanoramaView>(R.id.vrPanoramaView)
//    Glide
//        .with(this)
//        .asBitmap()
//        .load("https://pixexid.com/img/mw194el-modern-bedroom-interior-design.jpeg")
//        .into(object : CustomTarget<Bitmap>() {
//            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                vrPanoramaView?.loadImageFromBitmap(resource, options)
//            }
//
//            override fun onLoadCleared(placeholder: Drawable?) {}
//        })
//}
//
//override fun onPause() {
//    super.onPause()
//    vrPanoramaView?.pauseRendering()
//}
//
//override fun onResume() {
//    super.onResume()
//    vrPanoramaView?.resumeRendering()
//}
//
//override fun onDestroy() {
//    vrPanoramaView?.shutdown()
//    super.onDestroy()
//}