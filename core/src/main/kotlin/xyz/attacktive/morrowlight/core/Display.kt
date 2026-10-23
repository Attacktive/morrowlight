package xyz.attacktive.morrowlight.core

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object Display {
	private val suffixes = listOf("", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No")

	fun number(value: Double): String {
		if (!value.isFinite() || value < 0.0) {
			return "0"
		}

		if (value > 0.0 && value < 1.0) {
			return String.format(Locale.US, "%.2f", value)
		}

		if (value < 1_000.0) {
			return if (value < 10.0 && value % 1.0 >= 0.05) {
				String.format(Locale.US, "%.1f", value)
			} else {
				floor(value).toLong().toString()
			}
		}

		var group = (floor(log10(value)) / 3).toInt()
		if (group >= suffixes.size) {
			return String.format(Locale.US, "%.2fe%d", value / 10.0.pow(floor(log10(value))), floor(log10(value)).toInt())
		}

		var scaled = value / 1_000.0.pow(group)
		if (scaled >= 999.95 && group < suffixes.lastIndex) {
			group++
			scaled /= 1_000.0
		}

		val format = if (scaled < 10.0) {
			"%.2f%s"
		} else {
			"%.1f%s"
		}

		return String.format(Locale.US, format, scaled, suffixes[group])
	}

	/** Time for a purchase to earn its cost back at its current incremental passive rate. */
	fun payback(cost: Double, gainPerSecond: Double): String {
		if (!cost.isFinite() || !gainPerSecond.isFinite() || cost < 0.0 || gainPerSecond <= 0.0) {
			return "unavailable"
		}

		val seconds = cost / gainPerSecond

		return when {
			!seconds.isFinite() -> "unavailable"
			seconds < 1.0 -> "<1s"
			seconds >= 31_536_000.0 -> ">1y"
			seconds >= 86_400.0 -> "${ceil(seconds / 86_400.0).toLong()}d"
			else -> duration(ceil(seconds).toLong())
		}
	}

	fun duration(seconds: Long): String {
		val minutes = seconds.coerceAtLeast(0) / 60

		return when {
			seconds < 60 -> "${seconds.coerceAtLeast(0)}s"
			minutes < 60 -> "${minutes}m"
			else -> "${minutes / 60}h ${minutes % 60}m"
		}
	}
}
