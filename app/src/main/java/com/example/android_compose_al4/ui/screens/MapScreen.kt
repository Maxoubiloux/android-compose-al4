package com.example.android_compose_al4.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

data class AtmLocation(
    val name: String,
    val position: LatLng,
    val address: String
)

@Composable
fun MapScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val paris = LatLng(48.8566, 2.3522)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(paris, 12f)
        }

        val atms = listOf(
            AtmLocation(
                name = "DAB Opéra",
                position = LatLng(48.8709, 2.3326),
                address = "19 Av. de l'Opéra, 75001 Paris"
            ),
            AtmLocation(
                name = "DAB Châtelet",
                position = LatLng(48.8585, 2.3470),
                address = "Place du Châtelet, 75001 Paris"
            ),
            AtmLocation(
                name = "DAB République",
                position = LatLng(48.8674, 2.3630),
                address = "Pl. de la République, 75011 Paris"
            ),
            AtmLocation(
                name = "DAB Montparnasse",
                position = LatLng(48.8414, 2.3208),
                address = "Bd du Montparnasse, 75014 Paris"
            ),
            AtmLocation(
                name = "DAB Bastille",
                position = LatLng(48.8530, 2.3690),
                address = "Pl. de la Bastille, 75011 Paris"
            )
        )

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = rememberMarkerState(position = paris),
                title = "Paris",
                snippet = "Centre"
            )
            atms.forEach { atm ->
                Marker(
                    state = rememberMarkerState(position = atm.position),
                    title = atm.name,
                    snippet = atm.address
                )
            }
        }
    }
}
