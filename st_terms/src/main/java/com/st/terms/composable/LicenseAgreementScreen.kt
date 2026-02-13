/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
package com.st.terms.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.st.terms.R
import com.st.ui.composables.BlueMsButton
import com.st.ui.composables.BlueMsButtonOutlined
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PreviewBlueMSTheme
import com.st.ui.theme.Grey6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseAgreementScreen(
    modifier: Modifier = Modifier,
    onLicenseAgree: () -> Unit = { /** NOOP **/ }
) {
    val openDialog = remember { mutableStateOf(value = false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {

                    Text(
                        text = "License",
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        letterSpacing = 0.15.sp,
                        fontWeight = FontWeight.Bold
                    )
                })
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues)
                .padding(LocalDimensions.current.paddingNormal)
                .verticalScroll(state = rememberScrollState())
        ) {
            Text(
                modifier = Modifier.fillMaxWidth().padding(top=LocalDimensions.current.paddingNormal),
                text = stringResource(id = R.string.st_terms_title),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = LocalDimensions.current.paddingMedium),
                text = stringResource(id = R.string.st_terms_description),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = LocalDimensions.current.paddingNormal),
                text = stringResource(id = R.string.st_terms_licenseAgreement),
                color = Grey6,
                style = MaterialTheme.typography.bodySmall
            )


            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = LocalDimensions.current.paddingLarge),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BlueMsButtonOutlined(
                    modifier = Modifier.weight(weight = 1f),
                    text = stringResource(id = R.string.st_terms_doNotAgree),
                    onClick = {
                        openDialog.value = true
                    }
                )

                BlueMsButton(
                    modifier = Modifier.weight(weight = 1f),
                    text = stringResource(id = R.string.st_terms_agree),
                    onClick = onLicenseAgree
                )
            }

            if (openDialog.value) {
                AlertDialog(
                    title = {
                        Text(text = stringResource(id = R.string.st_terms_title))
                    },
                    text = {
                        Text(text = stringResource(id = R.string.st_terms_missingAgreement))
                    },
                    onDismissRequest = { /** NOOP **/ },
                    confirmButton = { /** NOOP **/ },
                    dismissButton = {
                        TextButton(onClick = { openDialog.value = false }) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    }
                )
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun LicenseAgreementScreenPreview() {
    PreviewBlueMSTheme {
        LicenseAgreementScreen()
    }
}
