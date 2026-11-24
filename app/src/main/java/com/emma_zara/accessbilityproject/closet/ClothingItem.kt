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
    /** The shopping website page it came from, or null if it was photographed or picked from the gallery. */
    val link: String? = null,
    /** Where it's good to wear. Empty means the user didn't say, so it can be used for anything. */
    val occasions: Set<Occasion> = emptySet(),
    /** What weather it's for, or null if it's fine in any weather. */
    val warmth: Warmth? = null,
)

/** Where you're going, for outfit recommendations. */
enum class Occasion(val label: String, val tip: String) {
    SCHOOL("School", "Comfortable pieces you can sit and move in all day."),
    FORMAL("Formal", "Plain pieces in calm colors like black, navy, white, gray or beige look the dressiest. Skip busy patterns."),
    SPORT("Sport", "Stretchy, breathable clothes and sports shoes. Leave jewellery at home so it doesn't get lost or hurt you."),
    HOME("Home", "Comfy clothes for staying in, like pajamas or loungewear."),
}

/** Names that sound like clothes only worn at home (pajamas, loungewear...). */
private val HomeWords = Regex("""pajama|pyjama|\bpjs?\b|sleep|night ?gown|nightie|nightwear|lounge|\brobe|bathrobe""", RegexOption.IGNORE_CASE)

/**
 * True if this piece is only for home: marked only "Home", or not marked at all but named like
 * sleepwear (like the user's own type "Pajama"). Home pieces are never mixed with going-out clothes.
 */
val ClothingItem.homeOnly: Boolean
    get() = occasions == setOf(Occasion.HOME) ||
        (occasions.isEmpty() && HomeWords.containsMatchIn("${typeName.orEmpty()} $name"))

/** True if the app guessed [homeOnly] from the name (the user didn't choose it). */
val ClothingItem.homeGuessed: Boolean get() = occasions.isEmpty() && homeOnly

/** Where on the body a piece goes. Two pieces in the same spot can't be worn together. */
enum class Slot { TOP, BOTTOM, ONE_PIECE, FEET, BAG, ACCESSORY }

val Category.slot: Slot
    get() = when (this) {
        Category.SHIRTS -> Slot.TOP
        Category.PANTS, Category.SKIRTS -> Slot.BOTTOM
        Category.DRESSES -> Slot.ONE_PIECE
        Category.SHOES -> Slot.FEET
        Category.BAGS -> Slot.BAG
        Category.JEWELLERY -> Slot.ACCESSORY
    }

/**
 * Can these two kinds of pieces be part of one outfit? Not two in the same spot (pants with a skirt),
 * and not a dress with a top or bottom (a dress already is both).
 */
fun canWearTogether(a: Category, b: Category): Boolean {
    val sa = a.slot
    val sb = b.slot
    if (sa == sb) return false
    val clothes = setOf(Slot.TOP, Slot.BOTTOM)
    if (sa == Slot.ONE_PIECE && sb in clothes || sb == Slot.ONE_PIECE && sa in clothes) return false
    return true
}

/**
 * Do these two pieces suit the same places? Home-only pieces only go with other home pieces,
 * and two pieces marked for completely different occasions (like Sport and Formal) don't mix.
 * Pieces with no occasions chosen fit anywhere (except with home-only pieces).
 */
fun sameOccasion(a: ClothingItem, b: ClothingItem): Boolean {
    val aHome = a.homeOnly
    val bHome = b.homeOnly
    if (aHome || bHome) {
        fun homeOk(x: ClothingItem) = x.homeOnly || Occasion.HOME in x.occasions
        return homeOk(a) && homeOk(b)
    }
    if (a.occasions.isEmpty() || b.occasions.isEmpty()) return true
    return a.occasions.any { it in b.occasions }
}

/** What weather a piece is for. */
enum class Warmth(val label: String, val description: String) {
    LIGHT("Hot days", "Light and thin, like a T-shirt, shorts or a summer dress"),
    WARM("Cold days", "Warm and thick, like a sweater, a coat or boots"),
}

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
