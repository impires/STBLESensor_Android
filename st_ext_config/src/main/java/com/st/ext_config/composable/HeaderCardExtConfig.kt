package com.st.ext_config.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.st.ui.theme.LocalDimensions

@Composable
fun HeaderCardExtConfig(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    isOpen: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = LocalDimensions.current.paddingNormal)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = LocalDimensions.current.paddingNormal),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Absolute.SpaceBetween
        ) {

            Icon(
                modifier = Modifier.size(size = LocalDimensions.current.iconNormal),
                imageVector = imageVector,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )

            Text(
                modifier = Modifier.padding(top = LocalDimensions.current.paddingSmall),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.68.sp,
                letterSpacing = 0.14.sp,
                color = MaterialTheme.colorScheme.primary,
                text = title
            )

            if (isOpen) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }

        if (isOpen) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = LocalDimensions.current.paddingNormal)
            )
        }
    }
}