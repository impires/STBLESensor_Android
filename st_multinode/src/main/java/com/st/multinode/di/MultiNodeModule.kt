package com.st.multinode.di

import com.st.multinode.AcquisitionServiceController
import com.st.multinode.AcquisitionServiceControllerImpl
import com.st.multinode.data.MultiNodeRepository
import com.st.multinode.data.MultiNodeRepositoryImpl
import com.st.multinode.logging.BoardSdLoggingTransport
import com.st.multinode.logging.BoardSdLoggingTransportImpl
import com.st.multinode.logging.StartLoggingUseCase
import com.st.multinode.logging.StartLoggingUseCaseImpl
import com.st.multinode.logging.StopLoggingUseCase
import com.st.multinode.logging.StopLoggingUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MultiNodeModule {

    @Binds
    @Singleton
    abstract fun bindAcquisitionServiceController(
        impl: AcquisitionServiceControllerImpl
    ): AcquisitionServiceController

    @Binds
    @Singleton
    abstract fun bindMultiNodeRepository(
        impl: MultiNodeRepositoryImpl
    ): MultiNodeRepository

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

    @Binds
    @Singleton
    abstract fun bindBoardSdLoggingTransport(
        impl: BoardSdLoggingTransportImpl
    ): BoardSdLoggingTransport
}