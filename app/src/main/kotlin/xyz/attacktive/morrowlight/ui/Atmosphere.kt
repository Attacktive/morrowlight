package xyz.attacktive.morrowlight.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.attacktive.morrowlight.core.Cultivator
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.Garden
import xyz.attacktive.morrowlight.core.GardenArt
import xyz.attacktive.morrowlight.core.GardenMark
import xyz.attacktive.morrowlight.core.Skywork
import xyz.attacktive.morrowlight.core.WindwardArt
import xyz.attacktive.morrowlight.core.WindwardStructure

val Night = Color(0xFF111A25)
val Panel = Color(0xFF1C2933)
val Mint = Color(0xFFAFE9CD)
val Gold = Color(0xFFF1D49B)
val Lilac = Color(0xFFC7BBF3)
val Ink = Color(0xFFEAF2EC)
val Muted = Color(0xFFA9BABF)
val Hairline = Color(0xFF344750)

@Composable
fun MorrowTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = darkColorScheme(
			primary = Mint, onPrimary = Night, primaryContainer = Color(0xFF2C5148),
			secondary = Gold, onSecondary = Night, tertiary = Lilac,
			background = Night, onBackground = Ink, surface = Panel, onSurface = Ink,
			surfaceVariant = Color(0xFF283B43), onSurfaceVariant = Muted,
			outline = Hairline, error = Color(0xFFFFB7AE)
		),
		typography = Typography(
			displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp),
			headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
			titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
			bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
			labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
		),
		content = content
	)
}

@Composable
fun GardenScene(state: GameState, animated: Boolean, modifier: Modifier = Modifier) {
	val marks = remember(state.owned, state.skyworks) { GardenArt.scene(state) }
	val paths = remember(marks) { marks.filterIsInstance<GardenMark.Shape>().associateWith { PathParser().parsePathString(it.data).toPath() } }
	val still = remember { mutableFloatStateOf(0f) }
	val phase: State<Float> = if (animated && !state.preferences.reducedMotion) {
		val transition = rememberInfiniteTransition(label = "Garden breeze")
		transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), label = "Firefly drift")
	} else {
		still
	}

	Canvas(modifier.semantics { contentDescription = "A floating garden with ${state.owned.sum()} cultivators, ${Skywork.entries.count { Garden.skyworkComplete(state, it) }} restored structures, and a glowing ember." }) {
		drawRect(Brush.verticalGradient(listOf(Color(0xFF192135), Color(0xFF242C43), Color(0xFF263D44))))

		withTransform({ scale(size.width / 480f, size.height / 380f, Offset.Zero) }) {
			for (mark in marks) {
				val color = Color(mark.color).copy(alpha = mark.alpha)
				when (mark) {
					is GardenMark.Circle -> {
						val dx = if (mark.drift == 0f) 0f else sin(phase.value + mark.drift) * 7f
						val dy = if (mark.drift == 0f) 0f else cos(phase.value * 2f + mark.drift) * 5f
						drawCircle(color, mark.radius, Offset(mark.x + dx, mark.y + dy))
					}

					is GardenMark.Oval -> drawOval(color, Offset(mark.x, mark.y), Size(mark.width, mark.height))
					is GardenMark.Shape -> if (mark.stroke > 0f) {
						drawPath(paths.getValue(mark), color, style = Stroke(mark.stroke, cap = StrokeCap.Round))
					} else {
						drawPath(paths.getValue(mark), color)
					}
				}
			}
		}
	}
}

@Composable
fun WindwardScene(state: GameState, animated: Boolean, modifier: Modifier = Modifier) {
	val bridgeActive = Garden.skybridgeActive(state)
	val marks = remember(state.windwardLevels, state.skybridgeLevel, bridgeActive) { WindwardArt.scene(state) }
	val paths = remember(marks) { marks.filterIsInstance<GardenMark.Shape>().associateWith { PathParser().parsePathString(it.data).toPath() } }
	val still = remember { mutableFloatStateOf(0f) }
	val phase: State<Float> = if (animated && !state.preferences.reducedMotion) {
		val transition = rememberInfiniteTransition(label = "Windward mist")
		transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "Moondew motes")
	} else {
		still
	}

	val built = WindwardStructure.entries.count { Garden.windwardLevel(state, it) > 0 }

	val bridgeDescription = when {
		state.skybridgeLevel == 0 -> "no Skybridge"
		bridgeActive -> "an active Skybridge"
		else -> "a dormant Skybridge"
	}

	Canvas(modifier.semantics { contentDescription = "Windward Isle, a misty floating island with $built restored structures and $bridgeDescription." }) {
		drawRect(Brush.verticalGradient(listOf(Color(0xFF182435), Color(0xFF2A3951), Color(0xFF274B52))))

		withTransform({ scale(size.width / 480f, size.height / 380f, Offset.Zero) }) {
			for (mark in marks) {
				val color = Color(mark.color).copy(alpha = mark.alpha)
				when (mark) {
					is GardenMark.Circle -> {
						val dx = if (mark.drift == 0f) 0f else sin(phase.value + mark.drift) * 7f
						val dy = if (mark.drift == 0f) 0f else cos(phase.value * 2f + mark.drift) * 5f
						drawCircle(color, mark.radius, Offset(mark.x + dx, mark.y + dy))
					}

					is GardenMark.Oval -> drawOval(color, Offset(mark.x, mark.y), Size(mark.width, mark.height))
					is GardenMark.Shape -> if (mark.stroke > 0f) {
						drawPath(paths.getValue(mark), color, style = Stroke(mark.stroke, cap = StrokeCap.Round))
					} else {
						drawPath(paths.getValue(mark), color)
					}
				}
			}
		}
	}
}

enum class Symbol { SEED, SPARK, SUN, SETTINGS, GROW, BOOK, BRIDGE }

@Composable
fun Glyph(modifier: Modifier = Modifier, symbol: Symbol, color: Color = Mint) {
	Canvas(modifier.size(24.dp)) {
		withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) {
			val stroke = Stroke(1.7f, cap = StrokeCap.Round)
			when (symbol) {
				Symbol.SEED, Symbol.GROW -> {
					val stem = Path().apply {
						moveTo(12f, 21f)
						cubicTo(13f, 13f, 8f, 9f, 6f, 7f)
					}

					drawPath(stem, color, style = stroke)

					val leftLeaf = Path().apply {
						moveTo(11f, 13f)
						cubicTo(2f, 14f, 3f, 4f, 3f, 4f)
						cubicTo(11f, 3f, 13f, 10f, 11f, 13f)
						close()
					}

					drawPath(leftLeaf, color, style = stroke)

					val rightLeaf = Path().apply {
						moveTo(12f, 16f)
						cubicTo(12f, 8f, 19f, 6f, 22f, 7f)
						cubicTo(22f, 15f, 17f, 17f, 12f, 16f)
						close()
					}

					drawPath(rightLeaf, color, style = stroke)
				}

				Symbol.SPARK -> {
					val spark = Path().apply {
						moveTo(12f, 2f)
						lineTo(15f, 9f)
						lineTo(22f, 12f)
						lineTo(15f, 15f)
						lineTo(12f, 22f)
						lineTo(9f, 15f)
						lineTo(2f, 12f)
						lineTo(9f, 9f)
						close()
					}

					drawPath(spark, color, style = stroke)
				}

				Symbol.SUN -> {
					drawArc(color, 180f, 180f, false, Offset(6f, 10f), Size(12f, 12f), style = stroke)
					drawLine(color, Offset(2f, 17f), Offset(22f, 17f), 1.7f, StrokeCap.Round)
					for (index in 0..4) {
						val angle = PI + index * PI / 4
						drawLine(color, Offset(12f + cos(angle).toFloat() * 9f, 16f + sin(angle).toFloat() * 9f), Offset(12f + cos(angle).toFloat() * 12f, 16f + sin(angle).toFloat() * 12f), 1.5f, StrokeCap.Round)
					}
				}

				Symbol.SETTINGS -> {
					drawCircle(color, 6f, Offset(12f, 12f), style = stroke)
					drawCircle(color, 2f, Offset(12f, 12f), style = stroke)
					for (index in 0..7) {
						val angle = index * PI / 4
						drawLine(color, Offset(12f + cos(angle).toFloat() * 6f, 12f + sin(angle).toFloat() * 6f), Offset(12f + cos(angle).toFloat() * 9f, 12f + sin(angle).toFloat() * 9f), 2.5f, StrokeCap.Round)
					}
				}

				Symbol.BRIDGE -> {
					val arch = Path().apply {
						moveTo(3f, 15f)
						cubicTo(7f, 8f, 17f, 8f, 21f, 15f)
					}

					drawPath(arch, color, style = stroke)
					drawLine(color, Offset(3f, 15f), Offset(21f, 15f), 1.7f, StrokeCap.Round)
					for (x in listOf(6f, 12f, 18f)) {
						drawLine(color, Offset(x, 6f), Offset(x, 19f), 1.4f, StrokeCap.Round)
					}
				}

				Symbol.BOOK -> {
					val book = Path().apply {
						moveTo(12f, 6f)
						quadraticTo(6f, 2f, 2f, 5f)
						lineTo(2f, 20f)
						quadraticTo(7f, 17f, 12f, 21f)
						quadraticTo(17f, 17f, 22f, 20f)
						lineTo(22f, 5f)
						quadraticTo(17f, 2f, 12f, 6f)
						lineTo(12f, 21f)
					}

					drawPath(book, color, style = stroke)
				}
			}
		}
	}
}

@Composable
fun PlantIcon(cultivator: Cultivator, modifier: Modifier = Modifier) {
	val accent = when (cultivator) {
		Cultivator.GLOWCAP, Cultivator.MOONWILLOW -> Mint
		Cultivator.LANTERN_LILY, Cultivator.FIREFLY_HAVEN -> Gold
		Cultivator.CLOUD_LOOM, Cultivator.STARWELL -> Lilac
	}

	Canvas(modifier.size(52.dp)) {
		withTransform({ scale(size.width / 52f, size.height / 52f, Offset.Zero) }) {
			drawCircle(accent.copy(alpha = 0.08f), 26f, Offset(26f, 26f))
			when (cultivator) {
				Cultivator.GLOWCAP -> {
					drawLine(accent, Offset(26f, 38f), Offset(26f, 23f), 3f, StrokeCap.Round)

					val cap = Path().apply {
						moveTo(12f, 26f)
						quadraticTo(26f, 1f, 40f, 26f)
						quadraticTo(26f, 32f, 12f, 26f)
						close()
					}

					drawPath(cap, accent)
					drawCircle(Night.copy(alpha = 0.3f), 2f, Offset(25f, 20f))
				}

				Cultivator.LANTERN_LILY, Cultivator.MOONWILLOW -> {
					drawLine(accent, Offset(26f, 41f), Offset(26f, 17f), 2f)
					drawOval(accent, Offset(19f, 9f), Size(14f, 20f))
					drawOval(accent.copy(alpha = 0.6f), Offset(12f, 27f), Size(14f, 7f))
					drawOval(accent.copy(alpha = 0.6f), Offset(26f, 32f), Size(14f, 7f))
				}

				Cultivator.FIREFLY_HAVEN -> {
					drawCircle(accent.copy(alpha = 0.15f), 16f, Offset(26f, 26f))
					for (point in listOf(Offset(18f, 19f), Offset(34f, 23f), Offset(24f, 35f))) {
						drawCircle(accent, 3f, point)
					}
				}

				Cultivator.CLOUD_LOOM -> {
					drawLine(accent, Offset(15f, 38f), Offset(15f, 13f), 2f)
					drawLine(accent, Offset(37f, 38f), Offset(37f, 13f), 2f)
					drawLine(accent, Offset(15f, 16f), Offset(37f, 16f), 2f)
					for (x in listOf(21f, 26f, 31f)) {
						drawLine(accent.copy(alpha = 0.7f), Offset(x, 17f), Offset(x, 32f), 1f)
					}

					drawOval(accent, Offset(11f, 29f), Size(32f, 10f))
				}

				Cultivator.STARWELL -> {
					drawOval(accent, Offset(11f, 34f), Size(30f, 9f), style = Stroke(2f))

					val star = Path().apply {
						moveTo(26f, 8f)
						lineTo(30f, 20f)
						lineTo(41f, 24f)
						lineTo(30f, 28f)
						lineTo(26f, 39f)
						lineTo(22f, 28f)
						lineTo(11f, 24f)
						lineTo(22f, 20f)
						close()
					}

					drawPath(star, accent)
				}
			}
		}
	}
}
