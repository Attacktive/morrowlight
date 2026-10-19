package xyz.attacktive.morrowlight

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import android.app.Application
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.SaveStore

class MorrowlightApplication: Application() {
	val repository by lazy { GardenRepository(File(filesDir, "garden")) }
}

/** One application-scoped writer serializes disk writes even across Activity recreation. */
class GardenRepository(directory: File) {
	private val store = SaveStore(directory)
	private val lock = Mutex()
	private val pending = Channel<GameState>(Channel.CONFLATED)
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val mutableError = MutableStateFlow<String?>(null)
	val error = mutableError.asStateFlow()

	init {
		scope.launch {
			for (state in pending) {
				try {
					lock.withLock { store.save(state) }
					mutableError.value = null
				} catch (error: CancellationException) {
					throw error
				} catch (_: java.io.IOException) {
					mutableError.value = "The garden could not be saved. Free some storage; saving will retry automatically."
				} catch (_: IllegalArgumentException) {
					mutableError.value = "This progress could not be validated. Your previous checkpoint has been kept."
				} catch (_: IllegalStateException) {
					mutableError.value = "Saving is unavailable. Your existing checkpoints have not been replaced."
				} catch (_: SecurityException) {
					mutableError.value = "Morrowlight cannot access its save folder. Progress is currently only in memory."
				}
			}
		}
	}

	suspend fun load() = withContext(Dispatchers.IO) { lock.withLock { store.load() } }

	fun save(state: GameState) {
		pending.trySend(state)
	}
}
