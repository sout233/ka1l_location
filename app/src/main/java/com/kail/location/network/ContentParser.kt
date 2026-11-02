package com.kail.location.network

import org.json.JSONArray

/** The public API uses plain arrays: no tenant, account, token or proprietary envelope. */
internal object ContentParser {
    fun notices(json: String): List<OpenContentClient.NoticeInfo> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OpenContentClient.NoticeInfo(
                id = item.getLong("id"),
                title = item.getString("title"),
                type = item.optInt("type", 0),
                content = item.getString("content"),
                createTime = item.optString("createTime", "")
            )
        }.also { list -> require(list.map { it.id }.distinct().size == list.size) { "Duplicate notice id" } }
    }

    fun faqs(json: String): List<OpenContentClient.FaqItem> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OpenContentClient.FaqItem(
                id = item.getLong("id"),
                title = item.getString("title"),
                answerMd = item.getString("answerMd"),
                sort = item.optInt("sort", 0)
            )
        }.also { list -> require(list.map { it.id }.distinct().size == list.size) { "Duplicate FAQ id" } }
            .sortedBy { it.sort }
    }
}
