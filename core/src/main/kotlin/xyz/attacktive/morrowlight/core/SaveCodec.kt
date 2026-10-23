package xyz.attacktive.morrowlight.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.MessageDigest

class UnsupportedSaveVersion(version: Int): IOException("Save version $version needs a newer Morrowlight.")

data class SavedGarden(val revision: Long, val state: GameState)

/** Versioned binary payload with integrity checking. No Java object deserialization. */
object SaveCodec {
	const val MAX_BYTES = 65_536
	private const val MAGIC = 0x4D4F5252
	private const val VERSION = 4

	fun encode(save: SavedGarden): ByteArray {
		validate(save)

		val bytes = ByteArrayOutputStream()

		DataOutputStream(bytes).use { out ->
			out.writeInt(MAGIC)
			out.writeInt(VERSION)
			out.writeLong(save.revision)
			val state = save.state
			out.writeDouble(state.glow)
			out.writeDouble(state.runGlow)
			out.writeDouble(state.lifetimeGlow)
			state.owned.forEach(out::writeInt)
			out.writeInt(state.research.size)
			state.research.sorted().forEach(out::writeUTF)
			out.writeInt(state.milestones.size)
			state.milestones.sorted().forEach(out::writeUTF)
			out.writeLong(state.dawnSeeds)
			out.writeInt(state.dawns)
			out.writeLong(state.taps)
			out.writeInt(state.charge)
			out.writeLong(state.lastSeenMillis)
			out.writeBoolean(state.preferences.reducedMotion)
			out.writeBoolean(state.preferences.haptics)
			out.writeUTF(state.constellation?.name.orEmpty())
			state.skyworks.forEach(out::writeDouble)
			out.writeDouble(state.moondew)
			out.writeUTF(state.routing.name)
			state.windwardLevels.forEach(out::writeInt)
		}

		val payload = bytes.toByteArray()

		return payload + MessageDigest.getInstance("SHA-256").digest(payload)
	}

	fun decode(bytes: ByteArray): SavedGarden {
		require(bytes.size in 48..MAX_BYTES) { "Invalid save size." }

		val payload = bytes.copyOfRange(0, bytes.size - 32)
		val checksum = bytes.copyOfRange(bytes.size - 32, bytes.size)

		require(MessageDigest.isEqual(checksum, MessageDigest.getInstance("SHA-256").digest(payload))) { "Save checksum does not match." }

		return DataInputStream(ByteArrayInputStream(payload)).use { input ->
			require(input.readInt() == MAGIC) { "Not a Morrowlight save." }

			val version = input.readInt()
			if (version !in 1..VERSION) {
				throw UnsupportedSaveVersion(version)
			}

			val revision = input.readLong()
			val glow = input.readDouble()
			val runGlow = input.readDouble()
			val lifetime = input.readDouble()
			val counts = List(Cultivator.entries.size) { input.readInt() }
			val researchSize = input.readInt()
			require(researchSize in 0..Almanac.research.size) { "Invalid research count." }

			val research = List(researchSize) { input.readUTF() }
			require(research.distinct().size == researchSize) { "Duplicate research." }

			val milestoneSize = input.readInt()
			require(milestoneSize in 0..Almanac.milestones.size)

			val milestones = List(milestoneSize) { input.readUTF() }
			require(milestones.distinct().size == milestoneSize)

			val state = GameState(
				glow = glow,
				runGlow = runGlow,
				lifetimeGlow = lifetime,
				owned = counts,
				research = research.toSet(),
				milestones = milestones.toSet(),
				dawnSeeds = input.readLong(),
				dawns = input.readInt(),
				taps = input.readLong(),
				charge = input.readInt(),
				lastSeenMillis = input.readLong(),
				preferences = Preferences(input.readBoolean(), input.readBoolean()),
				constellation = if (version >= 2) decodeConstellation(input.readUTF()) else null,
				skyworks = if (version >= 3) List(Skywork.entries.size) { input.readDouble() } else List(Skywork.entries.size) { 0.0 },
				moondew = if (version >= 4) input.readDouble() else 0.0,
				routing = if (version >= 4) decodeMoondewRouting(input.readUTF()) else MoondewRouting.NONE,
				windwardLevels = if (version >= 4) List(WindwardStructure.entries.size) { input.readInt() } else List(WindwardStructure.entries.size) { 0 }
			)

			require(input.available() == 0) { "Unexpected save content." }

			SavedGarden(revision, state).also(::validate)
		}
	}

	private fun decodeConstellation(name: String): Constellation? {
		if (name.isEmpty()) {
			return null
		}

		return Constellation.entries.firstOrNull { it.name == name } ?: throw IllegalArgumentException("Unknown constellation.")
	}

	private fun decodeMoondewRouting(name: String) = MoondewRouting.entries.firstOrNull { it.name == name } ?: throw IllegalArgumentException("Unknown Moondew route.")

	fun validate(save: SavedGarden) {
		val state = save.state

		require(save.revision in 0 until Long.MAX_VALUE)
		require(listOf(state.glow, state.runGlow, state.lifetimeGlow).all { it.isFinite() && it in 0.0..MAX_GLOW })
		require(state.glow <= state.runGlow && state.runGlow <= state.lifetimeGlow)
		require(state.owned.size == Cultivator.entries.size && state.owned.all { it in 0..MAX_OWNED })
		require(state.research.all { it in Almanac.researchById })
		require(state.milestones.all { id -> Almanac.milestones.any { it.id == id } })
		require(state.dawnSeeds in 0..MAX_SEEDS && state.dawns in 0 until Int.MAX_VALUE)
		require(state.constellation == null || state.dawns > 0)
		require(state.skyworks.size == Skywork.entries.size)
		require(Skywork.entries.all { work -> state.skyworks[work.ordinal].isFinite() && state.skyworks[work.ordinal] in 0.0..work.cost })
		require(state.dawns > 0 || state.skyworks.all { it == 0.0 })
		require(Skywork.entries.drop(1).all { work ->
			state.skyworks[work.ordinal] == 0.0 || state.skyworks[work.ordinal - 1] == Skywork.entries[work.ordinal - 1].cost
		})
		require(state.moondew.isFinite() && state.moondew in 0.0..MAX_GLOW)
		require(state.windwardLevels.size == WindwardStructure.entries.size)
		require(WindwardStructure.entries.all { it.maxLevel >= state.windwardLevels[it.ordinal] && state.windwardLevels[it.ordinal] >= 0 })
		require(Garden.windwardUnlocked(state) || (state.moondew == 0.0 && state.routing == MoondewRouting.NONE && state.windwardLevels.all { it == 0 }))
		require(state.taps in 0 until Long.MAX_VALUE && state.charge in 0 until FIREFLY_CHARGE)
		require(state.lastSeenMillis >= 0L)
	}
}
