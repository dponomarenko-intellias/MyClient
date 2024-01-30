package com.example.myclient.ble.ui

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myclient.databinding.BleDevicesListItemBinding

class BleDevicesAdapter(
    private var itemClickListener: ((BleDeviceItem) -> Unit)? = null,
    private var connectClickListener: ((BleDeviceItem) -> Unit)? = null,
) : ListAdapter<BleDeviceItem, RecyclerView.ViewHolder>(BleDevicesDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MoveRecordsItemViewHolder(
            BleDevicesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
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

    private object BleDevicesDiffUtil : DiffUtil.ItemCallback<BleDeviceItem>() {
        override fun areItemsTheSame(oldItem: BleDeviceItem, newItem: BleDeviceItem): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BleDeviceItem, newItem: BleDeviceItem): Boolean {
            return oldItem == newItem
        }
    }
    internal class MoveRecordsItemViewHolder(
        private val binding: BleDevicesListItemBinding,
        itemClickListener: ((Int) -> Unit)? = null,
        connectClickListener: ((Int) -> Unit)? = null
    ): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.container.setOnClickListener { itemClickListener?.invoke(adapterPosition) }
            binding.btnConnect.setOnClickListener { connectClickListener?.invoke(adapterPosition) }
        }

        fun bind(item: BleDeviceItem) {
            binding.txtTitle.text = item.name
            binding.txtDescription.text = item.info
        }
    }
}

data class BleDeviceItem(
//    val id: Int,
    val address: String,
    val name: String,
    val info: String,
//    val device: BluetoothDevice,
)
