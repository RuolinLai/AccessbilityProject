package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.closet.ProductLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductLinkTest {

    @Test
    fun findsTheLinkInSharedText() {
        assertEquals(
            "https://shop.example.com/p/123",
            ProductLink.findUrl("Look at this on ShopName! https://shop.example.com/p/123"),
        )
    }

    @Test
    fun linkWithoutHttpsGetsIt() {
        assertEquals("https://www.example.com/shirt", ProductLink.findUrl("www.example.com/shirt"))
        assertEquals("https://example.com/shirt", ProductLink.findUrl("http://example.com/shirt"))
    }

    @Test
    fun endOfSentencePunctuationIsNotPartOfTheLink() {
        assertEquals("https://example.com/a", ProductLink.findUrl("See https://example.com/a."))
    }

    @Test
    fun noLinkGivesNull() {
        assertNull(ProductLink.findUrl("just some words"))
    }

    @Test
    fun readsOpenGraphImageAndTitle() {
        val html = """
            <html><head>
            <title>Ignored | Shop</title>
            <meta property="og:title" content="Cotton Crew T-Shirt | ShopName">
            <meta content="https://cdn.example.com/img/shirt.jpg?w=800&amp;h=800" property="og:image" />
            </head></html>
        """.trimIndent()
        val found = ProductLink.parse(html, "https://shop.example.com/p/1")
        assertEquals("https://cdn.example.com/img/shirt.jpg?w=800&h=800", found.imageUrl)
        assertEquals("Cotton Crew T-Shirt", found.title)
    }

    @Test
    fun relativeImageIsMadeFull() {
        val html = """<meta name='twitter:image' content='/images/dress.png'><title>Red Dress</title>"""
        val found = ProductLink.parse(html, "https://shop.example.com/products/dress")
        assertEquals("https://shop.example.com/images/dress.png", found.imageUrl)
        assertEquals("Red Dress", found.title)
    }

    @Test
    fun fallsBackToJsonLdImage() {
        val html = """<script type="application/ld+json">{"@type":"Product","image":["https:\/\/cdn.example.com\/bag.webp"]}</script>"""
        assertEquals("https://cdn.example.com/bag.webp", ProductLink.parse(html, "https://example.com/").imageUrl)
    }

    @Test
    fun readsAmazonProductPhoto() {
        val html = """<title>Amazon.ca : Midi Dress</title>
            <img id="main-image" data-a-image-name="mainImage" data-a-hires="https://m.media-amazon.com/images/I/71kb._AC_UY1000_.jpg">
            <script>var data = {"hiRes":"https://m.media-amazon.com/images/I/other.jpg"};</script>"""
        assertEquals("https://m.media-amazon.com/images/I/71kb._AC_UY1000_.jpg", ProductLink.parse(html, "https://www.amazon.ca/dp/1").imageUrl)
    }

    @Test
    fun readsHiResFromScriptsWhenNothingElse() {
        val html = """<script>P.when('A').register("ImageBlockATF", function(){ var d = {"hiRes":"https://m.media-amazon.com/images/I/abc.jpg","thumb":"x"}; });</script>"""
        assertEquals("https://m.media-amazon.com/images/I/abc.jpg", ProductLink.parse(html, "https://www.amazon.ca/dp/1").imageUrl)
    }

    @Test
    fun pageWithoutImageGivesNull() {
        assertNull(ProductLink.parse("<html><title>Search results</title></html>", "https://example.com/").imageUrl)
    }

    @Test
    fun decodesEntities() {
        assertEquals("Tom & Jerry's \"Tee\" é", ProductLink.decodeEntities("Tom &amp; Jerry&#39;s &quot;Tee&quot; &#xE9;"))
    }
}
