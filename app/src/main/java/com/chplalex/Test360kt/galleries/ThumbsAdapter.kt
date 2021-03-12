package com.chplalex.Test360kt.galleries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.utils.*
import kotlinx.android.synthetic.main.list_item.view.*

class ThumbsAdapter(private val sourceList: List<SourceData>) :
    RecyclerView.Adapter<ThumbsAdapter.ViewHolder>() {

    class ViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(sourceData: SourceData, position: Int) = with(sourceData) {

            with(itemView) {
                url?.let {
                    val callBack = object : IImageLoaderCallBack {
                        override fun onSuccess(path: String) {
                            text_view_source.text = it
                        }

                        override fun onFailure() {
                            // nothing
                        }
                    }
                    loadImage(context, it, callBack)
                }
            }
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(sourceList[position], position)
}