package com.emma_zara.accessbilityproject.closet

import com.emma_zara.accessbilityproject.color.ColorNames
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

/** The pattern on the fabric. */
enum class Pattern(val label: String, val description: String) {
    SOLID("Plain", "One plain color, no pattern"),
    STRIPES("Stripes", "Lines going one way"),
    CHECKS("Checks / plaid", "Lines crossing each other"),
    PRINT("Print", "Dots, flowers, logos or other prints");

    /** Patterned pieces are "busy": wear one at a time. */
    val busy: Boolean get() = this != SOLID
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
    val pattern: Pattern = Pattern.SOLID,
    /** The user's own type (like "Jacket"), or null for a built-in one. [category] is then what it works like. */
    val typeName: String? = null,
    /** The user's own pattern (like "Floral"), or null. [pattern] is then SOLID (calm) or PRINT (busy). */
    val patternName: String? = null,
    /** True if it's on the wish list (thinking of buying it), not in the closet yet. */
    val wished: Boolean = false,
    /** Other colors on it besides the main [color], like the white in navy-and-white stripes. */
    val extraColors: List<RgbColor> = emptyList(),
)

/**
 * A type the user made up, like "Jacket" or "Hat".
 * [worksLike] tells the app where it goes in an outfit (a jacket works like a top, a hat like jewellery).
 */
data class CustomType(val name: String, val worksLike: Category)

/** A pattern the user made up, like "Floral" or "Lace". [busy] = true means wear it with plain pieces. */
data class CustomPattern(val name: String, val busy: Boolean) {
    /** The built-in pattern it counts as for matching. */
    val asPattern: Pattern get() = if (busy) Pattern.PRINT else Pattern.SOLID
}

/** What each built-in category does in an outfit, for choosing what a custom type works like. */
val Category.role: String
    get() = when (this) {
        Category.SHIRTS -> "a top"
        Category.PANTS -> "pants (a bottom)"
        Category.SKIRTS -> "a skirt (a bottom)"
        Category.DRESSES -> "a one-piece, like a dress"
        Category.SHOES -> "shoes"
        Category.BAGS -> "a bag"
        Category.JEWELLERY -> "an accessory, like jewellery"
    }

/** The type to show, like "Shirt" or the user's own "Jacket". */
val ClothingItem.typeLabel: String get() = typeName ?: category.singular

/** The pattern to show, like "Stripes" or the user's own "Floral". */
val ClothingItem.patternLabel: String get() = patternName ?: pattern.label

/** The color names on the item, main one first, like "Navy & White". */
fun ClothingItem.colorNames(): String =
    (listOf(color) + extraColors).map { ColorNames.nearest(it).name }.distinct().joinToString(" & ")

/** Short description for lists, like "Navy & White, stripes · Shirt". */
fun ClothingItem.summary(): String = buildString {
    append(colorNames())
    if (pattern.busy || patternName != null) append(", ${patternLabel.lowercase()}")
    append(" · $typeLabel")
}
