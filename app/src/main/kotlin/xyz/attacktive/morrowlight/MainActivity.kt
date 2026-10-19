package xyz.attacktive.morrowlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import xyz.attacktive.morrowlight.ui.Morrowlight

class MainActivity: ComponentActivity() {
	private lateinit var garden: GardenViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

		garden = ViewModelProvider(this)[GardenViewModel::class.java]
		setContent { Morrowlight(garden) }
	}

	override fun onStart() {
		super.onStart()
		garden.resume()
	}

	override fun onStop() {
		garden.pause()
		super.onStop()
	}
}
