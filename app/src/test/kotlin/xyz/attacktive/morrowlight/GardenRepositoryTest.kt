package xyz.attacktive.morrowlight

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.LoadResult
import xyz.attacktive.morrowlight.core.SaveCodec
import xyz.attacktive.morrowlight.core.SaveStore

@OptIn(ExperimentalCoroutinesApi::class)
class GardenRepositoryTest {
	@TempDir
	lateinit var directory: File

	@Test
	fun reloadPreservesAPrestigeQueuedBeforeTheWriterRuns() = runTest {
		val dispatcher = StandardTestDispatcher(testScheduler)
		val repository = GardenRepository(directory, dispatcher)
		load(repository, dispatcher)
		repository.save(GameState(glow = 100.0, runGlow = 4_000_000.0, lifetimeGlow = 4_000_000.0, owned = listOf(1, 0, 0, 0, 0, 0)))
		load(repository, dispatcher)
		repository.save(GameState(lifetimeGlow = 4_000_000.0, dawnSeeds = 2, dawns = 1))

		val reloaded = load(repository, dispatcher)
		assertEquals(2L, reloaded.dawnSeeds)
		assertEquals(1, reloaded.dawns)
		assertEquals(0.0, reloaded.glow)
		assertEquals(0, reloaded.owned.sum())

		// Match the recreated ViewModel persisting its loaded state on resume.
		repository.save(reloaded)
		load(repository, dispatcher)
		val persisted = (SaveStore(directory).load() as LoadResult.Ready).state
		assertEquals(2L, persisted.dawnSeeds)
		assertEquals(1, persisted.dawns)
	}

	@Test
	fun aSaveBurstRetainsThePreviousCheckpointForRecovery() = runTest {
		val dispatcher = StandardTestDispatcher(testScheduler)
		val repository = GardenRepository(directory, dispatcher)
		load(repository, dispatcher)
		repository.save(GameState(glow = 10.0, runGlow = 10.0, lifetimeGlow = 10.0))
		load(repository, dispatcher)
		for (glow in listOf(20.0, 30.0, 40.0)) {
			repository.save(GameState(glow = glow, runGlow = glow, lifetimeGlow = glow))
		}

		assertEquals(40.0, load(repository, dispatcher).glow)
		val latest = directory.listFiles()!!.filter { it.extension == "save" }.maxBy { SaveCodec.decode(it.readBytes()).revision }
		latest.writeBytes(byteArrayOf(0))
		val recovered = SaveStore(directory).load() as LoadResult.Ready
		assertTrue(recovered.recovered)
		assertEquals(10.0, recovered.state.glow)
	}

	@Test
	fun aLoadObservesEarlierSavesButNotLaterOnes() = runTest {
		val dispatcher = StandardTestDispatcher(testScheduler)
		val repository = GardenRepository(directory, dispatcher)
		load(repository, dispatcher)
		repository.save(GameState(glow = 10.0, runGlow = 10.0, lifetimeGlow = 10.0))
		repository.save(GameState(glow = 20.0, runGlow = 20.0, lifetimeGlow = 20.0))
		val loading = async(dispatcher, start = CoroutineStart.UNDISPATCHED) { repository.load() }
		repository.save(GameState(glow = 30.0, runGlow = 30.0, lifetimeGlow = 30.0))
		repository.save(GameState(glow = 40.0, runGlow = 40.0, lifetimeGlow = 40.0))
		runCurrent()
		assertEquals(20.0, (loading.await() as LoadResult.Ready).state.glow)
		assertEquals(40.0, load(repository, dispatcher).glow)
	}

	@Test
	fun anInvalidSaveKeepsTheCheckpointAndDoesNotStopLaterOperations() = runTest {
		val dispatcher = StandardTestDispatcher(testScheduler)
		val repository = GardenRepository(directory, dispatcher)
		load(repository, dispatcher)
		repository.save(GameState(glow = 20.0, runGlow = 20.0, lifetimeGlow = 20.0))
		load(repository, dispatcher)
		repository.save(GameState(glow = -1.0))
		assertEquals(20.0, load(repository, dispatcher).glow)
		assertNotNull(repository.error.value)
		repository.save(GameState(glow = 30.0, runGlow = 30.0, lifetimeGlow = 30.0))
		assertEquals(30.0, load(repository, dispatcher).glow)
		assertNull(repository.error.value)
	}

	@Test
	fun cancelingALoadDoesNotStopTheWorkerOrLoseQueuedSaves() = runTest {
		val dispatcher = StandardTestDispatcher(testScheduler)
		val repository = GardenRepository(directory, dispatcher)
		load(repository, dispatcher)
		repository.save(GameState(glow = 20.0, runGlow = 20.0, lifetimeGlow = 20.0))
		val loading = async(dispatcher, start = CoroutineStart.UNDISPATCHED) { repository.load() }
		loading.cancel()
		repository.save(GameState(glow = 30.0, runGlow = 30.0, lifetimeGlow = 30.0))
		assertEquals(30.0, load(repository, dispatcher).glow)
	}

	private suspend fun TestScope.load(repository: GardenRepository, dispatcher: CoroutineDispatcher): GameState {
		val result = async(dispatcher, start = CoroutineStart.UNDISPATCHED) { repository.load() }
		runCurrent()
		return (result.await() as LoadResult.Ready).state
	}
}
