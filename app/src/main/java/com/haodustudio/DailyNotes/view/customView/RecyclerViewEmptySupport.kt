package com.haodustudio.DailyNotes.view.customView

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.haodustudio.DailyNotes.R

class RecyclerViewEmptySupport @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var emptyView: View? = null

    private val observer = object : AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
    }

    private fun checkIfEmpty() {
        val currentEmptyView = emptyView ?: return
        val currentAdapter = adapter ?: return

        val isEmpty = currentAdapter.itemCount == 0
        currentEmptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        this.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        getAdapter()?.unregisterAdapterDataObserver(observer)

        super.setAdapter(adapter)

        adapter?.registerAdapterDataObserver(observer)

        checkIfEmpty()
    }

    fun setEmptyView(view: View) {
        this.emptyView = view
        checkIfEmpty()
    }
}