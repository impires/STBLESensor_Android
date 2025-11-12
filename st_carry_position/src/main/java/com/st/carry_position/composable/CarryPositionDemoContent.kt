package com.st.carry_position.composable

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.blue_sdk.features.carry_position.CarryPositionType
import com.st.carry_position.CarryPositionViewModel
import com.st.carry_position.R
import com.st.ui.composables.ComposableLifecycle
import com.st.ui.theme.LocalDimensions

@Composable
fun CarryPositionDemoContent(
    modifier: Modifier,
    viewModel: CarryPositionViewModel
) {
    val positionData by viewModel.positionData.collectAsStateWithLifecycle()

    val context = LocalContext.current

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                Toast.makeText(context, "Carry position started", Toast.LENGTH_SHORT).show()
            }

            else -> Unit
        }
    }

    val onDeskImage by remember(key1 = positionData.second) {
        derivedStateOf { positionData.first.position.value == CarryPositionType.OnDesk }
    }

    val inHandImage by remember(key1 = positionData.second) {
        derivedStateOf { positionData.first.position.value == CarryPositionType.InHand }
    }

    val nearHeadImage by remember(key1 = positionData.second) {
        derivedStateOf { positionData.first.position.value == CarryPositionType.NearHead }
    }

    val shirtPocketImage by remember(key1 = positionData.second) {
        derivedStateOf { positionData.first.position.value == CarryPositionType.ShirtPocket }
    }

    val trousersPocketImage by remember(key1 = positionData.second) {
        derivedStateOf { positionData.first.position.value == CarryPositionType.TrousersPocket }
    }

    val armSwingImage by remember(key1 = positionData.second) {
        derivedStateOf { positionData.first.position.value == CarryPositionType.ArmSwing }
    }

    val unknown by remember(key1 = positionData.second) {
        derivedStateOf {
            if (positionData.second != null) {
                positionData.first.position.value == CarryPositionType.Unknown
            } else false
        }
    }

    if (unknown) {
        Toast.makeText(context, "Carry position Unknown", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {


        Column(
            modifier = modifier.fillMaxWidth().padding(start = LocalDimensions.current.paddingLarge),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            CarryPositionItem(
                modifier = Modifier.weight(1f),
                isActive = inHandImage,
                id = R.drawable.carry_hand,
                label = "In Hand"
            )

            CarryPositionItem(
                modifier = Modifier.weight(1f),
                isActive = nearHeadImage,
                id = R.drawable.carry_head,
                label = "Near Head"
            )

            CarryPositionItem(
                modifier = Modifier.weight(1f),
                isActive = shirtPocketImage,
                id = R.drawable.carry_shirt,
                label = "Shirt Pocket"
            )

            CarryPositionItem(
                modifier = Modifier.weight(1f),
                isActive = trousersPocketImage,
                id = R.drawable.carry_trousers,
                label = "Trousers Pocket"
            )

            CarryPositionItem(
                modifier = Modifier.weight(1f),
                isActive = onDeskImage,
                id = R.drawable.carry_desk,
                label = "On Desk"
            )

            CarryPositionItem(
                modifier = Modifier.weight(1f),
                isActive = armSwingImage,
                id = R.drawable.carry_arm,
                label = "Arm Swing"
            )
        }

        if (positionData.second == null) {
            Text(
                style = MaterialTheme.typography.displayMedium,
                text = "Waiting data…"
            )
        }
    }
}
