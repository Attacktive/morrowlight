package xyz.attacktive.morrowlight

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.app.Application
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.LoadResult
import xyz.attacktive.morrowlight.core.SaveStore

class MorrowlightApplication: Application() {
	val repository by lazy { GardenRepository(File(filesDir, "garden")) }
}

/** One application-scoped worker orders checkpoint reads and writes across Activity recreation. */
class GardenRepository(directory: File, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
	private sealed interface Operation {
		data class Save(var state: GameState): Operation
		data class Load(val result: CompletableDeferred<LoadResult>): Operation
	}

	private val store = SaveStore(directory)
	private val pending = ArrayDeque<Operation>()
	private val wakeup = Channel<Unit>(Channel.CONFLATED)
	private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
	private val mutableError = MutableStateFlow<String?>(null)
	val error = mutableError.asStateFlow()

	init {
		scope.launch {
			while (true) {
				wakeup.receive()
				while (true) {
					val operation = synchronized(pending) { pending.removeFirstOrNull() } ?: break
					when (operation) {
						is Operation.Save -> saveCheckpoint(operation.state)
						is Operation.Load -> {
							try {
								operation.result.complete(store.load())
							} catch (error: Exception) {
								operation.result.completeExceptionally(error)
							}
						}
					}
				}
			}
		}
	}

	suspend fun load(): LoadResult {
		val result = CompletableDeferred<LoadResult>()
		synchronized(pending) {
			pending.addLast(Operation.Load(result))
		}

		wakeup.trySend(Unit).getOrThrow()
		return result.await()
	}

	fun save(state: GameState) {
		synchronized(pending) {
			val last = pending.lastOrNull()
			if (last is Operation.Save) {
				last.state = state
			} else {
				pending.addLast(Operation.Save(state))
			}
		}

		wakeup.trySend(Unit).getOrThrow()
	}

	private fun saveCheckpoint(state: GameState) {
		try {
			store.save(state)
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
