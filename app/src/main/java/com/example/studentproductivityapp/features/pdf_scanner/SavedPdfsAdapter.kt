package com.example.studentproductivityapp.features.pdf_scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentproductivityapp.R
import com.example.studentproductivityapp.features.pdf_scanner.db.SavedPdf
import java.text.SimpleDateFormat
import java.util.*

class SavedPdfsAdapter(
    private val onClick: (SavedPdf) -> Unit,
    private val onDelete: (SavedPdf) -> Unit
) : ListAdapter<SavedPdf, SavedPdfsAdapter.PdfViewHolder>(PdfDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_pdf, parent, false)
        return PdfViewHolder(view, onClick, onDelete)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PdfViewHolder(itemView: View, val onClick: (SavedPdf) -> Unit, val onDelete: (SavedPdf) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvPdfName: TextView = itemView.findViewById(R.id.tvPdfName)
        private val tvCreationDate: TextView = itemView.findViewById(R.id.tvCreationDate)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private var currentPdf: SavedPdf? = null

        init {
            itemView.setOnClickListener {
                currentPdf?.let {
                    onClick(it)
                }
            }
            btnDelete.setOnClickListener {
                currentPdf?.let {
                    onDelete(it)
                }
            }
        }

        fun bind(pdf: SavedPdf) {
            currentPdf = pdf
            tvPdfName.text = pdf.displayName
            tvCreationDate.text = SimpleDateFormat.getDateTimeInstance().format(Date(pdf.creationTimestamp))
        }
    }
}

object PdfDiffCallback : DiffUtil.ItemCallback<SavedPdf>() {
    override fun areItemsTheSame(oldItem: SavedPdf, newItem: SavedPdf): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SavedPdf, newItem: SavedPdf): Boolean {
        return oldItem == newItem
    }
}
