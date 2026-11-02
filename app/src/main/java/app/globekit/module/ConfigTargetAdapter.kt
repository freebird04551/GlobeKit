package app.globekit.module

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import app.globekit.module.model.SearchableOption

internal class ConfigTarget(
    val packageName: String?,
    val label: String,
    val subtitle: String,
    val icon: Drawable,
) : SearchableOption {
    override val searchFields: List<String> get() = listOf(label, packageName.orEmpty())
    override fun toString(): String = label

    companion object {
        fun default(context: Context) = ConfigTarget(
            null, context.getString(R.string.default_target_title),
            context.getString(R.string.default_target_subtitle),
            context.packageManager.defaultActivityIcon,
        )

        fun application(context: Context, packageName: String): ConfigTarget {
            val manager = context.packageManager
            return try {
                val info = manager.getApplicationInfo(packageName, 0)
                ConfigTarget(packageName, info.loadLabel(manager).toString(), packageName, info.loadIcon(manager))
            } catch (_: PackageManager.NameNotFoundException) {
                ConfigTarget(packageName, packageName, context.getString(R.string.target_unavailable), manager.defaultActivityIcon)
            } catch (_: SecurityException) {
                ConfigTarget(packageName, packageName, context.getString(R.string.target_unavailable), manager.defaultActivityIcon)
            }
        }
    }
}

internal class ConfigTargetAdapter(context: Context, targets: List<ConfigTarget>) :
    // ArrayAdapter mutates this collection when search results are replaced.
    ArrayAdapter<ConfigTarget>(context, R.layout.item_config_target, targets.toMutableList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_config_target, parent, false)
        val target = requireNotNull(getItem(position))
        view.findViewById<ImageView>(R.id.target_icon).setImageDrawable(target.icon)
        view.findViewById<TextView>(R.id.target_name).text = target.label
        view.findViewById<TextView>(R.id.target_package).text = target.subtitle
        return view
    }
}
