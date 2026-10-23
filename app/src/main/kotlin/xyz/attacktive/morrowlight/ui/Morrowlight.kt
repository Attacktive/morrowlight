package xyz.attacktive.morrowlight.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.attacktive.morrowlight.GardenUiState
import xyz.attacktive.morrowlight.GardenViewModel
import xyz.attacktive.morrowlight.core.Almanac
import xyz.attacktive.morrowlight.core.BuyAmount
import xyz.attacktive.morrowlight.core.Constellation
import xyz.attacktive.morrowlight.core.Contribution
import xyz.attacktive.morrowlight.core.Cultivator
import xyz.attacktive.morrowlight.core.Display
import xyz.attacktive.morrowlight.core.FIREFLY_CHARGE
import xyz.attacktive.morrowlight.core.GameState
import xyz.attacktive.morrowlight.core.Garden
import xyz.attacktive.morrowlight.core.MAX_OWNED
import xyz.attacktive.morrowlight.core.PRESTIGE_THRESHOLD
import xyz.attacktive.morrowlight.core.Preferences
import xyz.attacktive.morrowlight.core.Research
import xyz.attacktive.morrowlight.core.Skywork

private val chapterNames = listOf("A spark in the quiet", "Small things, growing", "The moon takes root", "Between earth and sky", "The stars came home", "A morning worth making")
private val chapterNotes = listOf("Even the longest night begins to end somewhere.", "Your little patch of dark is becoming a garden.", "Silver leaves. Golden lanterns. Room to breathe.", "The island is learning how to hold the sky.", "You made a home for something extraordinary.", "Let this light become the seed of another dawn.")

@Composable
fun Morrowlight(model: GardenViewModel) {
	val ui by model.ui.collectAsStateWithLifecycle()

	MorrowTheme {
		GardenApp(
			ui = ui,
			onTap = model::tap,
			onBuy = model::buy,
			onResearch = model::research,
			onPrestige = model::prestige,
			onAttune = model::attune,
			onContribute = model::contributeSkywork,
			onPreferences = model::preferences,
			onDismissOffline = model::dismissOffline,
			onDismissRecovery = model::dismissRecovery,
			onRetry = model::load
		)
	}
}

@Composable
private fun GardenApp(
	ui: GardenUiState,
	onTap: () -> Unit,
	onBuy: (Cultivator, BuyAmount) -> Unit,
	onResearch: (String) -> Unit,
	onPrestige: () -> Unit,
	onAttune: (Constellation) -> Unit,
	onContribute: (Skywork, Contribution) -> Unit,
	onPreferences: (Preferences) -> Unit,
	onDismissOffline: () -> Unit,
	onDismissRecovery: () -> Unit,
	onRetry: () -> Unit
) {
	var tab by rememberSaveable { mutableIntStateOf(0) }
	var settings by rememberSaveable { mutableStateOf(false) }
	var confirmDawn by rememberSaveable { mutableStateOf(false) }
	var pendingConstellation by rememberSaveable { mutableStateOf<Constellation?>(null) }
	val state = ui.garden
	val haptics = LocalHapticFeedback.current
	val touch: (Boolean) -> Unit = { repeating ->
		if (!repeating && state.preferences.haptics) {
			haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
		}

		onTap()
	}

	BackHandler(tab != 0 && !settings && !confirmDawn) { tab = 0 }

	Scaffold(
		containerColor = Night,
		bottomBar = {
			NavigationBar(containerColor = Night, tonalElevation = 0.dp) {
				listOf("Garden" to Symbol.SEED, "Cultivate" to Symbol.GROW, "Skyworks" to Symbol.BRIDGE, "Dawn" to Symbol.SUN).forEachIndexed { index, (label, symbol) ->
					NavigationBarItem(
						selected = tab == index, onClick = { tab = index },
						icon = { Glyph(symbol = symbol, color = if (tab == index) Mint else Muted) },
						label = { Text(label) },
						colors = NavigationBarItemDefaults.colors(selectedIconColor = Mint, selectedTextColor = Mint, indicatorColor = Mint.copy(alpha = 0.09f), unselectedTextColor = Muted)
					)
				}
			}
		}
	) { insets ->
		Box(Modifier
			.fillMaxSize()
			.padding(insets)
			.background(Brush.verticalGradient(listOf(Night, Color(0xFF14262C)))), contentAlignment = Alignment.TopCenter
		) {
			Column(Modifier
				.widthIn(max = 700.dp)
				.fillMaxSize()
			) {
				Row(Modifier
					.fillMaxWidth()
					.padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically
				) {
					Column(Modifier.weight(1f)) {
						Text("Morrowlight", fontFamily = FontFamily.Serif, fontSize = 32.sp, color = Ink)
						Text("A LITTLE LIGHT, EVERY DAY", style = MaterialTheme.typography.labelSmall, color = Muted)
					}

					IconButton(onClick = { settings = true }, modifier = Modifier.semantics { contentDescription = "Settings" }) { Glyph(symbol = Symbol.SETTINGS, color = Muted) }
				}

				when {
					ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Mint) }
					ui.loadError != null -> Column(Modifier
						.padding(24.dp)
						.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)
					) {
						Text("Your garden is resting safely.", style = MaterialTheme.typography.headlineMedium)
						Text(ui.loadError, color = Muted)
						Text("No save has been replaced. Retry after resolving storage access, or install a newer app when requested.", color = Muted)
						Button(onClick = onRetry) { Text("Try reading again") }
					}

					else -> {
						ui.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
						when (tab) {
							0 -> GardenHome(state, ui.active, touch)
							1 -> CultivateScreen(state, onBuy, onResearch)
							2 -> SkyworksScreen(state, onContribute)
							3 -> DawnScreen(state, { confirmDawn = true }) { pendingConstellation = it }
						}
					}
				}
			}
		}
	}

	if (settings) {
		SettingsDialog(state, onPreferences) { settings = false }
	}

	if (confirmDawn) {
		val reward = Garden.prestigeReward(state)

		AlertDialog(
			onDismissRequest = { confirmDawn = false }, icon = { Glyph(symbol = Symbol.SUN, color = Gold) },
			title = { Text("Begin again, brighter.", fontFamily = FontFamily.Serif) },
			text = { Text("Receive $reward Dawn Seed${if (reward == 1L) "" else "s"}. Your Glow, cultivators, research, and firefly charge reset. All Dawn Seeds, Skyworks restorations, memories, lifetime Glow, and settings stay with you.") },
			confirmButton = {
				TextButton(onClick = {
					onPrestige()
					confirmDawn = false
					tab = 0
				}, enabled = reward > 0) { Text("Restore the dawn") }
			},
			dismissButton = { TextButton(onClick = { confirmDawn = false }) { Text("Keep growing") } }
		)
	}

	pendingConstellation?.let { option ->
		AlertDialog(
			onDismissRequest = { pendingConstellation = null },
			icon = { Glyph(symbol = Symbol.SPARK, color = Lilac) },
			title = { Text("Follow ${option.title}?", fontFamily = FontFamily.Serif) },
			text = { Text("${option.description} This choice lasts until your next dawn.") },
			confirmButton = {
				TextButton(onClick = {
					onAttune(option)
					pendingConstellation = null
				}) { Text("Follow this path") }
			},
			dismissButton = { TextButton(onClick = { pendingConstellation = null }) { Text("Not yet") } }
		)
	}

	ui.offline?.let { growth ->
		AlertDialog(
			onDismissRequest = onDismissOffline, icon = { Glyph(symbol = Symbol.SPARK, color = Gold) },
			title = { Text("While the world was quiet", fontFamily = FontFamily.Serif) },
			text = {
				Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
					Text("+${Display.number(growth.earned)} Glow", fontSize = 30.sp, color = Gold)
					Text("Your garden grew for ${Display.duration(growth.seconds)} at full production. This light is already yours.")

					if (growth.capped) {
						Text("Offline growth reached your ${Garden.offlineLimitHours(state)}-hour limit.", color = Muted)
					}
				}
			},
			confirmButton = { TextButton(onClick = onDismissOffline) { Text("Back to the garden") } }
		)
	}

	if (ui.recovered && ui.offline == null) {
		AlertDialog(
			onDismissRequest = onDismissRecovery, title = { Text("Your last safe checkpoint is back") },
			text = { Text("The newest save was unreadable, so Morrowlight recovered the previous checkpoint. A little recent progress may be missing.") },
			confirmButton = { TextButton(onClick = onDismissRecovery) { Text("Continue") } }
		)
	}
}

@Composable
private fun GardenHome(state: GameState, active: Boolean, onGather: (Boolean) -> Unit) {
	val chapter = Garden.chapter(state)
	val listState = rememberLazyListState()
	var gathering by remember { mutableStateOf(false) }

	LaunchedEffect(gathering) {
		if (gathering) {
			listState.stopScroll()
		}
	}

	LazyColumn(state = listState, userScrollEnabled = !gathering, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
		item { Balance(state) }
		item {
			Box(Modifier
				.fillMaxWidth()
				.clip(RoundedCornerShape(28.dp))
				.border(1.dp, Hairline, RoundedCornerShape(28.dp))
			) {
				GardenScene(state, active, Modifier
					.fillMaxWidth()
					.aspectRatio(480f / 380f)
					.gatherPress(onGather) { gathering = it }
				)

				Row(Modifier
					.padding(16.dp)
					.clip(CircleShape)
					.background(Night.copy(alpha = 0.65f))
					.padding(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically
				) {
					Box(Modifier
						.size(5.dp)
						.background(Mint, CircleShape))
					Text("GARDEN ${state.dawns + 1}", style = MaterialTheme.typography.labelSmall, color = Ink)
				}
			}
		}

		item {
			Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
				Text(chapterNames[chapter], style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
				Text(chapterNotes[chapter], color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
			}
		}

		if (state.dawns > 0) {
			val restored = Skywork.entries.count { Garden.skyworkComplete(state, it) }
			item { Text("Skyworks: $restored of ${Skywork.entries.size} restorations complete. Every contribution survives the next dawn.", color = Gold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
		}

		if (state.dawns > 0 && state.constellation == null) {
			item { Text("A new constellation awaits in Dawn. Choose a path for this garden.", color = Gold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
		}

		item {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(min = 68.dp)
					.clip(RoundedCornerShape(20.dp))
					.background(Mint)
					.gatherPress(onGather) { gathering = it }
					.padding(16.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Glyph(symbol = Symbol.SPARK, color = Night)

				Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
					Text("Hold to gather light", style = MaterialTheme.typography.titleMedium, color = Night)
					Text("PRESS AND HOLD TO CONTINUOUSLY GATHER", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Night.copy(alpha = 0.7f))
				}

				Text("+${Display.number(Garden.tapValue(state))}", color = Night, fontWeight = FontWeight.Bold)
			}
		}

		item {
			Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
					Text("FIREFLY WISH", style = MaterialTheme.typography.labelSmall, color = Gold)
					Text("${state.charge} / $FIREFLY_CHARGE touches", color = Muted, fontSize = 12.sp)
				}

				LinearProgressIndicator(progress = { state.charge / FIREFLY_CHARGE.toFloat() }, modifier = Modifier
					.fillMaxWidth()
					.height(5.dp)
					.clip(CircleShape), color = Gold, trackColor = Hairline)
				Text("Every 25 touches brings a gift of ${Garden.fireflySeconds(state)} seconds of production.", color = Muted, fontSize = 12.sp)
			}
		}

		item { Text("No rush. The light will wait for you.", color = Muted, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 8.dp)) }
	}
}

@Composable
private fun Balance(state: GameState) {
	Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
		Text("GLOW", style = MaterialTheme.typography.labelSmall, color = Gold)
		Text(Display.number(state.glow), fontSize = 46.sp, lineHeight = 52.sp, fontWeight = FontWeight.Light, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { contentDescription = "${Display.number(state.glow)} Glow available" })

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
			Text("+${Display.number(Garden.production(state))} / second", color = Mint, fontSize = 14.sp)

			if (state.dawnSeeds > 0) {
				Text("${Display.number(Garden.seedMultiplier(state))}x dawn bonus", color = Gold, fontSize = 12.sp)
			}
		}

		if (Garden.skyworkComplete(state, Skywork.WINDMILLS)) {
			Text("Restored windmills: +15% permanent production", color = Gold, fontSize = 12.sp)
		}
	}
}

@Composable
private fun CultivateScreen(state: GameState, onBuy: (Cultivator, BuyAmount) -> Unit, onResearch: (String) -> Unit) {
	var section by rememberSaveable { mutableIntStateOf(0) }
	var amountIndex by rememberSaveable { mutableIntStateOf(0) }
	val amount = BuyAmount.entries[amountIndex]
	val listState = rememberLazyListState()
	var purchasing by remember { mutableStateOf(false) }

	LaunchedEffect(purchasing) {
		if (purchasing) {
			listState.stopScroll()
		}
	}

	Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
		Box(Modifier.padding(top = 20.dp, bottom = 14.dp)) {
			Balance(state)
		}

		LazyColumn(
			modifier = Modifier.weight(1f),
			state = listState,
			userScrollEnabled = !purchasing,
			contentPadding = PaddingValues(bottom = 20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp)
		) {
			item {
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("Cultivators") })
					FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("Research") })
				}
			}

			if (section == 0) {
				item {
					Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
						Text("PLANT AT ONCE", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
						BuyAmount.entries.forEachIndexed { index, item ->
							FilterChip(selected = index == amountIndex, onClick = { amountIndex = index }, label = { Text(item.label) }, modifier = Modifier.padding(start = 6.dp))
						}
					}
				}

				items(Cultivator.entries, key = { it.name }) { plant ->
					CultivatorCard(state, plant, amount, { onBuy(plant, amount) }) { purchasing = it }
				}

				item { Text("10 and Max plant as many as you can afford. Hold Plant to buy repeatedly. Payback estimates use the extra passive Glow per second at current rates, including synergies.", color = Muted, fontSize = 12.sp) }
			} else {
				item { Text("Make a little more of every light.", style = MaterialTheme.typography.headlineMedium) }
				items(Almanac.research.sortedBy { it.id in state.research }, key = { it.id }) { research -> ResearchCard(state, research) { onResearch(research.id) } }
			}
		}
	}
}

private fun synergyHint(cultivator: Cultivator) = when (cultivator) {
	Cultivator.GLOWCAP -> "Every 10 Glowcaps boost Lantern lily production by 20%, up to 100%."
	Cultivator.MOONWILLOW -> "Every 5 Moonwillows boost Firefly haven production by 15%, up to 75%."
	Cultivator.CLOUD_LOOM -> "Every 5 Cloud looms boost Starwell production by 10%, up to 100%."
	else -> null
}

@Composable
private fun CultivatorCard(state: GameState, plant: Cultivator, amount: BuyAmount, onBuy: () -> Unit, onPressed: (Boolean) -> Unit) {
	val discovered = Garden.discovered(state, plant)
	val count = state.count(plant)
	val quote = Garden.quote(state, plant, amount.limit)
	val gain = if (quote.quantity > 0) Garden.productionGain(state, plant, quote.quantity) else 0.0
	val nextMilestone = Garden.nextColonyMilestone(count)

	CardBox {
		Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			PlantIcon(plant)

			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(plant.title, style = MaterialTheme.typography.titleMedium)
				Text(if (discovered) "+${Display.number(Garden.production(state, plant))} Glow / sec" else "An undiscovered light", color = if (discovered) Mint else Muted, fontSize = 12.sp)
			}

			Text(count.toString().padStart(2, '0'), color = if (count > 0) Ink else Muted, fontFamily = FontFamily.Serif, fontSize = 28.sp)
		}

		Text(plant.description, color = Muted, style = MaterialTheme.typography.bodyMedium)
		if (state.dawns > 0) {
			synergyHint(plant)?.let { Text(it, color = Muted, fontSize = 12.sp) }
		}

		val synergy = Garden.synergyMultiplier(state, plant)
		if (synergy > 1.0) {
			Text("+${Display.number((synergy - 1.0) * 100.0)}% from supporting cultivators", color = Gold, fontSize = 12.sp)
		}

		if (discovered) {
			if (nextMilestone != null) {
				LinearProgressIndicator(progress = { count.toFloat() / nextMilestone }, modifier = Modifier
					.fillMaxWidth()
					.height(3.dp)
					.clip(CircleShape), color = Mint.copy(alpha = 0.65f), trackColor = Hairline)
				Text("${nextMilestone - count} more to double this colony's production", color = Muted, fontSize = 12.sp)
			} else {
				Text("All colony milestones reached", color = Gold, fontSize = 12.sp)
			}

			RepeatBuyButton(onBuy = onBuy, enabled = quote.quantity > 0, onPressed = onPressed, modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 48.dp)
			) {
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
					val label = when {
						count >= MAX_OWNED -> "Fully flourishing"
						quote.quantity > 0 -> "Plant ${quote.quantity}"
						else -> "Need more Glow"
					}

					Text(label)

					if (count < MAX_OWNED) {
						val price = if (quote.quantity > 0) {
							quote.cost
						} else {
							Garden.nextCost(state, plant)
						}

						Text("${Display.number(price)} Glow")
					}
				}
			}
			if (quote.quantity > 0) {
				Text("+${Display.number(gain)} Glow/s · ${Display.payback(quote.cost, gain)} payback", color = Mint, fontSize = 12.sp)
			}
		} else {
			Text("Discover at ${Display.number(plant.discovery)} Glow earned this garden", color = Gold, fontSize = 13.sp)
		}
	}
}

@Composable
private fun ResearchCard(state: GameState, research: Research, onResearch: () -> Unit) {
	val learned = research.id in state.research
	val available = Garden.canResearch(state, research)

	CardBox {
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
			val symbol = if (learned) Symbol.SPARK else Symbol.BOOK
			val color = if (learned) Mint else Lilac

			Glyph(symbol = symbol, color = color)
			Text(research.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
		}

		Text(research.description, color = Muted)
		if (learned) {
			Text("LEARNED", color = Mint, style = MaterialTheme.typography.labelSmall)
		} else {
			if (!available) {
				val prerequisite = research.prerequisite?.takeUnless { it in state.research }
					?.let { Almanac.researchById.getValue(it).title }

				val hint = if (prerequisite != null) "First discover $prerequisite" else "Grow ${research.requiredCount} ${research.cultivator?.title}s first"
				Text(hint, color = Lilac, fontSize = 12.sp)
			}

			OutlinedButton(onClick = onResearch, enabled = available && state.glow >= research.cost, modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 48.dp)) { Text("Discover \u00b7 ${Display.number(research.cost)} Glow") }
		}
	}
}


@Composable
private fun SkyworksScreen(state: GameState, onContribute: (Skywork, Contribution) -> Unit) {
	var contributionIndex by rememberSaveable { mutableIntStateOf(0) }
	val contribution = Contribution.entries[contributionIndex]
	val finished = Skywork.entries.count { Garden.skyworkComplete(state, it) }

	LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
		item {
			Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
				Box(Modifier.size(80.dp).background(Lilac.copy(alpha = 0.09f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Modifier.size(42.dp), Symbol.BRIDGE, Lilac) }
				Text("A path beyond the stars.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
				Text("Restore what the night has forgotten. Every Glow you contribute stays invested across dawns.", color = Muted, textAlign = TextAlign.Center)
			}
		}

		if (state.dawns == 0) {
			item {
				CardBox {
					Text("AWAITING THE FIRST DAWN", style = MaterialTheme.typography.labelSmall, color = Gold)
					Text("The faraway lanterns will reveal themselves after you restore your first dawn.", color = Muted)
				}
			}
		} else {
			item { SectionHeading("Skyworks", "$finished of ${Skywork.entries.size} restorations complete") }

			item {
				CardBox {
					Text("CHOOSE A CONTRIBUTION", style = MaterialTheme.typography.labelSmall, color = Gold)
					Text("Decide how much of your current Glow goes toward restoration instead of cultivators or research.", color = Muted)
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						Contribution.entries.forEachIndexed { index, option ->
							FilterChip(selected = contributionIndex == index, onClick = { contributionIndex = index }, label = { Text(option.title) })
						}
					}
					Text("10%, 50%, or all of your available Glow, capped by the unfinished project. Partial investments are permanent and cannot be refunded.", color = Muted, fontSize = 12.sp)
				}
			}

			items(Skywork.entries, key = { it.name }) { work ->
				val restored = Garden.skyworkProgress(state, work)
				val remaining = (work.cost - restored).coerceAtLeast(0.0)
				val complete = Garden.skyworkComplete(state, work)
				val available = Garden.skyworkAvailable(state, work)

				CardBox {
					Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
						Glyph(symbol = if (complete) Symbol.SPARK else Symbol.BRIDGE, color = if (complete) Gold else Lilac)
						Text(work.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
						if (complete) {
							Text("RESTORED", color = Mint, style = MaterialTheme.typography.labelSmall)
						}
					}

					Text(work.description, color = Muted)
					LinearProgressIndicator(progress = { (restored / work.cost).coerceIn(0.0, 1.0).toFloat() }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Gold, trackColor = Hairline)
					Text("${Display.number(restored)} / ${Display.number(work.cost)} Glow contributed", color = Ink, fontSize = 12.sp)
					Text(work.reward, color = if (complete) Mint else Muted, fontSize = 12.sp)

					if (available) {
						val quote = Garden.skyworkQuote(state, work, contribution)
						val afterAvailable = (remaining - state.glow).coerceAtLeast(0.0)
						val rate = Garden.production(state)
						val estimate = when {
							afterAvailable == 0.0 -> "You already hold enough Glow to complete this project."
							rate > 0.0 -> "At current production, around ${Display.payback(afterAvailable, rate)} to earn the Glow still needed, assuming you invest your current balance."
							else -> "Grow some cultivators to estimate the time to completion."
						}

						Text(estimate, color = Gold, fontSize = 12.sp)
						Button(onClick = { onContribute(work, contribution) }, enabled = quote > 0.0, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Night)) {
							Text("Contribute ${Display.number(quote)} Glow")
						}
					} else if (!complete) {
						Text("Restore ${Skywork.entries[work.ordinal - 1].title.lowercase()} first.", color = Lilac, fontSize = 12.sp)
					}
				}
			}

			if (finished == Skywork.entries.size) {
				item {
					CardBox {
						Text("ALL LIGHTS RESTORED", color = Gold, style = MaterialTheme.typography.labelSmall)
						Text("The next island is ready for a future update. Until then, your restored bonuses and memories remain yours.", color = Muted)
					}
				}
			}
		}
	}
}

@Composable
private fun DawnScreen(state: GameState, onPrestige: () -> Unit, onAttune: (Constellation) -> Unit) {
	val reward = Garden.prestigeReward(state)
	val constellation = state.constellation

	LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
		item {
			Column(Modifier
				.fillMaxWidth()
				.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Box(Modifier
					.size(80.dp)
					.background(Gold.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Modifier.size(42.dp), Symbol.SUN, Gold) }

				Text("Every ending, a beginning.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
				Text("Let a thriving garden become tomorrow's light.", color = Muted, textAlign = TextAlign.Center)
			}
		}
		item {
			CardBox {
				Text("DAWN SEEDS", color = Gold, style = MaterialTheme.typography.labelSmall)
				Text(state.dawnSeeds.toString(), fontFamily = FontFamily.Serif, fontSize = 42.sp, color = Gold)
				Text("${Display.number(Garden.seedMultiplier(state))}x permanent production and base touch power", color = Ink)
				Text("Each seed adds 15% to your permanent multiplier. Seeds are never spent.", color = Muted)
				HorizontalDivider(color = Hairline)

				val dawnMessage = if (reward > 0) {
					"+$reward seeds waiting in this dawn"
				} else {
					"The first dawn is growing"
				}

				Text(dawnMessage, style = MaterialTheme.typography.titleMedium)

				val target = if (reward == 0L) {
					PRESTIGE_THRESHOLD
				} else {
					Garden.nextSeedAt(state)
				}

				LinearProgressIndicator(progress = { (state.runGlow / target).coerceIn(0.0, 1.0).toFloat() }, modifier = Modifier
					.fillMaxWidth()
					.height(6.dp)
					.clip(CircleShape), color = Gold, trackColor = Hairline
				)

				val progressNote = if (reward > 0) "for the next seed" else "to restore the dawn"
				Text("${Display.number(state.runGlow)} / ${Display.number(target)} Glow earned $progressNote", color = Muted, fontSize = 12.sp)

				Button(
					onClick = onPrestige, enabled = reward > 0,
					modifier = Modifier
					.fillMaxWidth()
					.heightIn(min = 52.dp),
					colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)
				) {
					Text("Restore the dawn")
				}

				Text("Resets this garden's Glow, cultivators, and research. Your seeds, Skyworks restorations, and memories remain. You will confirm before anything resets.", color = Muted, fontSize = 12.sp)
			}
		}

		if (state.dawns > 0) {
			item { SectionHeading("Constellations", "Choose one strategy for this garden. You can choose again after restoring the next dawn.") }

			if (constellation == null) {
				items(Constellation.entries, key = { it.name }) { option ->
					CardBox {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
							Glyph(symbol = Symbol.SPARK, color = Lilac)
							Text(option.title, style = MaterialTheme.typography.titleMedium)
						}

						Text(option.description, color = Muted)
						OutlinedButton(onClick = { onAttune(option) }, modifier = Modifier
							.fillMaxWidth()
							.heightIn(min = 48.dp)
						) { Text("Follow this constellation") }
					}
				}
			} else {
				item {
					CardBox {
						Text("YOUR CONSTELLATION", color = Gold, style = MaterialTheme.typography.labelSmall)
						Text(constellation.title, style = MaterialTheme.typography.titleMedium)
						Text(constellation.description, color = Muted)
						Text("Your next dawn opens another choice.", color = Gold, fontSize = 12.sp)
					}
				}
			}
		}

		item { SectionHeading("Your almanac", "${state.milestones.size} of ${Almanac.milestones.size} memories collected") }

		items(Almanac.milestones, key = { it.id }) { milestone ->
			val earned = milestone.id in state.milestones

			CardBox {
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
					val symbol = if (earned) Symbol.SPARK else Symbol.SEED
					val color = if (earned) Gold else Muted
					Glyph(symbol = symbol, color = color)

					Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
						Text(milestone.title, style = MaterialTheme.typography.titleMedium, color = if (earned) Ink else Muted)
						Text(milestone.description, color = Muted, fontSize = 12.sp)

						if (earned) {
							Text("REMEMBERED", style = MaterialTheme.typography.labelSmall, color = Gold)
						}
					}
				}
			}
		}

		item {
			val summary = "${Display.number(state.lifetimeGlow)} lifetime Glow \u00b7 ${state.dawns} dawns restored"
			Text(summary, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
		}
	}
}

@Composable
private fun SettingsDialog(state: GameState, onPreferences: (Preferences) -> Unit, onDismiss: () -> Unit) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Make yourself at home.", fontFamily = FontFamily.Serif) },
		text = {
			Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
				PreferenceRow("Reduced motion", "Keep the garden still. Less movement, less battery use.", state.preferences.reducedMotion) { onPreferences(state.preferences.copy(reducedMotion = it)) }
				PreferenceRow("Gentle haptics", "A little response when you gather light.", state.preferences.haptics) { onPreferences(state.preferences.copy(haptics = it)) }
				HorizontalDivider(color = Hairline)
				Text("A garden, not a chore.", style = MaterialTheme.typography.titleMedium)
				Text("Offline growth runs at full production for up to ${Garden.offlineLimitHours(state)} hours. No background service, timer notification, or internet connection is needed.")
				Text("Progress is saved on purchases, on leaving the app, and every five seconds while playing. Two local checkpoints protect against interrupted writes.")
				Text("Morrowlight 1.0 \u00b7 No ads. No accounts. Just a little light.", color = Muted)
			}
		},
		confirmButton = { TextButton(onClick = onDismiss) { Text("Back to the quiet") } }
	)
}

@Composable
private fun PreferenceRow(title: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
	Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Text(title, style = MaterialTheme.typography.titleMedium)
			Text(description, color = Muted, fontSize = 12.sp)
		}

		Switch(checked = value, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = title })
	}
}

@Composable
private fun SectionHeading(title: String, note: String) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text(title, style = MaterialTheme.typography.headlineMedium)
		Text(note, color = Muted, fontSize = 13.sp)
	}
}

@Composable
private fun CardBox(content: @Composable ColumnScope.() -> Unit) {
	Column(Modifier
		.fillMaxWidth()
		.clip(RoundedCornerShape(22.dp))
		.background(Panel)
		.border(1.dp, Hairline.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
		.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content
	)
}

@Preview(name = "First light", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FirstLightPreview() {
	MorrowTheme {
		GardenApp(
			ui = GardenUiState(loading = false),
			onTap = {},
			onBuy = { _, _ -> },
			onResearch = {},
			onPrestige = {},
			onAttune = {},
			onContribute = { _, _ -> },
			onPreferences = {},
			onDismissOffline = {},
			onDismissRecovery = {},
			onRetry = {}
		)
	}
}

@Preview(name = "A living garden", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LivingGardenPreview() {
	val state = GameState(glow = 8_421.0, runGlow = 760_000.0, lifetimeGlow = 760_000.0, owned = listOf(35, 18, 12, 6, 3, 1))
	MorrowTheme {
		GardenApp(
			ui = GardenUiState(garden = state, loading = false),
			onTap = {},
			onBuy = { _, _ -> },
			onResearch = {},
			onPrestige = {},
			onAttune = {},
			onContribute = { _, _ -> },
			onPreferences = {},
			onDismissOffline = {},
			onDismissRecovery = {},
			onRetry = {}
		)
	}
}
