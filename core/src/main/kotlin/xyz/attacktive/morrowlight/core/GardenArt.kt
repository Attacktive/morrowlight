package xyz.attacktive.morrowlight.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Shared vector scene in a 480 x 380 view box. Plants follow ownership; structures follow permanent restoration. */
sealed interface GardenMark {
	val color: Long
	val alpha: Float

	data class Circle(val x: Float, val y: Float, val radius: Float, override val color: Long, override val alpha: Float = 1f, val drift: Float = 0f): GardenMark
	data class Oval(val x: Float, val y: Float, val width: Float, val height: Float, override val color: Long, override val alpha: Float = 1f): GardenMark
	data class Shape(val data: String, override val color: Long, override val alpha: Float = 1f, val stroke: Float = 0f): GardenMark
}

object GardenArt {
	private const val MINT = 0xFFA4EAD2
	private const val GOLD = 0xFFF6D899
	private const val LILAC = 0xFFC2B9FF

	fun scene(state: GameState) = buildList {
		val random = Random(740)
		fun circle(x: Float, y: Float, radius: Float, color: Long, alpha: Float = 1f, drift: Float = 0f) {
			add(GardenMark.Circle(x, y, radius, color, alpha, drift))
		}

		fun oval(x: Float, y: Float, width: Float, height: Float, color: Long, alpha: Float = 1f) {
			add(GardenMark.Oval(x, y, width, height, color, alpha))
		}

		fun path(data: String, color: Long, alpha: Float = 1f, stroke: Float = 0f) {
			add(GardenMark.Shape(data, color, alpha, stroke))
		}

		fun halo(x: Float, y: Float, radius: Float, color: Long) {
			for (step in 7 downTo 1) {
				circle(x, y, radius * step / 7f, color, 0.018f + (7 - step) * 0.005f)
			}
		}

		// A quiet crescent: a single silhouette, without the concentric artificial halo.
		path("M373 51 C355 58 351 80 363 95 C374 107 387 107 401 99 C392 115 368 121 351 108 C335 96 333 78 346 61 C354 51 363 49 373 51Z", 0xFFE5E8FF, 0.94f)
		repeat(40) {
			circle(18f + random.nextFloat() * 444f, 25f + random.nextFloat() * 213f, 0.7f + random.nextFloat() * 1.1f, 0xFFD4E9EE, 0.18f + random.nextFloat() * 0.4f)
		}

		path("M0 238 Q60 172 139 222 T310 208 T480 205 L480 380 L0 380Z", 0xFF252743, 0.48f)
		path("M0 265 Q90 214 187 252 T355 238 T480 264 L480 380 L0 380Z", 0xFF242B42, 0.7f)
		oval(86f, 329f, 325f, 34f, 0xFF080F20, 0.36f)
		halo(239f, 264f, 145f, 0xFF4C9A95)
		path("M75 252 L111 295 L147 302 L172 346 L214 328 L239 357 L272 334 L310 339 L335 305 L373 296 L407 254Z", 0xFF253B46)
		path("M75 252 L164 271 L172 346 L147 302 L111 295Z", 0xFF304C51)
		path("M164 271 L248 280 L239 357 L214 328 L172 346Z", 0xFF24333F)
		path("M248 280 L338 269 L310 339 L272 334 L239 357Z", 0xFF1C2C3D)
		path("M338 269 L407 254 L373 296 L335 305 L310 339Z", 0xFF334653)
		path("M159 287 L174 320 M257 300 L252 327 M324 289 L314 314", 0xFF47707A, 0.48f, 1.5f)
		oval(73f, 216f, 335f, 73f, 0xFF466B65)
		oval(79f, 213f, 323f, 65f, 0xFF638876)
		oval(93f, 216f, 296f, 50f, 0xFF779881, 0.42f)
		path("M109 260 Q134 281 137 309 M352 263 Q342 292 337 300 M176 278 Q184 291 177 306", 0xFF7BBA94, 0.55f, 2.2f)

		repeat(26) {
			val x = 96 + random.nextInt(287)
			val y = 232 + random.nextInt(30)

			path("M$x $y l-3 -7 m3 7 l4 -5", 0xFFA4CFAD, 0.36f, 1.2f)
		}

		if (Garden.skyworkComplete(state, Skywork.LANTERN_BRIDGE)) {
			// The far side of the garden lights up as the bridge is restored.
			path("M382 245 Q428 224 480 236", 0xFF4A6570, 1f, 6f)
			path("M384 240 Q429 219 481 231", GOLD, 0.9f, 2f)
			path("M395 241 L395 229 M421 231 L421 219 M449 229 L449 218 M475 233 L475 220", 0xFF9DBBA5, 0.92f, 1.9f)
			for ((x, y) in listOf(395f to 227f, 421f to 217f, 449f to 216f, 475f to 218f)) {
				halo(x, y, 17f, GOLD)
				circle(x, y, 3.5f, GOLD)
			}
		}

		if (Garden.skyworkComplete(state, Skywork.WINDMILLS)) {
			// A slender windmill rises above the lit bridge.
			path("M442 231 L444 188 L452 188 L454 232Z", 0xFF729B91)
			path("M440 189 L446 183 L454 189Z", 0xFFBFDBCA)
			circle(448f, 188f, 4f, GOLD)
			path("M448 188 L435 172 M448 188 L462 175 M448 188 L461 201 M448 188 L433 201", 0xFFDBD3AF, 0.92f, 2.4f)
		}

		if (Garden.skyworkComplete(state, Skywork.MOON_TERRACE)) {
			// The farthest landing glows, ready for the next island update.
			path("M455 236 L455 222 L472 222 L472 238Z", 0xFF6C8E8B)
			path("M454 220 L474 220 M459 217 L459 209 M469 217 L469 209", 0xFFEAD9B5, 0.95f, 2f)
			halo(464f, 207f, 24f, LILAC)
			circle(464f, 207f, 4f, LILAC)
		}

		if (state.count(Cultivator.MOONWILLOW) > 0) {
			val willowCount = state.count(Cultivator.MOONWILLOW)
			val darkBark = 0xFF609B88
			val lightBark = 0xFFACDEBE

			// The willow twists toward the light. The trunk is a sculpted silhouette instead of a bundle of straight sticks.
			path("M240 248 C231 229 252 211 262 195 C277 174 259 157 244 143 C223 127 232 111 248 94 C237 113 240 126 254 137 C278 153 288 173 274 198 C267 220 255 232 264 248Z", darkBark)
			path("M251 245 C245 223 268 211 271 190 C277 170 263 155 248 142 C241 134 239 126 240 119 C230 137 254 149 257 171 C260 194 236 220 242 244Z", 0xFF8FC6AA, 0.88f)
			path("M247 237 C250 211 281 188 264 165 C255 153 238 141 237 128", lightBark, 0.9f, 3f)
			path("M253 184 C230 160 207 147 186 144 C169 141 157 155 140 160", darkBark, 1f, 8f)
			path("M252 177 C224 156 207 145 186 145 C173 147 155 157 140 160", lightBark, 0.94f, 3.5f)
			path("M235 144 C218 121 200 105 177 116 C167 120 160 125 154 120", darkBark, 1f, 6f)
			path("M238 144 C219 123 200 105 180 116", lightBark, 0.87f, 2.5f)
			path("M243 139 C233 114 237 98 253 83", darkBark, 1f, 6f)
			path("M244 139 C236 116 238 99 254 84", lightBark, 0.9f, 2.5f)
			path("M267 168 C282 142 300 131 323 134 C339 137 346 145 350 148", darkBark, 1f, 8f)
			path("M269 165 C287 141 301 132 323 134 C336 136 345 143 350 148", lightBark, 0.94f, 3.5f)
			path("M265 147 C274 118 286 104 307 101 C321 99 329 106 334 109", darkBark, 1f, 5.5f)
			path("M267 144 C278 116 290 104 307 101", lightBark, 0.9f, 2f)
			path("M281 160 C310 155 330 163 346 181", darkBark, 1f, 5f)
			path("M212 160 C199 176 184 185 170 185", darkBark, 1f, 4f)

			fun leaf(x: Float, y: Float, length: Float, angle: Double, tone: Long, alpha: Float = 0.92f) {
				val dx = cos(angle).toFloat() * length
				val dy = sin(angle).toFloat() * length
				val nx = -sin(angle).toFloat() * length * 0.3f
				val ny = cos(angle).toFloat() * length * 0.3f
				val tipX = x + dx
				val tipY = y + dy

				path("M$x $y C${x + dx * 0.3f + nx} ${y + dy * 0.3f + ny} ${x + dx * 0.75f + nx} ${y + dy * 0.75f + ny} $tipX $tipY C${x + dx * 0.85f - nx} ${y + dy * 0.85f - ny} ${x + dx * 0.3f - nx} ${y + dy * 0.3f - ny} $x $y Z", tone, alpha)
			}

			// Deliberately placed leaf crowns keep the silhouette airy instead of making a tangled canopy.
			val crowns = listOf(
				Pair(143f, 157f), Pair(167f, 122f), Pair(193f, 113f),
				Pair(221f, 94f), Pair(252f, 81f), Pair(293f, 100f),
				Pair(323f, 128f), Pair(344f, 159f), Pair(172f, 177f),
				Pair(209f, 152f), Pair(288f, 146f)
			)

			for ((crownIndex, crown) in crowns.withIndex()) {
				for (leafIndex in 0 until 5 + (willowCount / 4).coerceAtMost(3)) {
					val angle = (2.0 * PI * leafIndex / 6.0) + crownIndex * 0.42
					val x = crown.first + cos(angle).toFloat() * (5f + leafIndex % 3 * 3f)
					val y = crown.second + sin(angle).toFloat() * (4f + leafIndex % 2 * 4f)
					val tone = when ((leafIndex + crownIndex) % 4) {
						0 -> 0xFF7BBDA8
						1 -> 0xFFA6DDC0
						2 -> 0xFF91D2B7
						else -> 0xFFB4E6CA
					}

					leaf(x, y, 17f + leafIndex % 3 * 2f, angle - 0.23, tone)
				}
			}

			val lanterns = listOf(
				Triple(151f, 157f, 193f),
				Triple(176f, 125f, 172f),
				Triple(204f, 145f, 193f),
				Triple(245f, 90f, 139f),
				Triple(288f, 119f, 171f),
				Triple(318f, 139f, 191f),
				Triple(338f, 160f, 211f)
			)

			for ((index, lantern) in lanterns.withIndex()) {
				val x = lantern.first
				val y = lantern.second
				val bottom = lantern.third

				path("M$x $y Q${x + if (index % 2 == 0) -6 else 6} ${(y + bottom) / 2} $x $bottom", 0xFF86BDA6, 0.72f, 1.3f)
				leaf(x - 2f, (y + bottom) / 2f - 5f, 11f, PI / 2 - 0.5, 0xFF91D5B7, 0.8f)
				leaf(x + 2f, (y + bottom) / 2f + 5f, 10f, PI / 2 + 0.5, 0xFF75B79D, 0.7f)
				halo(x, bottom, 17f, GOLD)
				circle(x, bottom, 5.5f, 0xFFFFE7A8, 0.94f)
				circle(x, bottom - 0.5f, 2.6f, 0xFFFFF9DD)
			}
		}

		if (state.count(Cultivator.STARWELL) > 0) {
			halo(327f, 193f, 60f, GOLD)
			path("M299 245 L304 229 L346 229 L354 245Z", 0xFFADB5B7)
			oval(299f, 233f, 55f, 18f, 0xFFC7C7BC)
			oval(306f, 235f, 40f, 10f, 0xFF7DDDCF)
			path("M308 234 L308 191 Q326 157 345 191 L345 234", 0xFFDBC594, 1f, 5f)
			path("M327 174 L332 191 L349 197 L332 202 L327 220 L321 202 L305 197 L321 191Z", GOLD)
			path("M327 144 L327 155 M292 171 L300 176 M352 168 L359 162", GOLD, 0.8f, 2f)
		}

		if (state.count(Cultivator.CLOUD_LOOM) > 0) {
			path("M32 190 L53 217 L73 201 L91 190Z", 0xFF45566F)
			oval(27f, 178f, 66f, 23f, 0xFF9395B6)
			path("M47 184 L47 157 L78 157 L78 184 M52 156 L52 179 M60 156 L60 183 M69 156 L69 181", LILAC, 0.9f, 2.2f)
			halo(64f, 176f, 36f, LILAC)
			oval(47f, 182f, 40f, 11f, 0xFFE6D8EE, 0.76f)

			if (state.count(Cultivator.CLOUD_LOOM) >= 5) {
				path("M399 162 L423 192 L452 169Z", 0xFF45566F)
				oval(392f, 153f, 68f, 21f, 0xFF9395B6)
				path("M412 158 L412 136 L440 136 L440 158", LILAC, 0.9f, 2f)
			}
		}

		if (state.count(Cultivator.FIREFLY_HAVEN) > 0) {
			path("M125 246 L125 215 Q140 192 156 215 L156 246Z", 0xFF405F5D)
			path("M124 216 Q140 190 157 216", 0xFFB7C9A0, 1f, 3f)
			oval(132f, 216f, 16f, 24f, GOLD, 0.48f)
			halo(140f, 226f, 32f, GOLD)
		}

		val glowcapCount = state.count(Cultivator.GLOWCAP).coerceAtMost(18)

		repeat(glowcapCount) { index ->
			val x = 121 + (index * 47) % 219
			val y = 246 + (index * 13) % 25
			val height = 16 + index % 3 * 5

			path("M$x $y q-2 -8 0 -$height", 0xFFABD8BB, 1f, 3.2f)
			halo(x.toFloat(), (y - height).toFloat(), 22f, MINT)
			path("M${x - 13} ${y - height} Q$x ${y - height - 22} ${x + 14} ${y - height} Q$x ${y - height + 5} ${x - 13} ${y - height}Z", 0xFF8CE1C5)
			circle((x - 3).toFloat(), (y - height - 5).toFloat(), 2f, 0xFFEEFFE7, 0.9f)
		}

		repeat(state.count(Cultivator.LANTERN_LILY).coerceAtMost(12)) { index ->
			val x = 154 + (index * 53) % 204
			val y = 239 + (index * 17) % 26
			val height = 30 + index % 4 * 6

			path("M$x $y q-8 -16 0 -$height", 0xFF9EBE94, 1f, 2.3f)
			path("M$x ${y - 9} q-17 -1 -14 -12 q12 0 14 12 M$x ${y - 15} q15 -4 14 -13 q-13 2 -14 13", 0xFF9FBD8A)
			halo(x.toFloat(), (y - height).toFloat(), 28f, GOLD)
			oval(x - 6f, (y - height - 8).toFloat(), 12f, 18f, GOLD)
			circle(x.toFloat(), (y - height - 2).toFloat(), 3f, 0xFFFFF5DB)
		}

		// The original ember stays present through every generation.
		halo(233f, 249f, 39f, GOLD)
		path("M225 261 Q214 247 227 243 Q235 248 234 260 M235 261 Q249 242 254 251 Q251 263 235 261", 0xFFBADEA6)
		path("M229 250 Q222 239 234 226 Q232 237 240 241 Q244 250 235 254Z", GOLD)
		circle(233f, 246f, 3.5f, 0xFFFFF7D7)

		val fireflies = (3 + state.count(Cultivator.FIREFLY_HAVEN) * 2).coerceAtMost(28)

		repeat(fireflies) { index ->
			val x = 98f + random.nextFloat() * 281f
			val y = 131f + random.nextFloat() * 128f

			circle(x, y, 7f, GOLD, 0.055f, index + 1f)
			circle(x, y, 2f, GOLD, 0.88f, index + 1f)
		}
	}
}
