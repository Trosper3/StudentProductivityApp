package com.example.studentproductivityapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PageAdapter(
    private val pages: MutableList<ScanPage>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PageAdapter.PageVH>() {

    class PageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
        val textPageLabel: TextView = itemView.findViewById(R.id.textPageLabel)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false)
        return PageVH(v)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val page = pages[position]
        holder.textPageLabel.text = "Page ${position + 1}"

        // Simple thumbnail loading (fine for class projects)
        holder.imgThumb.setImageURI(page.uri)

        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount(): Int = pages.size
}
