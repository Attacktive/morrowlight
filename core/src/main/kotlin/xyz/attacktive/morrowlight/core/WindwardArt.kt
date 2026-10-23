package xyz.attacktive.morrowlight.core

import kotlin.random.Random

/** A second floating island whose permanent structures grow visibly with their levels. */
object WindwardArt {
	private const val MIST = 0xFF97E3E0
	private const val GOLD = 0xFFF3D89E
	private const val LILAC = 0xFFC4BBF4

	fun scene(state: GameState) = buildList {
		val random = Random(1583)

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
			for (ring in 6 downTo 1) {
				circle(x, y, ring * radius / 6f, color, 0.016f + (6 - ring) * 0.006f)
			}
		}

		halo(387f, 88f, 42f, LILAC)
		circle(387f, 88f, 22f, 0xFFE1E8FF, 0.85f)
		circle(397f, 79f, 20f, 0xFF202C42)
		repeat(48) {
			circle(15f + random.nextFloat() * 449f, 18f + random.nextFloat() * 196f, 0.5f + random.nextFloat() * 1.5f, 0xFFCDDDE8, 0.22f + random.nextFloat() * 0.45f)
		}

		path("M0 263 Q77 227 138 251 Q228 184 318 245 T480 228 L480 380 L0 380Z", 0xFF263449, 0.69f)
		path("M0 293 Q68 249 151 272 T324 275 T480 254 L480 380 L0 380Z", 0xFF273B49, 0.75f)
		oval(79f, 297f, 350f, 43f, 0xFF0B1924, 0.45f)
		halo(244f, 249f, 145f, MIST)
		path("M67 239 Q126 220 200 244 L277 229 L394 231 L421 249 L393 272 L360 298 L331 305 L284 353 L248 365 L217 342 L172 326 L148 296 L106 287Z", 0xFF243C4A)
		path("M67 239 L136 260 L171 327 L148 296 L106 287Z", 0xFF365765)
		path("M137 258 L237 273 L248 365 L217 342 L171 327Z", 0xFF263B4D)
		path("M237 273 L321 260 L284 353 L248 365Z", 0xFF1B3042)
		path("M321 260 L421 249 L393 272 L360 298 L331 305 L284 353Z", 0xFF365262)
		path("M145 288 L161 313 M260 295 L248 343 M340 275 L312 327", 0xFF6896A1, 0.43f, 2f)
		oval(65f, 216f, 356f, 65f, 0xFF426A6B)
		oval(78f, 212f, 330f, 58f, 0xFF72A292)
		oval(103f, 215f, 280f, 42f, 0xFF9CB7A3, 0.35f)

		path("M123 255 C117 287 125 307 116 339", MIST, 0.43f, 7f)
		path("M137 262 C128 286 138 313 133 343", 0xFFB2F5E1, 0.6f, 4f)

		repeat(15) { index ->
			circle(112f + random.nextFloat() * 35f, 273f + index * 5f, 1f + index % 3, MIST, 0.28f)
		}

		repeat(16) { index ->
			val x = 109f + (index * 23 % 270)
			val y = 230f + (index * 17 % 26)
			path("M$x $y l-3 -8 m3 8 l4 -6", 0xFFB3E4B8, 0.58f, 1.4f)
		}

		val mist = Garden.windwardLevel(state, WindwardStructure.MIST_CONDENSER)
		val chimes = Garden.windwardLevel(state, WindwardStructure.WIND_CHIMES)
		val beacon = Garden.windwardLevel(state, WindwardStructure.STAR_BEACON)

		if (mist > 0) {
			halo(143f, 187f, 39f, MIST)
			path("M112 241 L121 221 L162 221 L174 241Z", 0xFF526E76)
			oval(112f, 218f, 61f, 18f, 0xFFB3DDD4)
			oval(122f, 220f, 40f, 9f, MIST)
			path("M127 218 L137 183 L147 215 L157 196 L161 219Z", 0xFF8ADBD7, 0.84f)
			circle(138f, 197f, 3f, 0xFFFFFFFF)
			repeat(mist.coerceAtMost(6)) { index ->
				val x = 108f + index * 13f
				val y = 175f - index % 3 * 12f
				halo(x, y, 12f, MIST)
				circle(x, y, 2.2f, MIST, 0.9f, (index + 2).toFloat())
			}
		} else {
			path("M114 243 L125 231 L157 231 L168 243Z", 0xFF547B80, 0.55f)
		}

		if (chimes > 0) {
			path("M329 226 L329 179 M331 184 Q363 167 394 189 M369 184 L369 226", 0xFFACCFB6, 0.9f, 3f)
			repeat(chimes.coerceAtMost(7)) { index ->
				val x = 333f + index * 9f
				val y = 183f + (index % 3) * 5f
				path("M$x $y l0 16", 0xFFC1E2CA, 0.9f, 1.1f)
				halo(x, y + 18f, 13f, GOLD)
				path("M${x - 3f} ${y + 16f} l6 0 l-3 10Z", GOLD, 0.92f)
			}
		} else {
			path("M343 240 L353 221 L370 241Z", 0xFF667F80, 0.5f)
		}

		if (beacon > 0) {
			path("M227 223 L234 134 L248 134 L256 223Z", 0xFF7AA5A5)
			path("M232 159 L251 159 M231 183 L252 183 M230 205 L254 205", 0xFFBED4C6, 0.7f, 2.1f)
			halo(241f, 128f, 55f + beacon * 2f, LILAC)
			path("M242 112 L248 123 L261 128 L248 133 L242 145 L237 133 L224 128 L237 123Z", LILAC)
			circle(242f, 128f, 5f, 0xFFFFFFFF)
			if (beacon >= 5) {
				path("M241 126 L124 28 M241 126 L359 30", 0xFFDEDAFF, 0.25f, 2.5f)
			}
		} else {
			path("M231 222 L235 204 L246 204 L251 222Z", 0xFF697C87, 0.7f)
		}

		for ((x, y) in listOf(55f to 88f, 89f to 57f, 125f to 91f, 91f to 137f)) {
			halo(x, y, 11f, GOLD)
			circle(x, y, 2.5f, GOLD)
		}

		path("M55 88 L89 57 L125 91 L91 137 L55 88", GOLD, 0.23f, 1.1f)
	}
}
