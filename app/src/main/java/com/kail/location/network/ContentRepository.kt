package com.kail.location.network

/** Tries validated remote content, the last valid response, then bundled content. */
internal class ContentRepository(
    private val fetchRemote: ((String) -> String)?,
    private val readCache: (String) -> String?,
    private val writeCache: (String, String) -> Unit,
    private val readBundled: (String) -> String
) {
    fun <T> load(name: String, parse: (String) -> T): Result<T> {
        val fetch = fetchRemote
        if (fetch != null) {
            val remote = runCatching {
                val json = fetch(name)
                val content = parse(json)
                // Storage failure must not discard a valid remote response.
                runCatching { writeCache(name, json) }
                content
            }
            if (remote.isSuccess) return remote
            val cached = runCatching { readCache(name)?.let(parse) }
            cached.getOrNull()?.let { return Result.success(it) }
        }
        return runCatching { parse(readBundled(name)) }
    }
}
