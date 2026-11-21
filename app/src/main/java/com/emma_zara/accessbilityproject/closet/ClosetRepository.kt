package com.emma_zara.accessbilityproject.closet

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emma_zara.accessbilityproject.color.RgbColor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Stores the closet on the phone:
 * - the list of items in a JSON file (closet.json)
 * - each photo as a JPEG in the "closet" folder
 * Both are in the app's private storage, so other apps can't see them.
 */
class ClosetRepository(context: Context) {
    private val dataFile = File(context.filesDir, "closet.json")
    val photoDir = File(context.filesDir, "closet").apply { mkdirs() }

    /** The items. Compose screens re-draw automatically when this changes. */
    var items: List<ClothingItem> by mutableStateOf(load())
        private set

    private val prefs = context.getSharedPreferences("closet", Context.MODE_PRIVATE)

    /** Which categories the user wants suggestions from (matches and outfits). Saved on the phone. */
    var suggestFrom: Set<Category> by mutableStateOf(loadSuggestFrom())
        private set

    fun setSuggestFrom(category: Category, on: Boolean) {
        suggestFrom = if (on) suggestFrom + category else suggestFrom - category
        prefs.edit().putStringSet("suggestFrom", suggestFrom.map { it.name }.toSet()).apply()
    }

    private fun loadSuggestFrom(): Set<Category> {
        val saved = prefs.getStringSet("suggestFrom", null) ?: return Category.entries.toSet()
        return Category.entries.filter { it.name in saved }.toSet()
    }

    // ---- The user's own types and patterns ----

    var customTypes: List<CustomType> by mutableStateOf(loadCustomTypes())
        private set
    var customPatterns: List<CustomPattern> by mutableStateOf(loadCustomPatterns())
        private set

    /** Why [name] can't be used for a new type, or null if it's fine. */
    fun problemWithTypeName(name: String): String? {
        val n = name.trim()
        return when {
            n.isEmpty() -> "Type a name."
            Category.entries.any { n.equals(it.label, true) || n.equals(it.singular, true) } -> "That one already exists."
            customTypes.any { it.name.equals(n, true) } -> "You already made that one."
            else -> null
        }
    }

    /** Why [name] can't be used for a new pattern, or null if it's fine. */
    fun problemWithPatternName(name: String): String? {
        val n = name.trim()
        return when {
            n.isEmpty() -> "Type a name."
            Pattern.entries.any { n.equals(it.label, true) } -> "That one already exists."
            customPatterns.any { it.name.equals(n, true) } -> "You already made that one."
            else -> null
        }
    }

    fun addCustomType(type: CustomType) {
        customTypes = customTypes + type.copy(name = type.name.trim())
        saveCustoms()
    }

    fun addCustomPattern(pattern: CustomPattern) {
        customPatterns = customPatterns + pattern.copy(name = pattern.name.trim())
        saveCustoms()
    }

    /** Removes a custom type. Items of that type keep what it worked like (for example a jacket becomes a shirt). */
    fun deleteCustomType(type: CustomType) {
        customTypes = customTypes - type
        saveCustoms()
        if (items.any { it.typeName == type.name }) {
            items = items.map { if (it.typeName == type.name) it.copy(typeName = null) else it }
            save()
        }
    }

    /** Removes a custom pattern. Items with it become "Plain" or "Print", depending on whether it was busy. */
    fun deleteCustomPattern(pattern: CustomPattern) {
        customPatterns = customPatterns - pattern
        saveCustoms()
        if (items.any { it.patternName == pattern.name }) {
            items = items.map { if (it.patternName == pattern.name) it.copy(patternName = null) else it }
            save()
        }
    }

    private fun saveCustoms() {
        val types = JSONArray()
        customTypes.forEach { types.put(JSONObject().put("name", it.name).put("worksLike", it.worksLike.name)) }
        val patterns = JSONArray()
        customPatterns.forEach { patterns.put(JSONObject().put("name", it.name).put("busy", it.busy)) }
        prefs.edit()
            .putString("customTypes", types.toString())
            .putString("customPatterns", patterns.toString())
            .apply()
    }

    private fun loadCustomTypes(): List<CustomType> = try {
        val array = JSONArray(prefs.getString("customTypes", "[]"))
        (0 until array.length()).mapNotNull { i ->
            val o = array.getJSONObject(i)
            val worksLike = Category.entries.firstOrNull { it.name == o.getString("worksLike") } ?: return@mapNotNull null
            CustomType(o.getString("name"), worksLike)
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun loadCustomPatterns(): List<CustomPattern> = try {
        val array = JSONArray(prefs.getString("customPatterns", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CustomPattern(o.getString("name"), o.getBoolean("busy"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun photoFor(item: ClothingItem) = File(photoDir, item.photoFile)

    fun find(id: String): ClothingItem? = items.firstOrNull { it.id == id }

    /** Saves the photo and adds a new item. */
    fun add(
        name: String,
        category: Category,
        color: RgbColor,
        pattern: Pattern,
        photo: Bitmap,
        typeName: String? = null,
        patternName: String? = null,
    ): ClothingItem {
        val id = UUID.randomUUID().toString()
        val fileName = "$id.jpg"
        FileOutputStream(File(photoDir, fileName)).use { photo.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        val item = ClothingItem(id, name, category, color, fileName, System.currentTimeMillis(), pattern, typeName, patternName)
        items = items + item
        save()
        return item
    }

    fun update(item: ClothingItem) {
        items = items.map { if (it.id == item.id) item else it }
        save()
    }

    fun delete(item: ClothingItem) {
        photoFor(item).delete()
        items = items.filter { it.id != item.id }
        save()
    }

    private fun save() {
        val array = JSONArray()
        for (item in items) {
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("category", item.category.name)
                    .put("color", item.color.hex)
                    .put("photo", item.photoFile)
                    .put("addedAt", item.addedAt)
                    .put("pattern", item.pattern.name)
                    .put("typeName", item.typeName ?: JSONObject.NULL)
                    .put("patternName", item.patternName ?: JSONObject.NULL),
            )
        }
        dataFile.writeText(array.toString(2))
    }

    private fun load(): List<ClothingItem> {
        if (!dataFile.exists()) return emptyList()
        return try {
            val array = JSONArray(dataFile.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val category = Category.entries.firstOrNull { it.name == o.getString("category") } ?: return@mapNotNull null
                ClothingItem(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    category = category,
                    color = RgbColor.fromArgb(android.graphics.Color.parseColor(o.getString("color"))),
                    photoFile = o.getString("photo"),
                    addedAt = o.getLong("addedAt"),
                    // Items saved before patterns existed count as plain
                    pattern = Pattern.entries.firstOrNull { it.name == o.optString("pattern") } ?: Pattern.SOLID,
                    typeName = o.optString("typeName").takeIf { !o.isNull("typeName") && it.isNotEmpty() },
                    patternName = o.optString("patternName").takeIf { !o.isNull("patternName") && it.isNotEmpty() },
                )
            }
        } catch (e: Exception) {
            Log.e("ClosetRepository", "Could not read closet.json", e)
            emptyList()
        }
    }
}
