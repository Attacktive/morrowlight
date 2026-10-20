package xyz.attacktive.morrowlight.core

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed interface LoadResult {
	data class Ready(val state: GameState, val recovered: Boolean = false): LoadResult
	data class Failed(val message: String): LoadResult
}

/** Alternating atomic save slots retain the last good checkpoint during interrupted writes. */
class SaveStore(private val directory: File) {
	private var revision = 0L
	private var loaded = false

	@Synchronized
	fun load(): LoadResult {
		loaded = false
		val candidates = mutableListOf<SavedGarden>()
		var damaged = false
		try {
			for (name in listOf("garden-a.save", "garden-b.save")) {
				val file = File(directory, name)
				if (!file.exists()) {
					continue
				}

				try {
					require(file.length() <= SaveCodec.MAX_BYTES) { "Save is too large." }
					candidates += SaveCodec.decode(file.readBytes())
				} catch (error: UnsupportedSaveVersion) {
					return LoadResult.Failed(error.message ?: "This save needs a newer app.")
				} catch (_: IOException) {
					damaged = true
				} catch (_: IllegalArgumentException) {
					damaged = true
				}
			}
		} catch (_: SecurityException) {
			return LoadResult.Failed("The garden save could not be read. Your files were not changed.")
		}

		val best = candidates.maxByOrNull { it.revision }
		if (best == null && damaged) {
			return LoadResult.Failed("Both save checkpoints are unreadable. Your files were not changed.")
		}

		revision = best?.revision ?: 0L
		loaded = true
		return LoadResult.Ready(best?.state ?: GameState(), damaged)
	}

	@Synchronized
	fun save(state: GameState) {
		check(loaded) { "Load a garden before saving it." }
		check(revision < Long.MAX_VALUE - 1) { "Save revision limit reached." }
		if (!directory.isDirectory && !directory.mkdirs()) {
			throw IOException("Cannot create the save directory.")
		}

		val next = revision + 1
		val destination = File(directory, if (next % 2L == 0L) "garden-a.save" else "garden-b.save")
		val temporary = File(directory, "garden-writing.tmp")
		val bytes = SaveCodec.encode(SavedGarden(next, state))
		try {
			FileOutputStream(temporary).use { stream ->
				stream.write(bytes)
				stream.fd.sync()
			}
			try {
				Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
			} catch (_: AtomicMoveNotSupportedException) {
				Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
			}
			revision = next
		} finally {
			temporary.delete()
		}
	}
}
