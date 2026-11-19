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

    fun photoFor(item: ClothingItem) = File(photoDir, item.photoFile)

    fun find(id: String): ClothingItem? = items.firstOrNull { it.id == id }

    /** Saves the photo and adds a new item. */
    fun add(name: String, category: Category, color: RgbColor, photo: Bitmap): ClothingItem {
        val id = UUID.randomUUID().toString()
        val fileName = "$id.jpg"
        FileOutputStream(File(photoDir, fileName)).use { photo.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        val item = ClothingItem(id, name, category, color, fileName, System.currentTimeMillis())
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
                    .put("addedAt", item.addedAt),
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
                )
            }
        } catch (e: Exception) {
            Log.e("ClosetRepository", "Could not read closet.json", e)
            emptyList()
        }
    }
}
