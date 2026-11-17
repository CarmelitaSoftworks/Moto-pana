package com.carmelitasoftworks.motointercom.core.service

import kotlin.random.Random

/** The 6-character code riders share to join the same group (CLAUDE.md §4). */
object GroupCode {
    const val LENGTH = 6

    /** No 0/O, 1/I/L: easy to read out loud and to type with gloves. */
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    /** Uppercases and drops anything that cannot be part of a code (spaces, dashes...). */
    fun normalize(input: String): String = input.uppercase().filter { it in ALPHABET }.take(LENGTH)

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}
