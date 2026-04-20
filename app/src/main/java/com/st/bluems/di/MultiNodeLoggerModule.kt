package com.st.bluems.di

import com.st.blue_sdk.logger.Logger
import com.st.core.multinode.MultiNodeCsvFileLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MultiNodeLoggerModule {

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMultiNodeCsvLogger(
        impl: MultiNodeCsvFileLogger
    ): Logger
}