/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
package com.st.working_in_progress

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class WorkingInProgressViewModel
@Inject internal constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val packageManager = context.packageManager

    fun openGooglePlayConsole(context: Context) {
        Intent(Intent.ACTION_VIEW).also { intent ->
            intent.data =
                "https://play.google.com/store/apps/details?id=com.st.bluemsclassic".toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }
}
