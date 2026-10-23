package xyz.attacktive.morrowlight.core

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.random.Random

/** Shared by the JUnit adapter and the no-download JVM smoke runner. */
object Scenarios {
	private fun close(expected: Double, actual: Double) {
		check(abs(expected - actual) <= maxOf(1e-8, abs(expected) * 1e-10)) { "Expected $expected, got $actual" }
	}

	private fun funded(amount: Double = 10_000.0) = Garden.earn(GameState(), amount)
	private fun producer(count: Int = 1) = GameState(owned = listOf(count, 0, 0, 0, 0, 0))
	private inline fun rejected(block: () -> Unit) {
		var failed = false

		try {
			block()
		} catch (_: Exception) {
			failed = true
		}

		check(failed) { "Invalid operation unexpectedly succeeded" }
	}

	val all: List<Pair<String, () -> Unit>> = listOf(
		"A new garden has no passive income" to { close(0.0, Garden.production(GameState())) },
		"The first touch creates a Glow" to { close(1.0, Garden.tap(GameState()).glow) },
		"Fifteen touches buy the first Glowcap" to {
			var state = GameState()
			repeat(15) { state = Garden.tap(state) }
			state = Garden.buy(state, Cultivator.GLOWCAP, 1)
			check(state.count(Cultivator.GLOWCAP) == 1)
			close(0.0, state.glow)
			close(0.25, Garden.production(state))
		},
		"Unaffordable purchases are no-ops" to { check(Garden.buy(GameState(), Cultivator.GLOWCAP, 1) == GameState()) },
		"Invalid purchase quantities are no-ops" to { check(Garden.buy(funded(), Cultivator.GLOWCAP, -1) == funded()) },
		"Locked cultivators cannot be purchased" to {
			val state = funded(500.0)
			check(Garden.buy(state, Cultivator.FIREFLY_HAVEN, 1) == state)
		},
		"Discovery uses earned Glow, not remaining balance" to {
			val state = funded(120.0).copy(glow = 0.0)
			check(Garden.discovered(state, Cultivator.LANTERN_LILY))
		},
		"Bulk quotes and single purchases agree" to {
			val state = funded()
			val bulk = Garden.buy(state, Cultivator.GLOWCAP, 10)
			var single = state
			repeat(10) { single = Garden.buy(single, Cultivator.GLOWCAP, 1) }
			check(single.owned == bulk.owned)
			close(single.glow, bulk.glow)
		},
		"Bulk purchase uses the affordable portion" to {
			val state = funded(33.0)
			check(Garden.quote(state, Cultivator.GLOWCAP, 10).quantity == 2)
			close(0.75, Garden.buy(state, Cultivator.GLOWCAP, 10).glow)
		},
		"Max purchase leaves less than the next price" to {
			val state = Garden.buy(funded(), Cultivator.GLOWCAP, MAX_OWNED)
			check(state.glow >= 0.0 && state.glow < Garden.nextCost(state, Cultivator.GLOWCAP))
		},
		"Ownership stops at its finite limit" to {
			val state = Garden.buy(funded(MAX_GLOW), Cultivator.GLOWCAP, Int.MAX_VALUE)
			check(state.count(Cultivator.GLOWCAP) == MAX_OWNED)
			check(Garden.quote(state, Cultivator.GLOWCAP, 1).quantity == 0)
		},
		"Supporting cultivators boost other colonies at their thresholds" to {
			val before = GameState(owned = listOf(9, 10, 0, 0, 0, 0))
			val after = before.copy(owned = listOf(10, 10, 0, 0, 0, 0))
			close(1.0, Garden.synergyMultiplier(before, Cultivator.LANTERN_LILY))
			close(1.2, Garden.synergyMultiplier(after, Cultivator.LANTERN_LILY))
			close(Garden.production(before, Cultivator.LANTERN_LILY) * 1.2, Garden.production(after, Cultivator.LANTERN_LILY))
		},
		"Synergies have a bounded ceiling" to {
			val abundant = GameState(owned = listOf(1_000, 1, 1, 1_000, 1_000, 1))
			close(2.0, Garden.synergyMultiplier(abundant, Cultivator.LANTERN_LILY))
			close(1.75, Garden.synergyMultiplier(abundant, Cultivator.FIREFLY_HAVEN))
			close(2.0, Garden.synergyMultiplier(abundant, Cultivator.STARWELL))
		},
		"Investment previews include indirect synergy gains" to {
			val before = GameState(owned = listOf(9, 10, 0, 0, 0, 0))
			val after = before.copy(owned = listOf(10, 10, 0, 0, 0, 0))
			close(Garden.production(after) - Garden.production(before), Garden.productionGain(before, Cultivator.GLOWCAP, 1))
			check(Garden.productionGain(before, Cultivator.GLOWCAP, 1) > Garden.production(after, Cultivator.GLOWCAP) - Garden.production(before, Cultivator.GLOWCAP))
			close(0.0, Garden.productionGain(before, Cultivator.GLOWCAP, 0))
		},
		"Colony milestones double production" to {
			close(1.0, Garden.colonyMultiplier(9))
			close(2.0, Garden.colonyMultiplier(10))
			close(4.0, Garden.colonyMultiplier(25))
			close(8.0, Garden.colonyMultiplier(50))
			close(16.0, Garden.colonyMultiplier(100))
		},
		"Tick frequency does not change earnings" to {
			var state = producer()
			repeat(100) { state = Garden.tick(state, 0.1) }
			close(Garden.tick(producer(), 10.0).glow, state.glow)
		},
		"Negative and non-finite elapsed time is rejected" to {
			for (delta in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
				check(Garden.tick(producer(), delta) == producer())
			}
		},
		"The 25th touch releases one firefly gift" to {
			val before = producer().copy(charge = 24)
			val after = Garden.tap(before)
			close(Garden.tapValue(before) + 15.0 * Garden.production(before), after.glow)
			check(after.charge == 0)
		},
		"Touches scale with production" to { check(Garden.tapValue(producer(100)) > Garden.tapValue(producer())) },
		"Research purchases are charged once" to {
			val bought = Garden.research(funded(100.0), "gentle-hands")
			close(50.0, bought.glow)
			check(Garden.research(bought, "gentle-hands") == bought)
			close(2.0, Garden.tapValue(bought))
		},
		"Research prerequisites are enforced" to { check(Garden.research(funded(), "ember-keeper") == funded()) },
		"Research counts are enforced" to { check(Garden.research(funded(), "glowcap-1") == funded()) },
		"Cultivator research doubles output" to {
			val state = funded().copy(owned = producer(10).owned)
			close(2.0 * Garden.production(state), Garden.production(Garden.research(state, "glowcap-1")))
		},
		"Unknown research IDs do nothing" to { check(Garden.research(funded(), "alien-potato") == funded()) },
		"Offline earnings use actual elapsed time" to {
			val result = Garden.offline(producer().copy(lastSeenMillis = 1_000L), 61_000L)
			close(15.0, result.earned)
			check(result.seconds == 60L && !result.capped)
		},
		"Offline cap discards excess time" to {
			val result = Garden.offline(producer().copy(lastSeenMillis = 1_000L), 100_000_000L)
			close(7_200.0, result.earned)
			check(result.capped && result.seconds == 28_800L)
			close(0.0, Garden.offline(result.state, 100_000_000L).earned)
		},
		"Offline upgrades extend the cap" to {
			check(Garden.offlineLimitHours(GameState()) == 8)
			check(Garden.offlineLimitHours(GameState(research = setOf("long-nights"))) == 12)
			check(Garden.offlineLimitHours(GameState(research = setOf("dream-garden"))) == 24)
		},
		"New installs do not earn from the Unix epoch" to { close(0.0, Garden.offline(producer(), 1_000_000L).earned) },
		"Clock rollback cannot replay earnings" to {
			val initial = producer().copy(lastSeenMillis = 100_000L)
			val rewound = Garden.offline(initial, 20_000L)
			check(rewound.state.lastSeenMillis == 100_000L)
			close(0.0, Garden.offline(rewound.state, 100_000L).earned)
		},
		"Duplicate resume does not double credit" to {
			val clock = PlayClock()
			val entered = clock.enter(producer().copy(lastSeenMillis = 1_000), 61_000, 500)
			close(15.0, entered.earned)
			close(0.0, clock.enter(entered.state, 61_000, 500).earned)
		},
		"Foreground production ignores wall clock jumps" to {
			val clock = PlayClock()
			val entered = clock.enter(producer(), 1_000, 500)
			close(0.25, clock.advance(entered.state, 100_000_000, 1_500).glow)
		},
		"Pause settles the last fraction of a second" to {
			val clock = PlayClock()
			val state = clock.enter(producer(), 1_000, 1_000).state
			val paused = clock.leave(state, 1_200, 1_200)
			close(0.05, paused.glow)
			check(clock.advance(paused, 10_000, 10_000) == paused)
		},
		"Background and foreground time are disjoint" to {
			val clock = PlayClock()
			val start = clock.enter(producer(), 1_000, 1_000).state
			val paused = clock.leave(start, 2_000, 2_000)
			val resumed = clock.enter(paused, 12_000, 12_000)
			close(2.75, resumed.state.glow)
			close(2.5, resumed.earned)
		},
		"Prestige unlocks exactly at one million" to {
			check(Garden.prestigeReward(funded(999_999.0)) == 0L)
			check(Garden.prestigeReward(funded(1_000_000.0)) == 1L)
			check(Garden.prestigeReward(funded(4_000_000.0)) == 2L)
		},
		"Spending Glow does not reduce prestige rewards" to {
			val state = funded(4_000_000.0)
			check(Garden.prestigeReward(state.copy(glow = 0.0)) == 2L)
		},
		"Prestige preserves lifetime progress and preferences" to {
			val old = funded(4_000_000.0).copy(owned = producer(100).owned, research = setOf("gentle-hands"), preferences = Preferences(reducedMotion = true, haptics = false), taps = 250)
			val fresh = Garden.prestige(old)
			check(fresh.owned.sum() == 0 && fresh.research.isEmpty() && fresh.glow == 0.0 && fresh.runGlow == 0.0)
			check(fresh.dawnSeeds == 2L && fresh.dawns == 1 && fresh.taps == 250L)
			check(fresh.preferences == old.preferences && fresh.lifetimeGlow == old.lifetimeGlow)
			check("dawn" in fresh.milestones)
			close(1.3, Garden.seedMultiplier(fresh))
		},
		"Constellations unlock after the first dawn and lock until the next" to {
			check(Garden.attune(GameState(), Constellation.UNDERSTORY) == GameState())
			val afterDawn = Garden.prestige(funded(1_000_000.0))
			val attuned = Garden.attune(afterDawn, Constellation.UNDERSTORY)
			check(attuned.constellation == Constellation.UNDERSTORY)
			check(Garden.attune(attuned, Constellation.SKY_WEAVE) == attuned)
			check("constellation" in attuned.milestones)
			val nextDawn = Garden.prestige(Garden.earn(attuned, 1_000_000.0))
			check(nextDawn.constellation == null && "constellation" in nextDawn.milestones)
		},
		"Constellation production trade-offs affect only the named colonies" to {
			val before = GameState(owned = listOf(10, 10, 10, 10, 10, 10), dawns = 1)
			val under = Garden.attune(before, Constellation.UNDERSTORY)
			close(Garden.production(before, Cultivator.GLOWCAP) * 2.0, Garden.production(under, Cultivator.GLOWCAP))
			close(Garden.production(before, Cultivator.STARWELL) * 0.8, Garden.production(under, Cultivator.STARWELL))
			close(Garden.production(before, Cultivator.MOONWILLOW), Garden.production(under, Cultivator.MOONWILLOW))
			val moon = Garden.attune(before, Constellation.MOON_TIDE)
			close(Garden.production(before, Cultivator.FIREFLY_HAVEN) * 2.0, Garden.production(moon, Cultivator.FIREFLY_HAVEN))
			close(Garden.production(before, Cultivator.GLOWCAP) * 0.7, Garden.production(moon, Cultivator.GLOWCAP))
			val sky = Garden.attune(before, Constellation.SKY_WEAVE)
			close(Garden.production(before, Cultivator.CLOUD_LOOM) * 2.0, Garden.production(sky, Cultivator.CLOUD_LOOM))
			close(Garden.production(before, Cultivator.LANTERN_LILY) * 0.7, Garden.production(sky, Cultivator.LANTERN_LILY))
		},
		"Firefly chorus trades passive production for stronger wishes" to {
			val before = GameState(owned = producer(10).owned, dawns = 1, charge = 24)
			val chorus = Garden.attune(before, Constellation.FIREFLY_CHORUS)
			close(Garden.production(before) * 0.85, Garden.production(chorus))
			check(Garden.fireflySeconds(chorus) == 45)
			close(Garden.tapValue(chorus) + Garden.production(chorus) * 45.0, Garden.tap(chorus).glow)
			check(Garden.fireflySeconds(before) == 15)
		},
		"Prestige cannot be claimed twice without growing" to {
			val fresh = Garden.prestige(funded(1_000_000.0))
			check(Garden.prestige(fresh) == fresh)
		},
		"Achievements persist across prestige" to {
			val garden = Garden.buy(funded(1_000_000.0), Cultivator.MOONWILLOW, 1)
			check("moon" in Garden.prestige(garden).milestones)
		},
		"Huge progress saturates without infinity" to {
			val state = Garden.earn(funded(MAX_GLOW), Double.MAX_VALUE)
			close(MAX_GLOW, state.glow)
			check(Garden.prestigeReward(state) == MAX_SEEDS)
			check(Garden.prestigeReward(state.copy(dawnSeeds = MAX_SEEDS)) == 0L)
		},
		"Save codec round trips every field" to {
			val state = funded(1e12).copy(owned = listOf(10, 4, 2, 1, 0, 0), research = setOf("gentle-hands", "morning-dew"), dawnSeeds = 123, dawns = 4, constellation = Constellation.SKY_WEAVE, taps = 88, charge = 3, lastSeenMillis = 42, preferences = Preferences(reducedMotion = true, haptics = false))
			val save = SavedGarden(91, state)
			check(SaveCodec.decode(SaveCodec.encode(save)) == save)
		},
		"Corrupt and truncated saves are rejected" to {
			val bytes = SaveCodec.encode(SavedGarden(1, GameState()))
			for (length in listOf(0, 16, bytes.size - 1)) {
				rejected { SaveCodec.decode(bytes.copyOf(length)) }
			}

			bytes[20] = (bytes[20].toInt() xor 1).toByte()
			rejected { SaveCodec.decode(bytes) }
		},
		"Invalid numeric state cannot be saved" to {
			for (number in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
				rejected { SaveCodec.encode(SavedGarden(1, GameState(glow = number))) }
			}
		},
		"Version 1 saves migrate without resetting the garden" to {
			val saved = SavedGarden(9, funded(12_345.0).copy(dawns = 2, dawnSeeds = 3))
			val encoded = SaveCodec.encode(saved)
			val payload = encoded.copyOfRange(0, encoded.size - 34)
			payload[7] = 1
			val legacy = payload + MessageDigest.getInstance("SHA-256").digest(payload)
			check(SaveCodec.decode(legacy) == saved)
		},
		"Unrestored gardens cannot save constellation selections" to {
			rejected { SaveCodec.encode(SavedGarden(1, GameState(constellation = Constellation.UNDERSTORY))) }
		},
		"Unknown save schemas are rejected, not reset" to {
			val bytes = SaveCodec.encode(SavedGarden(1, GameState()))
			val payload = bytes.copyOfRange(0, bytes.size - 32)
			payload[7] = 3
			try {
				SaveCodec.decode(payload + MessageDigest.getInstance("SHA-256").digest(payload))
				error("Expected version rejection")
			} catch (_: UnsupportedSaveVersion) {}
		},
		"Save store selects its latest checkpoint" to {
			withStore { directory ->
				val store = SaveStore(directory)
				check(store.load() is LoadResult.Ready)
				store.save(funded(10.0))
				store.save(funded(20.0))
				close(20.0, (SaveStore(directory).load() as LoadResult.Ready).state.glow)
			}
		},
		"A damaged latest checkpoint recovers the previous one" to {
			withStore { directory ->
				val store = SaveStore(directory)
				store.load()
				store.save(funded(10.0))
				store.save(funded(20.0))

				File(directory, "garden-a.save").writeText("broken")
				val recovered = SaveStore(directory).load() as LoadResult.Ready
				check(recovered.recovered)
				close(10.0, recovered.state.glow)
			}
		},
		"Unreadable checkpoints are never silently overwritten" to {
			withStore { directory ->
				File(directory, "garden-a.save").writeText("broken")
				val store = SaveStore(directory)
				check(store.load() is LoadResult.Failed)
				rejected { store.save(GameState()) }
				check(File(directory, "garden-a.save").readText() == "broken")
			}
		},
		"Interrupted temporary writes do not replace a good save" to {
			withStore { directory ->
				val store = SaveStore(directory)
				store.load()
				store.save(funded(42.0))

				File(directory, "garden-writing.tmp").writeText("interrupted")
				close(42.0, (SaveStore(directory).load() as LoadResult.Ready).state.glow)
			}
		},
		"Formatting covers small and astronomical numbers" to {
			check(Display.number(0.25) == "0.25")
			check(Display.number(1_000.0) == "1.00K")
			check(Display.number(999_999.0) == "1.00M")
			check(Display.number(1e60) == "1.00e60")
			check(Display.duration(3_661) == "1h 1m")
		},
		"Moon is a crescent without a circular halo" to {
			val artwork = GardenArt.scene(GameState())
			check(artwork.filterIsInstance<GardenMark.Shape>()
				.any { "M373 51" in it.data }
			)

			check(artwork.filterIsInstance<GardenMark.Circle>()
				.none {
					it.x in 340f..410f && it.y in 35f..130f && it.radius > 8f
				}
			)
		},
		"The Moonwillow appears only after it is purchased" to {
			val empty = GardenArt.scene(GameState())
			val planted = GardenArt.scene(GameState(owned = listOf(0, 0, 0, 1, 0, 0)))
			check(planted.size > empty.size + 80)
		},
		"A growing Moonwillow adds leaves rather than replacing the tree" to {
			val young = GardenArt.scene(GameState(owned = listOf(0, 0, 0, 1, 0, 0)))
			val mature = GardenArt.scene(GameState(owned = listOf(0, 0, 0, 25, 0, 0)))
			check(mature.size > young.size)
			check(mature.containsAll(young.filterIsInstance<GardenMark.Shape>()
				.filter { it.color == 0xFF609B88 })
			)
		},
		"Garden illustration is deterministic across recompositions" to {
			val state = GameState(owned = listOf(18, 12, 6, 3, 2, 1))
			check(GardenArt.scene(state) == GardenArt.scene(state))
		},
		"Ten thousand random actions preserve save invariants" to {
			val random = Random(17)
			var state = GameState()

			repeat(10_000) {
				state = when (random.nextInt(7)) {
					0 -> Garden.tap(state)
					1 -> Garden.tick(state, random.nextDouble(0.0, 1_000.0))
					2 -> Garden.buy(state, Cultivator.entries[random.nextInt(6)], listOf(1, 10, 1_000)[random.nextInt(3)])
					3 -> Garden.research(state, Almanac.research[random.nextInt(Almanac.research.size)].id)
					4 -> Garden.prestige(state)
					5 -> Garden.attune(state, Constellation.entries[random.nextInt(Constellation.entries.size)])
					else -> SaveCodec.decode(SaveCodec.encode(SavedGarden(1, state))).state
				}

				SaveCodec.validate(SavedGarden(1, state))
			}
		}
	)

	private fun withStore(block: (File) -> Unit) {
		val directory = Files.createTempDirectory("morrowlight-test-").toFile()
		try {
			block(directory)
		} finally {
			directory.deleteRecursively()
		}
	}
}

fun main() {
	var failures = 0

	for ((name, test) in Scenarios.all) {
		try {
			test()
			println("PASS $name")
		} catch (error: Throwable) {
			failures++
			System.err.println("FAIL $name: ${error.message}")
			error.printStackTrace()
		}
	}

	check(failures == 0) { "$failures / ${Scenarios.all.size} scenarios failed" }
	println("${Scenarios.all.size} / ${Scenarios.all.size} scenarios passed")
}
