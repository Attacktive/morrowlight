package xyz.attacktive.morrowlight.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Preserve normal click and accessibility behavior while repeating a held cultivator purchase. */
@Composable
fun RepeatBuyButton(onBuy: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, onPressed: (Boolean) -> Unit = {}, content: @Composable RowScope.() -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }
	val currentBuy by rememberUpdatedState(onBuy)
	val currentPressed by rememberUpdatedState(onPressed)
	var repeated by remember { mutableStateOf(false) }

	LaunchedEffect(interactionSource, enabled) {
		if (!enabled) {
			repeated = false

			return@LaunchedEffect
		}

		interactionSource.interactions.collectLatest { interaction ->
			when (interaction) {
				is PressInteraction.Press -> {
					repeated = false
					currentPressed(true)

					try {
						delay(HOLD_DELAY_MILLIS.milliseconds)

						while (true) {
							repeated = true
							currentBuy()
							delay(REPEAT_DELAY_MILLIS.milliseconds)
						}
					} finally {
						currentPressed(false)
					}
				}

				is PressInteraction.Cancel -> repeated = false
				else -> Unit
			}
		}
	}

	Button(
		onClick = {
			val click = !repeated
			repeated = false
			if (click) {
				currentBuy()
			}
		},
		enabled = enabled,
		modifier = modifier,
		shape = RoundedCornerShape(14.dp),
		colors = ButtonDefaults.buttonColors(containerColor = Mint.copy(alpha = 0.13f), contentColor = Mint),
		interactionSource = interactionSource,
		content = content
	)
}
