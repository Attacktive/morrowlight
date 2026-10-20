package xyz.attacktive.morrowlight.core

/** Foreground elapsed time uses a monotonic clock; only offline growth uses wall time. */
class PlayClock {
	private var anchor: Long? = null

	fun enter(state: GameState, wallMillis: Long, monotonicMillis: Long): OfflineGrowth {
		if (anchor != null) {
			return OfflineGrowth(state, 0.0, 0, false)
		}

		anchor = monotonicMillis

		return Garden.offline(state, wallMillis)
	}

	fun advance(state: GameState, wallMillis: Long, monotonicMillis: Long): GameState {
		val previous = anchor ?: return state
		val elapsed = if (monotonicMillis > previous) monotonicMillis - previous else 0L
		anchor = monotonicMillis

		return Garden.tick(state, elapsed / 1_000.0).copy(lastSeenMillis = maxOf(state.lastSeenMillis, wallMillis))
	}

	fun leave(state: GameState, wallMillis: Long, monotonicMillis: Long): GameState {
		val updated = advance(state, wallMillis, monotonicMillis)
		anchor = null

		return updated
	}
}
