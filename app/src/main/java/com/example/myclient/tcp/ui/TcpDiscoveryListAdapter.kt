package com.example.myclient.tcp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myclient.databinding.DeviceListItemBinding

class TcpDiscoveryListAdapter(
    private var itemClickListener: ((DiscoveryItem) -> Unit)? = null,
    private var connectClickListener: ((DiscoveryItem) -> Unit)? = null,
) : ListAdapter<DiscoveryItem, RecyclerView.ViewHolder>(BleDevicesDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MoveRecordsItemViewHolder(
            DeviceListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            { position ->
                itemClickListener?.invoke(getItem(position))
            }, { position ->
                connectClickListener?.invoke(getItem(position))
            })
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MoveRecordsItemViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private object BleDevicesDiffUtil : DiffUtil.ItemCallback<DiscoveryItem>() {
        override fun areItemsTheSame(oldItem: DiscoveryItem, newItem: DiscoveryItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: DiscoveryItem, newItem: DiscoveryItem): Boolean {
            return oldItem == newItem
        }
    }
    internal class MoveRecordsItemViewHolder(
        private val binding: DeviceListItemBinding,
        itemClickListener: ((Int) -> Unit)? = null,
        connectClickListener: ((Int) -> Unit)? = null
    ): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.container.setOnClickListener { itemClickListener?.invoke(adapterPosition) }
            binding.btnConnect.setOnClickListener { connectClickListener?.invoke(adapterPosition) }
        }

        fun bind(item: DiscoveryItem) {
            binding.txtTitle.text = item.name
            binding.txtDescription.text = item.toDescription()
        }
    }
}

private fun DiscoveryItem.toDescription(): String {
    return "Host: " + this.host +" port: " + this.port + " type: " + this.type
}

data class DiscoveryItem(
    val name: String,
    val host: String?,
    val port: Int,
    val type: String,
)
