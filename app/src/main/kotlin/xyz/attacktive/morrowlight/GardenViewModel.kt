package xyz.attacktive.morrowlight

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import xyz.attacktive.morrowlight.core.BuyAmount
import xyz.attacktive.morrowlight.core.Constellation
import xyz.attacktive.morrowlight.core.Contribution
import xyz.attacktive.morrowlight.core.Cultivator
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.Garden
import xyz.attacktive.morrowlight.core.LoadResult
import xyz.attacktive.morrowlight.core.MoondewRouting
import xyz.attacktive.morrowlight.core.OfflineGrowth
import xyz.attacktive.morrowlight.core.PlayClock
import xyz.attacktive.morrowlight.core.Preferences
import xyz.attacktive.morrowlight.core.Skywork
import xyz.attacktive.morrowlight.core.WindwardStructure

data class GardenUiState(val garden: GameState = GameState(), val loading: Boolean = true, val loadError: String? = null, val saveError: String? = null, val recovered: Boolean = false, val offline: OfflineGrowth? = null, val active: Boolean = false)

class GardenViewModel(application: Application): AndroidViewModel(application) {
	private val repository = (application as MorrowlightApplication).repository
	private val mutableUi = MutableStateFlow(GardenUiState())
	val ui = mutableUi.asStateFlow()
	private val clock = PlayClock()
	private var visible = false
	private var ticker: Job? = null
	private var loading: Job? = null

	init {
		load()
		viewModelScope.launch { repository.error.collect { error -> mutableUi.value = mutableUi.value.copy(saveError = error) } }
	}

	fun load() {
		if (loading?.isActive == true) {
			return
		}

		mutableUi.value = mutableUi.value.copy(loading = true, loadError = null)

		loading = viewModelScope.launch {
			when (val result = repository.load()) {
				is LoadResult.Ready -> {
					mutableUi.value = mutableUi.value.copy(garden = result.state, loading = false, recovered = result.recovered)
					if (visible) {
						resume()
					}
				}

				is LoadResult.Failed -> mutableUi.value = mutableUi.value.copy(loading = false, loadError = result.message)
			}
		}
	}

	fun resume() {
		visible = true
		val current = mutableUi.value
		if (current.loading || current.loadError != null || ticker?.isActive == true) {
			return
		}

		val growth = clock.enter(current.garden, System.currentTimeMillis(), SystemClock.elapsedRealtime())
		mutableUi.value = current.copy(garden = growth.state, active = true, offline = growth.takeIf { it.seconds >= 60 && (it.earned > 0.0 || it.moondewEarned > 0.0) } ?: current.offline)
		persist()

		ticker = viewModelScope.launch {
			var lastSave = SystemClock.elapsedRealtime()

			while (isActive) {
				delay(250.milliseconds)
				advance()
				val now = SystemClock.elapsedRealtime()
				if (now - lastSave >= 5_000) {
					persist()
					lastSave = now
				}
			}
		}
	}

	fun pause() {
		visible = false
		ticker?.cancel()
		ticker = null
		if (mutableUi.value.loading || mutableUi.value.loadError != null) {
			return
		}

		val garden = clock.leave(mutableUi.value.garden, System.currentTimeMillis(), SystemClock.elapsedRealtime())
		mutableUi.value = mutableUi.value.copy(garden = garden, active = false)
		persist()
	}

	private fun advance() {
		val garden = clock.advance(mutableUi.value.garden, System.currentTimeMillis(), SystemClock.elapsedRealtime())
		mutableUi.value = mutableUi.value.copy(garden = garden)
	}

	private fun change(saveImmediately: Boolean = true, transform: (GameState) -> GameState) {
		if (mutableUi.value.loading || mutableUi.value.loadError != null || !visible) {
			return
		}

		// Settle time at the old production rate before buying, researching, or resetting.
		advance()
		mutableUi.value = mutableUi.value.copy(garden = transform(mutableUi.value.garden))
		if (saveImmediately) {
			persist()
		}
	}

	fun tap() = change(false, Garden::tap)

	fun buy(cultivator: Cultivator, amount: BuyAmount) = change { Garden.buy(it, cultivator, amount.limit) }

	fun research(id: String) = change { Garden.research(it, id) }

	fun prestige() = change { Garden.prestige(it) }

	fun attune(constellation: Constellation) = change { Garden.attune(it, constellation) }

	fun contributeSkywork(work: Skywork, contribution: Contribution) = change { Garden.contributeSkywork(it, work, contribution) }

	fun routeMoondew(routing: MoondewRouting) = change { Garden.routeMoondew(it, routing) }

	fun buildWindward(structure: WindwardStructure) = change { Garden.buildWindward(it, structure) }

	fun preferences(preferences: Preferences) = change { it.copy(preferences = preferences) }

	fun dismissOffline() {
		mutableUi.value = mutableUi.value.copy(offline = null)
	}

	fun dismissRecovery() {
		mutableUi.value = mutableUi.value.copy(recovered = false)
	}

	private fun persist() = repository.save(mutableUi.value.garden)
}
