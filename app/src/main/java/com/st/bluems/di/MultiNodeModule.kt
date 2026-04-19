package com.st.bluems.di

import com.st.bluems.StartLoggingUseCase
import com.st.bluems.StartLoggingUseCaseImpl
import com.st.bluems.StopLoggingUseCase
import com.st.bluems.StopLoggingUseCaseImpl
import com.st.bluems.multinode.MultiNodeRepository
import com.st.bluems.multinode.MultiNodeRepositoryImpl
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
}