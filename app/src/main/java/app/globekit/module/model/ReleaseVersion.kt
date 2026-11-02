package app.globekit.module.model

object ReleaseVersion {
    fun isNewer(latest: String, current: String): Boolean? {
        val remote = parse(latest) ?: return null
        val local = parse(current) ?: return null
        for (index in 0 until maxOf(remote.size, local.size)) {
            val comparison = (remote.getOrNull(index) ?: 0L).compareTo(local.getOrNull(index) ?: 0L)
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    private fun parse(value: String): List<Long>? {
        val tag = value.trim().removePrefix("v").removePrefix("V")
        if (!tag.matches(Regex("[0-9]+(\\.[0-9]+)*"))) return null
        return tag.split('.').map { it.toLongOrNull() ?: return null }
    }
}
