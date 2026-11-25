package com.js10bii.calculator.support

import org.junit.Assert.fail

class Row(private val header: List<String>, private val cells: List<String>) {
    fun str(name: String) = cells[header.indexOf(name)]
    fun num(name: String) = str(name).toDouble()
    fun flag(name: String) = str(name).toBooleanStrict()
    override fun toString() = header.zip(cells).joinToString(" ") { (h, c) -> "$h=$c" }
}

object Golden {
    fun load(file: String): List<Row> {
        val text = checkNotNull(Golden::class.java.getResource("/golden/$file")) { "missing golden/$file" }.readText()
        val lines = text.trim().lines()
        val header = lines.first().split(",")
        return lines.drop(1).map { Row(header, it.split(",")) }
    }
}

/** Runs [check] on every case; it returns null when fine or a description of the problem. Reports all failures at once. */
fun <T> checkAll(cases: List<T>, check: (T) -> String?) {
    check(cases.isNotEmpty()) { "no cases" }
    val bad = cases.mapNotNull { c ->
        val problem = try { check(c) } catch (e: ArithmeticException) { "threw ${e.message}" }
        problem?.let { "$c -> $it" }
    }
    if (bad.isNotEmpty()) {
        // Evenly spaced sample so one corner of the grid does not hide the others.
        val sample = bad.filterIndexed { i, _ -> i % maxOf(1, bad.size / 12) == 0 }
        fail("${bad.size} of ${cases.size} cases failed, e.g.\n  " + sample.joinToString("\n  "))
    }
}

fun near(expected: Double, actual: Double, tol: Double): String? =
    if (Math.abs(expected - actual) <= tol) null else "expected $expected got $actual"
