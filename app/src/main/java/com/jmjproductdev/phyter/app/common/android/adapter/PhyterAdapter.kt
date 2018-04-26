package com.jmjproductdev.phyter.app.common.android.adapter

import android.content.Context
import android.support.v4.view.ViewPropertyAnimatorCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.jmjproductdev.phyter.R
import com.jmjproductdev.phyter.core.instrument.Phyter
import com.mikepenz.itemanimators.BaseItemAnimator
import kotlin.properties.Delegates

/**
 * View holder for a [Phyter] (or just a footer) in a [RecyclerView].
 */
class PhyterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

  /* views */

  private val footerText: TextView?
    get() = itemView.findViewById(R.id.footerText)
  private val nameText: TextView?
    get() = itemView.findViewById(R.id.nameText)
  private val addressText: TextView?
    get() = itemView.findViewById(R.id.addressText)
  private val rssiIcon: ImageView?
    get() = itemView.findViewById(R.id.rssiIcon)

  /* callbacks */

  var onClickListener: ((PhyterViewHolder) -> Unit)? = null

  /* binding functions */

  fun bindAsDevice(phyter: Phyter) {
    itemView.setOnClickListener { onClickListener?.invoke(this@PhyterViewHolder) }
    nameText?.text = phyter.name
    addressText?.text = phyter.address
    rssiIcon?.apply {
      when {
        phyter.rssi > -65       -> setImageDrawable(context.getDrawable(R.drawable.ic_signal_full_bars))
        phyter.rssi in -75..-65 -> setImageDrawable(context.getDrawable(R.drawable.ic_signal_3_bars))
        phyter.rssi in -85..-75 -> setImageDrawable(context.getDrawable(R.drawable.ic_signal_2_bars))
        else                    -> setImageDrawable(context.getDrawable(R.drawable.ic_signal_1_bar))
      }
    }
  }

  fun bindAsFooter(text: String) {
    footerText?.text = text
  }

}

/**
 * [RecyclerView] adapter for displaying a list of PhyterApp devices.
 */
class PhytersAdapter(val context: Context) : RecyclerView.Adapter<PhyterViewHolder>() {

  companion object {
    /**
     * View type for a footer view
     */
    const val VIEW_TYPE_FOOTER = 0
    /**
     * View type for a PhyterApp
     */
    const val VIEW_TYPE_PHYTER = 1
  }

  /**
   * The text to be displayed on the footer view. Changes to this property are automatically applied to the view
   * and the adapter refreshed.
   */
  var footerText: String by Delegates.observable("") { _, _, _ -> refreshFooter() }

  var onItemClickListener: ((device: Phyter, position: Int) -> Unit)? = null

  private val inflater: LayoutInflater by lazy { LayoutInflater.from(context) }
  private val devices = mutableListOf<Phyter>()
  private val footerIndex: Int
    get() = devices.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhyterViewHolder {
    val layoutId = if (viewType == VIEW_TYPE_FOOTER) R.layout.rv_footer_single_text else R.layout.rv_item_phyter
    with(inflater.inflate(layoutId, parent, false)) { return PhyterViewHolder(this) }
  }

  override fun getItemCount(): Int = devices.size + 1 // add one for footer

  override fun onBindViewHolder(holder: PhyterViewHolder, position: Int) {
    if (getItemViewType(position) == VIEW_TYPE_FOOTER) {
      holder.bindAsFooter(footerText)
    } else {
      holder.onClickListener = { onItemClickListener?.invoke(devices[it.adapterPosition], it.adapterPosition) }
      holder.bindAsDevice(devices[position])
    }
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == footerIndex)
      VIEW_TYPE_FOOTER
    else
      VIEW_TYPE_PHYTER
  }

  /**
   * Add a device.
   */
  fun add(phyter: Phyter) {
    devices.add(phyter)
    notifyItemInserted(devices.size - 1)
  }

  /**
   * Refresh a device that matches the one provided.
   * @return true on success, false if a matching device was not found.
   */
  fun refresh(device: Phyter): Boolean {
    val index = devices.indexOfFirst { it.address == device.address }
    if (index < 0) return false
    devices[index] = device
    notifyItemChanged(index)
    return true
  }

  /**
   * Remove a device that matches the one provided.
   * @return true on success, false if a matching device was not found.
   */
  fun remove(device: Phyter): Boolean {
    val index = devices.indexOfFirst { it.address == device.address }
    if (index < 0) return false
    devices.removeAt(index)
    notifyItemRemoved(index)
    return true
  }

  /**
   * Clear all devices.
   */
  fun clear() {
    val count = devices.size
    devices.clear()
    notifyItemRangeRemoved(0, count)
  }

  private fun refreshFooter() {
    notifyItemChanged(footerIndex)
  }
}

