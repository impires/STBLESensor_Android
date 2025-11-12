package com.st.activity_recognition.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.st.activity_recognition.R
import com.st.blue_sdk.features.FeatureField
import com.st.blue_sdk.features.activity.ActivityInfo
import com.st.blue_sdk.features.activity.ActivityType
import com.st.ui.theme.BlueMSTheme
import com.st.ui.theme.LocalDimensions
import java.util.Date

@Composable
fun ActivityRecognitionMotionIGNContent(
    modifier: Modifier,
    activityData: Pair<ActivityInfo, Long?>
) {

    val stationaryImage by remember(key1 = activityData.second) {
        derivedStateOf { activityData.first.activity.value == ActivityType.Stationary }
    }

    val walkingImage by remember(key1 = activityData.second) {
        derivedStateOf { activityData.first.activity.value == ActivityType.Walking }
    }


    val joggingImage by remember(key1 = activityData.second) {
        derivedStateOf { activityData.first.activity.value == ActivityType.Jogging }
    }


    val stairsImage by remember(key1 = activityData.second) {
        derivedStateOf { activityData.first.activity.value == ActivityType.Stairs }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(start = LocalDimensions.current.paddingLarge),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        ActivityRecognitionItem(
            modifier = Modifier.weight(1f),
            isActive = stationaryImage,
            id = R.drawable.activity_stationary,
            label = "Inactive"
        )

        ActivityRecognitionItem(
            modifier = Modifier.weight(1f),
            isActive = walkingImage,
            id = R.drawable.activity_walking,
            label = "Walking"
        )

        ActivityRecognitionItem(
            modifier = Modifier.weight(1f),
            isActive = joggingImage,
            id = R.drawable.activity_jogging,
            label = "Jogging"
        )

        ActivityRecognitionItem(
            modifier = Modifier.weight(1f),
            isActive = stairsImage,
            id = R.drawable.activity_stairs,
            label = "Go up the stairs"
        )
    }

}

@Preview(showBackground = true)
@Composable
private fun ShowWalking() {
    BlueMSTheme {
        ActivityRecognitionMotionIGNContent(
            modifier = Modifier,
            activityData = Pair(
                ActivityInfo(
                    activity = FeatureField(
                        value = ActivityType.Walking,
                        name = "Activity"
                    ),
                    algorithm = FeatureField(
                        value = ActivityInfo.ALGORITHM_NOT_DEFINED,
                        name = "Algorithm"
                    ),
                    date = FeatureField(
                        value = Date(),
                        name = "Date"
                    )
                ), 1L
            )
        )
    }
}