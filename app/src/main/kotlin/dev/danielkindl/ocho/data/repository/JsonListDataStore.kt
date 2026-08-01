package dev.danielkindl.ocho.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generic JSON-array-in-a-string-preference CRUD store, shared by the EMOM and Tabata
 * preset repositories (which differ only in item shape and DataStore key).
 *
 * Unparseable stored JSON yields an empty list rather than throwing. Presets are
 * disposable convenience data, so a corrupt blob should cost the user their saved
 * presets, not crash the setup screen every time they open it.
 *
 * @param key the DataStore preference key holding this list's JSON.
 * @param parseItem reads one item from its JSON object.
 * @param serializeItem writes one item into a JSON object.
 * @param idOf extracts the identity used to deduplicate and delete.
 */
class JsonListDataStore<T>(
    private val dataStore: DataStore<Preferences>,
    key: String,
    private val parseItem: (JSONObject) -> T,
    private val serializeItem: (T, JSONObject) -> Unit,
    private val idOf: (T) -> String,
) {
    private val key = stringPreferencesKey(key)

    /** Emits the stored list, and again on every [upsert] or [delete]. */
    fun observe(): Flow<List<T>> =
        dataStore.data.map { prefs -> parseItems(prefs[key] ?: return@map emptyList()) }

    /** Adds [item], replacing any existing entry whose id matches. */
    suspend fun upsert(item: T) {
        dataStore.edit { prefs ->
            val current = parseItems(prefs[key] ?: "[]").toMutableList()
            current.removeAll { idOf(it) == idOf(item) }
            current.add(item)
            prefs[key] = serializeItems(current)
        }
    }

    /** Removes the entry with [id]. A missing id is not an error. */
    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val current = parseItems(prefs[key] ?: "[]").toMutableList()
            current.removeAll { idOf(it) == id }
            prefs[key] = serializeItems(current)
        }
    }

    private fun parseItems(json: String): List<T> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { i -> parseItem(array.getJSONObject(i)) }
    }.getOrDefault(emptyList())

    private fun serializeItems(items: List<T>): String {
        val array = JSONArray()
        items.forEach { item -> array.put(JSONObject().apply { serializeItem(item, this) }) }
        return array.toString()
    }
}
