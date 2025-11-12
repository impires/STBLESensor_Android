package com.st.external_app.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.st.external_app.ExternalAppMap
import com.st.external_app.ExternalAppViewModel
import com.st.external_app.R
import com.st.external_app.model.ExternalAppDetailType
import com.st.external_app.model.ExternalAppType
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PreviewBlueMSTheme
import com.st.ui.theme.PrimaryBlue
import com.st.ui.theme.SecondaryBlue

@Composable
fun ExternalAppDemoContent(
    modifier: Modifier,
    viewModel: ExternalAppViewModel? = null,
    externalAppDetail: ExternalAppDetailType?
) {
    externalAppDetail?.let {

        val context = LocalContext.current

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = LocalDimensions.current.paddingMedium,
                    end = LocalDimensions.current.paddingMedium
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {


            Text(
                externalAppDetail.appTitle,
                textAlign = TextAlign.Center,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                modifier = Modifier.padding(top = LocalDimensions.current.paddingSmall),
                textAlign = TextAlign.Center,
                text = externalAppDetail.appShortDescription,
                style = MaterialTheme.typography.bodyMedium
            )

            Icon(
                modifier = Modifier
                    .padding(top = LocalDimensions.current.paddingMedium)
                    .size(size = LocalDimensions.current.iconMedium)
                    .background(
                        SecondaryBlue, shape = MaterialTheme.shapes.medium
                    ),
                tint = PrimaryBlue,
                painter = painterResource(id = externalAppDetail.appIcon),
                contentDescription = null
            )

            Text(
                modifier = Modifier.padding(top = LocalDimensions.current.paddingMedium),
                text = externalAppDetail.appLongDescription,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
            )

            Icon(
                modifier = Modifier
                    .size(size = LocalDimensions.current.iconLarge)
                    .clickable {
                        viewModel?.linkToGoogleStoreApp(context, externalAppDetail.appLink)
                    },
                painter = painterResource(id = R.drawable.getitongoogleplay_badge_web_color_english_01),
                tint = Color.Unspecified,
                contentDescription = null
            )
        }
    }
}

/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun ExternalAppDemoContentPreview() {
    PreviewBlueMSTheme {
        ExternalAppDemoContent(
            modifier = Modifier.fillMaxWidth(),
            externalAppDetail = ExternalAppMap[ExternalAppType.AIOTCRAFT]
        )
    }
}