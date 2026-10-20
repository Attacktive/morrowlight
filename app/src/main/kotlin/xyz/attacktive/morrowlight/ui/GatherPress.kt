package xyz.attacktive.morrowlight.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal const val HOLD_DELAY_MILLIS = 350L
internal const val REPEAT_DELAY_MILLIS = 120L

/** Gather immediately and repeat while pressed, without allowing a scroll gesture to interrupt the hold. */
@Composable
fun Modifier.gatherPress(gather: (repeated: Boolean) -> Unit, onPressed: (Boolean) -> Unit = {}): Modifier {
	val currentGather by rememberUpdatedState(gather)
	val currentPressed by rememberUpdatedState(onPressed)

	return pointerInput(Unit) {
		coroutineScope {
			awaitEachGesture {
				val down = awaitFirstDown()
				down.consume()
				currentPressed(true)

				val repeating = this@coroutineScope.launch {
					delay(HOLD_DELAY_MILLIS.milliseconds)

					while (isActive) {
						currentGather(true)
						delay(REPEAT_DELAY_MILLIS.milliseconds)
					}
				}

				try {
					currentGather(false)

					do {
						val event = awaitPointerEvent()
						event.changes.forEach { it.consume() }
					} while (event.changes.any { it.id == down.id && it.pressed })
				} finally {
					repeating.cancel()
					currentPressed(false)
				}
			}
		}
	}.semantics {
		role = Role.Button
		contentDescription = "Gather light. Press and hold to gather continuously."
		onClick(label = "Gather light") {
			currentGather(false)
			true
		}
	}
}
