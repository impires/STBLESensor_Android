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
fun ActivityRecognitionMotionAPD_MLCContent(
    modifier: Modifier,
    activityData: Pair<ActivityInfo, Long?>
) {

    val adultNotInCarImage by remember(key1 = activityData.second) {
        derivedStateOf { activityData.first.activity.value == ActivityType.NoActivity }
    }


    val adultInCarImage by remember(key1 = activityData.second) {
        derivedStateOf { activityData.first.activity.value == ActivityType.AdultInCar }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = LocalDimensions.current.paddingLarge,
                top = LocalDimensions.current.paddingLarge
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        ActivityRecognitionItem(
            //modifier =  Modifier.weight(1f),
            isActive = adultNotInCarImage,
            id = R.drawable.activity_adult_not_in_car,
            label = "Adult not in car"
        )

        ActivityRecognitionItem(
            //modifier = Modifier.weight(1f),
            isActive = adultInCarImage,
            id = R.drawable.activity_adult_in_car,
            label = "Adult in car"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoAdultInCar() {
    BlueMSTheme {
        ActivityRecognitionMotionAPD_MLCContent(
            modifier = Modifier,
            activityData = Pair(
                ActivityInfo(
                    activity = FeatureField(
                        value = ActivityType.AdultInCar,
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

