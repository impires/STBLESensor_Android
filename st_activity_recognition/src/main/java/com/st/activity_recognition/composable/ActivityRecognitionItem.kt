package com.st.activity_recognition.composable

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.NotActiveColor
import com.st.ui.theme.PrimaryBlue
import com.st.ui.theme.Shapes


@Composable
fun ActivityRecognitionItem(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    @DrawableRes id: Int,
    label: String,
) {
    Row(
        modifier = modifier
            .padding(
                start = LocalDimensions.current.paddingLarge,
                end = LocalDimensions.current.paddingLarge,
                top = LocalDimensions.current.paddingNormal,
                bottom = LocalDimensions.current.paddingNormal
            )
            //.fillMaxHeight()
            .graphicsLayer(
                alpha = if (isActive) {
                    1f
                } else {
                    0.3f
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .sizeIn(maxHeight = LocalDimensions.current.imageMedium),
            shape = Shapes.small,
            color = NotActiveColor
        ) {

            Icon(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(LocalDimensions.current.paddingSmall),
                painter = painterResource(id),
                tint = PrimaryBlue,
                contentDescription = null
            )
        }

        Text(
            modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            text = label.uppercase(),
        )
    }
}