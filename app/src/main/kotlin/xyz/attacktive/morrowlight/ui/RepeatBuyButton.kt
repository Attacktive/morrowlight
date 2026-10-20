package xyz.attacktive.morrowlight.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Recognize Plant touches even when an active list fling consumes the pointer down. */
@Composable
fun RepeatBuyButton(onBuy: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, onPressed: (Boolean) -> Unit = {}, content: @Composable RowScope.() -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }
	val currentBuy by rememberUpdatedState(onBuy)
	val currentPressed by rememberUpdatedState(onPressed)

	Button(
		onClick = { currentBuy() },
		enabled = enabled,
		modifier = modifier.pointerInput(enabled, interactionSource) {
			if (!enabled) {
				return@pointerInput
			}

			coroutineScope {
				awaitEachGesture {
					val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
					down.consume()
					currentPressed(true)

					val press = PressInteraction.Press(down.position)
					var repeated = false
					var released = false
					var canceled = false
					val repeating = this@coroutineScope.launch {
						delay(HOLD_DELAY_MILLIS.milliseconds)

						while (isActive) {
							repeated = true
							currentBuy()
							delay(REPEAT_DELAY_MILLIS.milliseconds)
						}
					}

					try {
						interactionSource.tryEmit(press)

						do {
							val event = awaitPointerEvent(pass = PointerEventPass.Initial)
							val pointer = event.changes.firstOrNull { it.id == down.id }
							event.changes.forEach { it.consume() }
							canceled = pointer == null || pointer.isOutOfBounds(size, extendedTouchPadding)
							released = pointer != null && !pointer.pressed
						} while (!released && !canceled)
					} finally {
						repeating.cancel()
						interactionSource.tryEmit(if (released && !canceled) PressInteraction.Release(press) else PressInteraction.Cancel(press))
						currentPressed(false)
					}

					if (released && !canceled && !repeated) {
						currentBuy()
					}
				}
			}
		},
		shape = RoundedCornerShape(14.dp),
		colors = ButtonDefaults.buttonColors(containerColor = Mint.copy(alpha = 0.13f), contentColor = Mint),
		interactionSource = interactionSource,
		content = content
	)
}
