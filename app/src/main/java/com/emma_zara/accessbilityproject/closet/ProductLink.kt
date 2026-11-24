package com.emma_zara.accessbilityproject.closet

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** What we got from a shopping website link: the product photo (saved in [imageFile]) and its name, if found. */
data class ProductPage(val imageFile: File, val title: String?, val pageUrl: String)

/** A problem with the link, with a message that can be shown to the user as it is. */
class LinkProblem(message: String) : Exception(message)

/**
 * Gets the product photo from a shopping website link.
 *
 * How it works: almost every shopping website puts a "preview picture" in the page for when the link
 * is shared on social media (the og:image tag). We download the page, find that picture and download it.
 * Nothing about the user is sent: it's the same as opening the link in a browser.
 */
object ProductLink {
    private val URL_REGEX = Regex("""(https?://|www\.)[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val META_TAG = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val LINK_TAG = Regex("""<link\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("""([a-zA-Z_:][\w:.-]*)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val TITLE_TAG = Regex("""<title[^>]*>([^<]*)</title>""", RegexOption.IGNORE_CASE)
    private val JSON_IMAGE = Regex(""""image"\s*:\s*\[?\s*(?:\{[^}]*?"url"\s*:\s*)?"([^"]+)"""")

    /** The meta tags that can hold the product photo, best first. */
    private val IMAGE_KEYS = listOf("og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src")

    private const val MAX_PAGE_BYTES = 2_000_000
    private const val MAX_IMAGE_BYTES = 20_000_000

    // Some websites only send the full page to normal browsers, so we say we're a phone browser
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

    /**
     * Finds the link in what the user pasted or shared. Shared text is often like
     * "Look at this on ShopName! https://...", so we take the first link in it.
     * Plain "http" is changed to "https" (Android blocks unsafe http connections).
     */
    fun findUrl(text: String): String? {
        val found = URL_REGEX.find(text)?.value?.trimEnd('.', ',', '!', '?', ')', ']') ?: return null
        return when {
            found.startsWith("https://", ignoreCase = true) -> found
            found.startsWith("http://", ignoreCase = true) -> "https://" + found.substring(7)
            else -> "https://$found"
        }
    }

    /** The photo and product name found in a page's HTML, as full URLs. */
    data class Found(val imageUrl: String?, val title: String?)

    /** Reads the product photo and name from a web page's HTML. [baseUrl] is the page's address. */
    fun parse(html: String, baseUrl: String): Found {
        val metas = META_TAG.findAll(html).map { attributes(it.value) }.toList()
        fun meta(key: String) = metas.firstOrNull {
            (it["property"] ?: it["name"] ?: it["itemprop"]).equals(key, ignoreCase = true) && !it["content"].isNullOrBlank()
        }?.get("content")

        val image = IMAGE_KEYS.firstNotNullOfOrNull { meta(it) }
            ?: meta("image") // itemprop="image"
            ?: LINK_TAG.findAll(html).map { attributes(it.value) }
                .firstOrNull { it["rel"].equals("image_src", ignoreCase = true) }?.get("href")
            ?: JSON_IMAGE.find(html)?.groupValues?.get(1)?.replace("\\/", "/")

        val title = (meta("og:title") ?: meta("twitter:title") ?: TITLE_TAG.find(html)?.groupValues?.get(1))
            ?.let { cleanTitle(decodeEntities(it)) }

        return Found(image?.let { resolve(baseUrl, decodeEntities(it.trim())) }, title)
    }

    /**
     * Downloads the page at [url], finds the product photo and saves it into [dir].
     * Blocks while downloading, so call it off the main thread. Throws [LinkProblem] with a message for the user.
     */
    fun fetch(url: String, dir: File): ProductPage {
        val connection = connect(url)
        try {
            val pageUrl = connection.url.toString()
            val type = connection.contentType.orEmpty()
            // The link may already be a picture
            if (type.startsWith("image/", ignoreCase = true)) {
                return ProductPage(save(connection, dir), null, pageUrl)
            }
            val html = readText(connection)
            val found = parse(html, pageUrl)
            val imageUrl = found.imageUrl ?: throw LinkProblem(
                "Couldn't find a product photo on that page. Make sure it's the link to one product " +
                    "(not a search or a list), or save the picture and choose it from the gallery.",
            )
            connection.disconnect()
            val imageConnection = connect(imageUrl)
            try {
                if (!imageConnection.contentType.orEmpty().startsWith("image/", ignoreCase = true)) {
                    throw LinkProblem("The website's product photo couldn't be downloaded. Save the picture and choose it from the gallery instead.")
                }
                return ProductPage(save(imageConnection, dir), found.title, pageUrl)
            } finally {
                imageConnection.disconnect()
            }
        } catch (e: LinkProblem) {
            throw e
        } catch (e: IOException) {
            throw LinkProblem("Couldn't open the link. Check the internet connection and try again.")
        } finally {
            connection.disconnect()
        }
    }

    /** Opens the link, following redirects (short links like "a.co/..." redirect a few times). */
    private fun connect(url: String): HttpURLConnection {
        var current = url
        repeat(6) {
            val parsed = try {
                URL(current)
            } catch (e: Exception) {
                throw LinkProblem("That doesn't look like a link. Copy the link from the shopping website or app and paste it here.")
            }
            if (parsed.protocol != "https") throw LinkProblem("The app can only open safe (https) links.")
            val c = parsed.openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,image/*;q=0.9,*/*;q=0.8")
            c.setRequestProperty("Accept-Language", "en")
            val code = try {
                c.responseCode
            } catch (e: IOException) {
                c.disconnect()
                throw LinkProblem("Couldn't open the link. Check the internet connection and try again.")
            }
            when (code) {
                in 200..299 -> return c
                in 300..399 -> {
                    val location = c.getHeaderField("Location")
                    c.disconnect()
                    if (location.isNullOrBlank()) throw LinkProblem("The website sent the app nowhere. Try opening the product page and copying its link again.")
                    current = resolve(current, location).replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
                }
                401, 403, 429, 503 -> {
                    c.disconnect()
                    throw LinkProblem(
                        "That website doesn't let apps read its pages. Instead, save the product picture and choose " +
                            "it from the gallery, or use the website's Share button on the picture and pick Color Closet.",
                    )
                }
                404, 410 -> {
                    c.disconnect()
                    throw LinkProblem("That page doesn't exist anymore. Check that the link is complete.")
                }
                else -> {
                    c.disconnect()
                    throw LinkProblem("The website had a problem (error $code). Try again later.")
                }
            }
        }
        throw LinkProblem("The link redirected too many times. Try opening the product page and copying its link again.")
    }

    private fun readText(c: HttpURLConnection): String {
        val charset = Regex("""charset=([\w-]+)""", RegexOption.IGNORE_CASE).find(c.contentType.orEmpty())
            ?.groupValues?.get(1)
            ?.let { runCatching { charset(it) }.getOrNull() }
            ?: Charsets.UTF_8
        val bytes = c.inputStream.use { input ->
            val buffer = ByteArray(16 * 1024)
            val out = java.io.ByteArrayOutputStream()
            while (out.size() < MAX_PAGE_BYTES) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
        return String(bytes, charset)
    }

    private fun save(c: HttpURLConnection, dir: File): File {
        dir.mkdirs()
        val file = File(dir, "link_photo")
        var total = 0L
        c.inputStream.use { input ->
            file.outputStream().use { out ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_IMAGE_BYTES) throw LinkProblem("The product photo is too big to download.")
                    out.write(buffer, 0, n)
                }
            }
        }
        return file
    }

    private fun attributes(tag: String): Map<String, String> =
        ATTRIBUTE.findAll(tag).associate { m ->
            m.groupValues[1].lowercase() to m.groupValues[2].ifEmpty { m.groupValues[3] }
        }

    private fun resolve(base: String, link: String): String = try {
        URL(URL(base), link).toString()
    } catch (e: Exception) {
        link
    }

    /** Website titles are often like "Cotton T-Shirt | ShopName": keep the product part, and not too long. */
    private fun cleanTitle(title: String): String? {
        val t = title.split(" | ").first().replace(Regex("\\s+"), " ").trim()
        if (t.isEmpty()) return null
        return if (t.length <= 60) t else t.take(57).trimEnd() + "…"
    }

    /** Turns "&amp;" into "&" and so on (web pages write some characters this way). */
    fun decodeEntities(text: String): String =
        Regex("""&(#x[0-9a-fA-F]+|#\d+|amp|quot|apos|lt|gt|nbsp|#39);""").replace(text) { m ->
            when (val e = m.groupValues[1]) {
                "amp" -> "&"
                "quot" -> "\""
                "apos", "#39" -> "'"
                "lt" -> "<"
                "gt" -> ">"
                "nbsp" -> " "
                else -> {
                    val code = if (e.startsWith("#x", true)) e.substring(2).toIntOrNull(16) else e.substring(1).toIntOrNull()
                    code?.let { String(Character.toChars(it)) } ?: m.value
                }
            }
        }
}
