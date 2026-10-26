package com.potatameister.barenotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.potatameister.barenotes.databinding.ItemNoteBinding

class NoteAdapter(
    private val onClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val items = mutableListOf<Note>()
    private var showCreated = false

    fun setShowCreated(show: Boolean) {
        if (showCreated != show) {
            showCreated = show
            notifyDataSetChanged()
        }
    }

    fun submit(newItems: List<Note>) {
        val oldItems = items.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldItems[oldItemPosition].id == newItems[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldItems[oldItemPosition] == newItems[newItemPosition]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            val context = binding.root.context
            binding.noteTitle.setTextColor(ContextCompat.getColor(context, R.color.text))
            binding.notePreview.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            binding.noteDate.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            binding.noteDivider.setBackgroundColor(
                ContextCompat.getColor(context, R.color.divider)
            )
            binding.noteTitle.text = note.title.ifBlank {
                context.getString(R.string.untitled)
            }
            binding.notePreview.text = note.body
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?: context.getString(R.string.no_body)
            val time = if (showCreated) note.createdAt else note.updatedAt
            binding.noteDate.text = context.getString(
                if (showCreated) R.string.created_relative else R.string.edited_relative,
                NoteTime.relative(context, time)
            )
            binding.root.setOnClickListener { onClick(note) }
        }
    }
}
