package com.chplalex.Test360kt.galleries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.utils.*
import com.dermandar.viewerlibrary.DMDViewer
import kotlinx.android.synthetic.main.list_item.view.*
import kotlinx.coroutines.Job

class ThumbsAdapter(private val sourceList: List<SourceData>) :
    RecyclerView.Adapter<ThumbsAdapter.ThumbsViewHolder>() {

    private val jobs = mutableListOf<Job>()
    private val viewers = mutableListOf<DMDViewer>()

    inner class ImageLoaderCallBack(
        private val itemView: View,
        private val url: String
    ):
        IImageLoaderCallBack {
        override fun onSuccess(path: String) {

            val callBacks = object: DMDViewer.DMDViewerCallBacks {
                override fun onFinishLoading(success: Boolean) {
                }

                override fun onReleased() {
                }

                override fun afterPanoLoad() {
                }

            }

            val viewer = DMDViewer()
            viewers += viewer
            val pano = viewer.init(itemView.context, path, 150, "c", callBacks)
            itemView.image_view_thumbnail.addView(pano)
            itemView.text_view_source.text = url
        }

        override fun onFailure() {
            Toast.makeText(itemView.context, "Image load failure", Toast.LENGTH_SHORT).show()
        }

    }

    inner class ThumbsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(sourceData: SourceData) {
            sourceData.url?.let { url ->
                jobs += loadImage(itemView.context, url, ImageLoaderCallBack(itemView, url))
            }
            itemView.text_view_source.setOnClickListener { view -> DMDViewerActivity.start(view.context, sourceData) }
        }
    }

    override fun getItemCount() = sourceList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ThumbsViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.list_item,
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ThumbsViewHolder, position: Int) {
        holder.bind(sourceList[position])
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        jobs.forEach { job ->  if (job.isActive) job.cancel()}
        viewers.forEach { viewer -> viewer.stopViewer() }
    }

    fun onPause() {
        viewers.forEach { viewer -> viewer.pause() }
    }

    fun onResume() {
        viewers.forEach { viewer -> viewer.resume() }
    }
}