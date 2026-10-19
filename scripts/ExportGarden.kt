import java.io.File
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.GardenArt
import xyz.attacktive.morrowlight.core.GardenMark

/** Export the exact in-game geometry for artwork review, not an Android screenshot. */
fun main(args: Array<String>) {
	val output = File(args.firstOrNull() ?: "garden.svg")
	val state = if (args.getOrNull(1) == "empty") {
		GameState()
	} else {
		GameState(owned = listOf(35, 18, 12, 6, 3, 1))
	}

	output.writeText(buildString {
		append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 480 380\">")
		append("<defs><linearGradient id=\"sky\" x2=\"0\" y2=\"1\"><stop stop-color=\"#192135\"/><stop offset=\".5\" stop-color=\"#242c43\"/><stop offset=\"1\" stop-color=\"#263d44\"/></linearGradient></defs><path fill=\"url(#sky)\" d=\"M0 0H480V380H0Z\"/>")

		for (mark in GardenArt.scene(state)) {
			append(mark.toSvg())
		}

		append("</svg>")
	})
}

private fun GardenMark.toSvg(): String {
	val color = "#" + (color and 0xffffff).toString(16).padStart(6, '0')
	val fill = "fill='$color' opacity='$alpha'"

	return when (this) {
		is GardenMark.Circle -> "<circle cx='$x' cy='$y' r='$radius' $fill/>"
		is GardenMark.Oval -> "<ellipse cx='${x + width / 2}' cy='${y + height / 2}' rx='${width / 2}' ry='${height / 2}' $fill/>"
		is GardenMark.Shape -> {
			val paint = if (stroke > 0f) {
				"fill='none' stroke='$color' stroke-width='$stroke' stroke-linecap='round' opacity='$alpha'"
			} else {
				fill
			}

			"<path d='$data' $paint/>"
		}
	}
}
