package app.globekit.module

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

internal fun <T> Spinner.setItems(items: List<T>) {
    adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items).also {
        it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}

internal fun Spinner.onSelectionChanged(action: () -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action()
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}

/** The adapter must own a mutable collection; notify only after the batch is complete. */
internal fun <T> ArrayAdapter<T>.replaceItems(items: List<T>) {
    setNotifyOnChange(false)
    clear()
    addAll(items)
    notifyDataSetChanged()
}
