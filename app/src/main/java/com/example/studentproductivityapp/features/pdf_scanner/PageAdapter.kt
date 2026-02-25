package com.example.studentproductivityapp.features.pdf_scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.R

class PageAdapter(
    private val pages: MutableList<ScanPage>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPage: ImageView = view.findViewById(R.id.ivPage)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeletePage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.ivPage.setImageURI(page.picture)

        holder.btnDelete.setOnClickListener {
            onDelete(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = pages.size
}