package ir.ersaz.sandogh

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ir.ersaz.sandogh.databinding.ItemVaultBinding

class VaultAdapter(
    private var items: List<VaultItem>,
    private val onClick: (VaultItem) -> Unit
) : RecyclerView.Adapter<VaultAdapter.VH>() {

    class VH(val b: ItemVaultBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemVaultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val it = items[position]
        h.b.name.text = it.name
        h.b.size.text = formatSize(it.size)
        h.b.icon.setImageResource(iconFor(it.category))
        h.b.root.setOnClickListener { onClick(it) }
    }

    fun submit(newItems: List<VaultItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun iconFor(category: String) = when (category) {
        VaultItem.IMAGE -> R.drawable.ic_image
        VaultItem.VIDEO -> R.drawable.ic_video
        VaultItem.AUDIO -> R.drawable.ic_audio
        else -> R.drawable.ic_doc
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val units = arrayOf("B", "KB", "MB", "GB")
        var s = bytes.toDouble()
        var i = 0
        while (s >= 1024 && i < units.size - 1) { s /= 1024; i++ }
        return if (i == 0) "${bytes} ${units[i]}" else String.format("%.1f %s", s, units[i])
    }
}
