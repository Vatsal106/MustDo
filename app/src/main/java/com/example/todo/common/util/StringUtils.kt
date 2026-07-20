package com.example.todo.common.util

fun String.capitalizeFirstLetter(): String {
    if (this.isEmpty()) return this
    val firstLetterIdx = this.indexOfFirst { it.isLetter() }
    if (firstLetterIdx == -1) return this
    return this.substring(0, firstLetterIdx) + this[firstLetterIdx].uppercase() + this.substring(firstLetterIdx + 1)
}
