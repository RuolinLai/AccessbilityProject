package com.emma_zara.accessbilityproject.closet

import com.emma_zara.accessbilityproject.color.RgbColor

/** The clothing categories. [singular] is used for automatic names like "Navy Shirt". */
enum class Category(val label: String, val singular: String) {
    SHIRTS("Shirts", "Shirt"),
    PANTS("Pants", "Pants"),
    SKIRTS("Skirts", "Skirt"),
    DRESSES("Dresses", "Dress"),
    SHOES("Shoes", "Shoes"),
    BAGS("Bags", "Bag"),
    JEWELLERY("Jewellery", "Jewellery"),
}

/** One piece of clothing (or jewellery) in the digital closet. */
data class ClothingItem(
    val id: String,
    val name: String,
    val category: Category,
    val color: RgbColor,
    /** File name of the photo inside the app's private "closet" folder. */
    val photoFile: String,
    /** When it was added (milliseconds since 1970), used for "Newest" sorting. */
    val addedAt: Long,
)
