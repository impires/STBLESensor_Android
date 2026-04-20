package com.st.multinode.di

import com.st.blue_sdk.logger.Logger
import com.st.multinode.MultiNodeCsvFileLogger
import com.st.multinode.data.MultiNodeRepository
import com.st.multinode.data.MultiNodeRepositoryImpl
import com.st.multinode.logging.StartLoggingUseCase
import com.st.multinode.logging.StartLoggingUseCaseImpl
import com.st.multinode.logging.StopLoggingUseCase
import com.st.multinode.logging.StopLoggingUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MultiNodeModule {

    @Binds
    @Singleton
    abstract fun bindMultiNodeRepository(
        impl: MultiNodeRepositoryImpl
    ): MultiNodeRepository

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMultiNodeCsvLogger(
        impl: MultiNodeCsvFileLogger
    ): Logger

    @Binds
    @Singleton
    abstract fun bindStartLoggingUseCase(
        impl: StartLoggingUseCaseImpl
    ): StartLoggingUseCase

    @Binds
    @Singleton
    abstract fun bindStopLoggingUseCase(
        impl: StopLoggingUseCaseImpl
    ): StopLoggingUseCase

}