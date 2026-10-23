package xyz.attacktive.morrowlight.core

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

const val MAX_GLOW = 1e240
const val MAX_OWNED = 1_000
const val MAX_SEEDS = 1_000_000_000L
const val PRESTIGE_THRESHOLD = 1_000_000.0
const val FIREFLY_CHARGE = 25

data class Preferences(val reducedMotion: Boolean = false, val haptics: Boolean = true)

data class GameState(
	val glow: Double = 0.0,
	val runGlow: Double = 0.0,
	val lifetimeGlow: Double = 0.0,
	val owned: List<Int> = List(Cultivator.entries.size) { 0 },
	val research: Set<String> = emptySet(),
	val milestones: Set<String> = emptySet(),
	val dawnSeeds: Long = 0L,
	val dawns: Int = 0,
	val constellation: Constellation? = null,
	val skyworks: List<Double> = List(Skywork.entries.size) { 0.0 },
	val moondew: Double = 0.0,
	val routing: MoondewRouting = MoondewRouting.NONE,
	val windwardLevels: List<Int> = List(WindwardStructure.entries.size) { 0 },
	val taps: Long = 0L,
	val charge: Int = 0,
	val lastSeenMillis: Long = 0L,
	val preferences: Preferences = Preferences()
) {
	fun count(cultivator: Cultivator) = owned[cultivator.ordinal]
}

enum class Cultivator(val title: String, val description: String, val baseCost: Double, val baseRate: Double, val discovery: Double) {
	GLOWCAP("Glowcap", "A small mushroom with a very bright idea.", 15.0, 0.25, 0.0),
	LANTERN_LILY("Lantern lily", "Bottles the last gold of the evening.", 120.0, 2.0, 80.0),
	FIREFLY_HAVEN("Firefly haven", "Tiny tenants. Excellent work ethic.", 900.0, 12.0, 600.0),
	MOONWILLOW("Moonwillow", "Its silver leaves remember the moon.", 7_500.0, 80.0, 4_500.0),
	CLOUD_LOOM("Cloud loom", "Weaves passing clouds into ribbons of light.", 65_000.0, 480.0, 40_000.0),
	STARWELL("Starwell", "An invitation for the stars to come home.", 550_000.0, 3_000.0, 300_000.0)
}

enum class Constellation(val title: String, val description: String) {
	UNDERSTORY("Understory", "Glowcaps and Lantern lilies produce 2x, but Cloud looms and Starwells produce 20% less."),
	MOON_TIDE("Moon tide", "Firefly havens and Moonwillows produce 2x, but Glowcaps produce 30% less."),
	SKY_WEAVE("Sky weave", "Cloud looms and Starwells produce 2x, but Lantern lilies produce 30% less."),
	FIREFLY_CHORUS("Firefly chorus", "Firefly Wishes give 45 seconds of production instead of 15, but all cultivators produce 15% less.")
}

/** Ordered, one-time restorations. The enum order is part of the v3 save schema. */
enum class Skywork(val title: String, val description: String, val cost: Double, val reward: String) {
	LANTERN_BRIDGE("Light the bridge", "Carry light beyond the garden's edge.", 5_000_000.0, "A permanent lantern bridge appears in the garden."),
	WINDMILLS("Restore the windmills", "Wake the wind that turns the distant mills.", 30_000_000.0, "All cultivators permanently produce 15% more Glow."),
	MOON_TERRACE("Awaken the terrace", "Prepare a quiet landing for the neighboring island.", 180_000_000.0, "Base touch Glow permanently increases by 20%, and Windward Isle opens for exploration.")
}

enum class Contribution(val fraction: Double, val title: String) {
	TENTH(0.1, "10%"),
	HALF(0.5, "50%"),
	ALL(1.0, "All")
}

/** Fraction of passive Glow production sent to Windward. Order and names are stable save values. */
enum class MoondewRouting(val fraction: Double, val title: String) {
	NONE(0.0, "0%"),
	QUARTER(0.25, "25%"),
	HALF(0.5, "50%"),
	THREE_QUARTERS(0.75, "75%")
}

/** Each upgrade is permanent across dawns and paid only with Moondew. */
enum class WindwardStructure(val title: String, val description: String, val baseCost: Double, val costGrowth: Double, val maxLevel: Int) {
	MIST_CONDENSER("Mist condenser", "Condense more Moondew from every Glow you route.", 20_000.0, 2.1, 12),
	WIND_CHIMES("Wind chimes", "Return some diverted Glow to your home garden.", 60_000.0, 2.15, 10),
	STAR_BEACON("Star beacon", "Guide both islands with a permanent cultivator production bonus.", 180_000.0, 2.25, 10)
}

data class IslandRateGain(val glowPerSecond: Double, val moondewPerSecond: Double)

data class Research(val id: String, val title: String, val description: String, val cost: Double, val cultivator: Cultivator? = null, val requiredCount: Int = 0, val prerequisite: String? = null)

object Almanac {
	val research: List<Research> = run {
		val baseline = listOf(
			Research("gentle-hands", "Gentle hands", "Double the base Glow from each touch.", 50.0),
			Research("ember-keeper", "Ember keeper", "Quadruple base touch Glow again.", 1_250.0, prerequisite = "gentle-hands"),
			Research("morning-dew", "Morning dew", "All cultivators produce 30% more Glow.", 800.0),
			Research("star-charts", "Star charts", "All cultivators produce 50% more Glow.", 150_000.0, prerequisite = "morning-dew"),
			Research("long-nights", "Long nights", "Your garden grows offline for up to 12 hours, instead of 8.", 10_000.0),
			Research("dream-garden", "Dream garden", "Extend offline growth to a full 24 hours.", 250_000.0, prerequisite = "long-nights")
		)

		val perCultivator = Cultivator.entries.flatMap { cultivator ->
			listOf(10 to 40.0, 25 to 250.0, 50 to 2_000.0).mapIndexed { index, (count, factor) ->
				Research(
					id = "${cultivator.name.lowercase()}-${index + 1}",
					title = "${cultivator.title}: ${listOf("Awakening", "Harmony", "Radiance")[index]}",
					description = "Double this cultivator's production.",
					cost = cultivator.baseCost * factor,
					cultivator = cultivator,
					requiredCount = count,
					prerequisite = if (index == 0) null else "${cultivator.name.lowercase()}-$index"
				)
			}
		}

		baseline + perCultivator
	}

	val researchById = research.associateBy { it.id }

	val milestones = listOf(
		Milestone("first-light", "First light", "Gather your first Glow.") { it.lifetimeGlow >= 1.0 },
		Milestone("roots", "Putting down roots", "Plant a Glowcap.") { it.count(Cultivator.GLOWCAP) > 0 || it.dawns > 0 },
		Milestone("little-forest", "A little forest", "Own 25 cultivators in one garden.") { it.owned.sum() >= 25 },
		Milestone("thousand", "A thousand little suns", "Gather 1,000 lifetime Glow.") { it.lifetimeGlow >= 1_000.0 },
		Milestone("touch", "A gentle habit", "Touch the garden 250 times.") { it.taps >= 250 },
		Milestone("moon", "A place for the moon", "Grow your first Moonwillow.") { it.count(Cultivator.MOONWILLOW) > 0 },
		Milestone("stars", "The stars came home", "Build a Starwell.") { it.count(Cultivator.STARWELL) > 0 },
		Milestone("dawn", "Begin again, brighter", "Restore your first dawn.") { it.dawns >= 1 },
		Milestone("ten-seeds", "Keeper of mornings", "Collect 10 permanent Dawn Seeds.") { it.dawnSeeds >= 10 },
		Milestone("network", "We grow together", "Grow ten Glowcaps alongside a Lantern lily after your first dawn.") { it.dawns > 0 && it.count(Cultivator.GLOWCAP) >= 10 && it.count(Cultivator.LANTERN_LILY) > 0 },
		Milestone("constellation", "Written in starlight", "Follow your first constellation.") { it.constellation != null },
		Milestone("skyworks-bridge", "A path through the night", "Complete the lantern bridge.") { Garden.skyworkComplete(it, Skywork.LANTERN_BRIDGE) },
		Milestone("skyworks-windmills", "The wind remembers", "Restore the windmills.") { Garden.skyworkComplete(it, Skywork.WINDMILLS) },
		Milestone("skyworks-terrace", "The next horizon", "Awaken the moon terrace.") { Garden.skyworkComplete(it, Skywork.MOON_TERRACE) },
		Milestone("windward-first-build", "A second garden takes root", "Restore your first Windward structure.") { it.windwardLevels.any { level -> level > 0 } },
		Milestone("windward-threefold", "Both islands, one sky", "Build all three Windward structures.") { it.windwardLevels.all { level -> level > 0 } },
		Milestone("billion", "A sky of your own", "Gather one billion lifetime Glow.") { it.lifetimeGlow >= 1e9 }
	)
}

data class Milestone(val id: String, val title: String, val description: String, val earned: (GameState) -> Boolean)

data class Quote(val quantity: Int, val cost: Double)

data class OfflineGrowth(val state: GameState, val earned: Double, val seconds: Long, val capped: Boolean, val moondewEarned: Double = 0.0)

enum class BuyAmount(val limit: Int, val label: String) {
	ONE(1, "1"), TEN(10, "10"), MAX(MAX_OWNED, "Max")
}

/** Pure, deterministic rules. Android clocks, rendering, and storage live outside this object. */
object Garden {
	fun seedMultiplier(state: GameState) = 1.0 + state.dawnSeeds * 0.15

	fun skyworkProgress(state: GameState, work: Skywork) = state.skyworks[work.ordinal]

	fun skyworkComplete(state: GameState, work: Skywork) = skyworkProgress(state, work) >= work.cost

	fun skyworkAvailable(state: GameState, work: Skywork) = state.dawns > 0 &&
		!skyworkComplete(state, work) &&
		(work.ordinal == 0 || skyworkComplete(state, Skywork.entries[work.ordinal - 1]))

	/** Quotes a fraction of currently held Glow, capped by the current project's remaining cost. */
	fun skyworkQuote(state: GameState, work: Skywork, contribution: Contribution): Double {
		if (!skyworkAvailable(state, work)) {
			return 0.0
		}

		val available = if (contribution == Contribution.ALL) state.glow else floor(state.glow * contribution.fraction)
		val remaining = (work.cost - skyworkProgress(state, work)).coerceAtLeast(0.0)

		return minOf(available, remaining, state.glow).coerceAtLeast(0.0)
	}

	/** Contributions are permanent, never counted as new earnings, and never refunded by prestige. */
	fun contributeSkywork(state: GameState, work: Skywork, contribution: Contribution): GameState {
		val amount = skyworkQuote(state, work, contribution)
		if (amount <= 0.0) {
			return state
		}

		val progress = state.skyworks.toMutableList()
		progress[work.ordinal] = (progress[work.ordinal] + amount).coerceAtMost(work.cost)

		return rememberMilestones(state.copy(glow = (state.glow - amount).coerceAtLeast(0.0), skyworks = progress.toList()))
	}

	fun skyworkProductionMultiplier(state: GameState) = if (skyworkComplete(state, Skywork.WINDMILLS)) 1.15 else 1.0

	fun skyworkTouchMultiplier(state: GameState) = if (skyworkComplete(state, Skywork.MOON_TERRACE)) 1.2 else 1.0

	fun windwardUnlocked(state: GameState) = skyworkComplete(state, Skywork.MOON_TERRACE)

	fun windwardLevel(state: GameState, structure: WindwardStructure) = state.windwardLevels[structure.ordinal]

	/** An exchange rate, not an independent generator. Routing 250 Glow yields 1 Moondew before upgrades. */
	fun moondewConversion(state: GameState) = (1.0 + windwardLevel(state, WindwardStructure.MIST_CONDENSER) * 0.35) / 250.0

	/** Recover at most 40% of the Glow routed away from the garden. */
	fun routingRecovery(state: GameState) = windwardLevel(state, WindwardStructure.WIND_CHIMES) * 0.04

	/** Star beacons boost gross production, which then feeds both routing and retained Glow. */
	fun windwardProductionMultiplier(state: GameState) = 1.0 + windwardLevel(state, WindwardStructure.STAR_BEACON) * 0.08

	/** Gross passive Glow before any routing, retained for cultivator readouts and active Firefly Wishes. */
	fun glowRate(state: GameState) = bounded(production(state) * (1.0 - state.routing.fraction * (1.0 - routingRecovery(state))))

	/** Moondew accumulates only from routed passive production, including capped offline growth. */
	fun moondewRate(state: GameState) = bounded(production(state) * state.routing.fraction * moondewConversion(state))

	fun routeMoondew(state: GameState, routing: MoondewRouting): GameState {
		if (!windwardUnlocked(state) || state.routing == routing) {
			return state
		}

		return state.copy(routing = routing)
	}

	fun windwardNextCost(state: GameState, structure: WindwardStructure): Double? {
		val level = windwardLevel(state, structure)
		if (level >= structure.maxLevel) {
			return null
		}

		return structure.baseCost * structure.costGrowth.pow(level)
	}

	fun canBuildWindward(state: GameState, structure: WindwardStructure): Boolean {
		val price = windwardNextCost(state, structure) ?: return false

		return windwardUnlocked(state) && state.moondew >= price
	}

	/** Preview the impact of one new level at the current routing without spending Moondew. */
	fun windwardGain(state: GameState, structure: WindwardStructure): IslandRateGain {
		if (!windwardUnlocked(state) || windwardNextCost(state, structure) == null) {
			return IslandRateGain(0.0, 0.0)
		}

		val levels = state.windwardLevels.toMutableList()
		levels[structure.ordinal]++
		val after = state.copy(windwardLevels = levels)

		return IslandRateGain(
			(glowRate(after) - glowRate(state)).coerceAtLeast(0.0),
			(moondewRate(after) - moondewRate(state)).coerceAtLeast(0.0)
		)
	}

	fun buildWindward(state: GameState, structure: WindwardStructure): GameState {
		if (!canBuildWindward(state, structure)) {
			return state
		}

		val cost = windwardNextCost(state, structure) ?: return state
		val levels = state.windwardLevels.toMutableList()
		levels[structure.ordinal]++

		return rememberMilestones(state.copy(moondew = (state.moondew - cost).coerceAtLeast(0.0), windwardLevels = levels.toList()))
	}

	/** Extra production earned by growing supporting cultivators, including when the supported colony is purchased first. */
	fun synergyMultiplier(state: GameState, cultivator: Cultivator) = when {
		state.dawns == 0 -> 1.0
		cultivator == Cultivator.LANTERN_LILY -> 1.0 + (state.count(Cultivator.GLOWCAP) / 10).coerceAtMost(5) * 0.2
		cultivator == Cultivator.FIREFLY_HAVEN -> 1.0 + (state.count(Cultivator.MOONWILLOW) / 5).coerceAtMost(5) * 0.15
		cultivator == Cultivator.STARWELL -> 1.0 + (state.count(Cultivator.CLOUD_LOOM) / 5).coerceAtMost(10) * 0.1
		else -> 1.0
	}

	fun constellationMultiplier(state: GameState, cultivator: Cultivator) = when (state.constellation) {
		null -> 1.0
		Constellation.UNDERSTORY -> when (cultivator) {
			Cultivator.GLOWCAP, Cultivator.LANTERN_LILY -> 2.0
			Cultivator.CLOUD_LOOM, Cultivator.STARWELL -> 0.8
			else -> 1.0
		}
		Constellation.MOON_TIDE -> when (cultivator) {
			Cultivator.FIREFLY_HAVEN, Cultivator.MOONWILLOW -> 2.0
			Cultivator.GLOWCAP -> 0.7
			else -> 1.0
		}
		Constellation.SKY_WEAVE -> when (cultivator) {
			Cultivator.CLOUD_LOOM, Cultivator.STARWELL -> 2.0
			Cultivator.LANTERN_LILY -> 0.7
			else -> 1.0
		}
		Constellation.FIREFLY_CHORUS -> 0.85
	}

	fun fireflySeconds(state: GameState) = if (state.constellation == Constellation.FIREFLY_CHORUS) 45 else 15

	fun colonyMultiplier(count: Int) = 2.0.pow(listOf(10, 25, 50, 100).count { count >= it })

	fun nextColonyMilestone(count: Int) = listOf(10, 25, 50, 100).firstOrNull { it > count }

	fun production(state: GameState, cultivator: Cultivator): Double {
		val researchCount = Almanac.research.count {
			it.cultivator == cultivator && it.id in state.research
		}

		val researchMultiplier = 2.0.pow(researchCount)

		val dew = if ("morning-dew" in state.research) 1.3 else 1.0
		val charts = if ("star-charts" in state.research) 1.5 else 1.0

		return bounded(state.count(cultivator) * cultivator.baseRate * colonyMultiplier(state.count(cultivator)) * researchMultiplier * seedMultiplier(state) * dew * charts * synergyMultiplier(state, cultivator) * constellationMultiplier(state, cultivator) * skyworkProductionMultiplier(state) * windwardProductionMultiplier(state))
	}

	fun production(state: GameState) = bounded(Cultivator.entries.sumOf { production(state, it) })

	/** Includes the indirect production gained by reaching a supporting colony's synergy threshold. */
	fun productionGain(state: GameState, cultivator: Cultivator, quantity: Int): Double {
		val count = state.count(cultivator)
		if (quantity <= 0 || count >= MAX_OWNED) {
			return 0.0
		}

		val counts = state.owned.toMutableList()
		counts[cultivator.ordinal] += quantity.coerceAtMost(MAX_OWNED - count)

		return (glowRate(state.copy(owned = counts)) - glowRate(state)).coerceAtLeast(0.0)
	}

	fun tapValue(state: GameState): Double {
		val hands = if ("gentle-hands" in state.research) 2.0 else 1.0
		val keeper = if ("ember-keeper" in state.research) 4.0 else 1.0

		return bounded(hands * keeper * seedMultiplier(state) * skyworkTouchMultiplier(state) + production(state) * 0.04)
	}

	fun tap(state: GameState): GameState {
		val charged = state.charge + 1 >= FIREFLY_CHARGE
		val gift = if (charged) production(state) * fireflySeconds(state) else 0.0

		return rememberMilestones(
			earn(state, tapValue(state) + gift)
				.copy(
					taps = (state.taps + 1).coerceAtMost(Long.MAX_VALUE - 1),
					charge = if (charged) 0 else state.charge + 1
				)
		)
	}

	fun tick(state: GameState, seconds: Double): GameState {
		if (!seconds.isFinite() || seconds <= 0.0) {
			return state
		}

		val elapsed = seconds.coerceAtMost(86_400.0)
		val retained = bounded(glowRate(state) * elapsed)
		val condensed = bounded(moondewRate(state) * elapsed)
		val advanced = earn(state, retained)

		return rememberMilestones(advanced.copy(moondew = bounded(advanced.moondew + condensed)))
	}

	fun discovered(state: GameState, cultivator: Cultivator) = state.runGlow >= cultivator.discovery || state.count(cultivator) > 0

	fun nextCost(state: GameState, cultivator: Cultivator) = cultivator.baseCost * 1.15.pow(state.count(cultivator))

	/** Quotes at most [amount] affordable units; the exact same quote is used by purchase. */
	fun quote(state: GameState, cultivator: Cultivator, amount: Int): Quote {
		if (!discovered(state, cultivator) || amount <= 0) {
			return Quote(0, 0.0)
		}

		val limit = amount.coerceAtMost(MAX_OWNED - state.count(cultivator))
		var count = 0
		var total = 0.0

		while (count < limit) {
			val price = cultivator.baseCost * 1.15.pow(state.count(cultivator) + count)
			val nextTotal = total + price
			if (!nextTotal.isFinite() || nextTotal > state.glow) {
				break
			}

			total = nextTotal
			count++
		}

		return Quote(count, total)
	}

	fun buy(state: GameState, cultivator: Cultivator, amount: Int): GameState {
		val quote = quote(state, cultivator, amount)
		if (quote.quantity == 0) {
			return state
		}

		val counts = state.owned.toMutableList()
		counts[cultivator.ordinal] += quote.quantity

		return rememberMilestones(state.copy(glow = (state.glow - quote.cost).coerceAtLeast(0.0), owned = counts.toList()))
	}

	fun canResearch(state: GameState, research: Research) = research.id !in state.research &&
		(research.prerequisite == null || research.prerequisite in state.research) &&
		(research.cultivator == null || state.count(research.cultivator) >= research.requiredCount)

	fun research(state: GameState, id: String): GameState {
		val item = Almanac.researchById[id] ?: return state
		if (!canResearch(state, item) || state.glow < item.cost) {
			return state
		}

		return state.copy(glow = state.glow - item.cost, research = state.research + id)
	}

	fun offlineLimitHours(state: GameState) = when {
		"dream-garden" in state.research -> 24
		"long-nights" in state.research -> 12
		else -> 8
	}

	fun offline(state: GameState, nowMillis: Long): OfflineGrowth {
		val now = nowMillis.coerceAtLeast(0L)
		if (state.lastSeenMillis == 0L || now <= state.lastSeenMillis) {
			return OfflineGrowth(state.copy(lastSeenMillis = maxOf(now, state.lastSeenMillis)), 0.0, 0L, false)
		}

		val elapsed = now - state.lastSeenMillis
		val limit = offlineLimitHours(state) * 3_600_000L
		val creditedMillis = elapsed.coerceAtMost(limit)
		val advanced = tick(state, creditedMillis / 1_000.0).copy(lastSeenMillis = now)

		return OfflineGrowth(advanced, advanced.glow - state.glow, creditedMillis / 1_000L, elapsed > limit, advanced.moondew - state.moondew)
	}

	fun prestigeReward(state: GameState): Long {
		val available = MAX_SEEDS - state.dawnSeeds

		return floor(sqrt(state.runGlow / PRESTIGE_THRESHOLD)).coerceAtMost(available.toDouble()).toLong()
	}

	fun nextSeedAt(state: GameState) = PRESTIGE_THRESHOLD * (prestigeReward(state) + 1.0).pow(2)

	/** A single reversible-at-dawn strategy choice, available only after the first restored dawn. */
	fun attune(state: GameState, constellation: Constellation): GameState {
		if (state.dawns == 0 || state.constellation != null) {
			return state
		}

		return rememberMilestones(state.copy(constellation = constellation))
	}

	fun prestige(state: GameState): GameState {
		val reward = prestigeReward(state)
		if (reward == 0L) {
			return state
		}

		return rememberMilestones(
			GameState(
				milestones = state.milestones,
				lifetimeGlow = state.lifetimeGlow,
				dawnSeeds = state.dawnSeeds + reward,
				dawns = (state.dawns + 1).coerceAtMost(Int.MAX_VALUE - 1),
				skyworks = state.skyworks,
				moondew = state.moondew,
				windwardLevels = state.windwardLevels,
				taps = state.taps,
				lastSeenMillis = state.lastSeenMillis,
				preferences = state.preferences
			)
		)
	}

	fun chapter(state: GameState) = when {
		state.runGlow >= 1_000_000 -> 5
		state.runGlow >= 300_000 -> 4
		state.runGlow >= 40_000 -> 3
		state.runGlow >= 4_500 -> 2
		state.runGlow >= 80 -> 1
		else -> 0
	}

	fun earn(state: GameState, amount: Double): GameState {
		if (amount.isNaN() || amount <= 0.0) {
			return state
		}

		val gain = bounded(amount)
		return rememberMilestones(
			state.copy(
				glow = bounded(state.glow + gain),
				runGlow = bounded(state.runGlow + gain),
				lifetimeGlow = bounded(state.lifetimeGlow + gain)
			)
		)
	}

	private fun rememberMilestones(state: GameState) = state.copy(
		milestones = state.milestones + Almanac.milestones.filter { it.earned(state) }.map { it.id }
	)

	private fun bounded(value: Double) = value.coerceIn(0.0, MAX_GLOW)
}
