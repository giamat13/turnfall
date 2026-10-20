package io.github.giamat13.turnfall.game

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The reference answers, written by tools/make-android-fixtures.js from the page's own src/core.js. The tests check
 * the Kotlin port against them, one board at a time.
 */
internal fun fixture(name: String): JsonElement {
    val text = JsonElement::class.java.getResource("/fixtures/$name")!!.readText()
    return Json.parseToJsonElement(text)
}

internal fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

internal fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int

internal fun JsonObject.bool(key: String): Boolean = getValue(key).jsonPrimitive.int == 1

internal fun JsonObject.rows(key: String = "rows"): List<String> =
    getValue(key).jsonArray.map { it.jsonPrimitive.content }

internal fun JsonElement.isNull(): Boolean = this is JsonNull

internal fun JsonElement.asFound(): Found? {
    if (isNull()) return null
    val o = jsonObject
    return Found(o.int("par"), o.str("moves"))
}

internal fun JsonElement.asInts(): List<Int> = jsonArray.map { it.jsonPrimitive.int }

internal fun JsonElement.asArray(): JsonArray = jsonArray

/** Compares two possibly-missing results, with the test's message. */
internal fun assertSameFound(expected: Found?, actual: Found?, message: String) {
    if (expected == null || actual == null) {
        org.junit.Assert.assertEquals(message, expected == null, actual == null)
        return
    }
    org.junit.Assert.assertEquals("$message: par", expected.par, actual.par)
    org.junit.Assert.assertEquals("$message: moves", expected.moves, actual.moves)
}
