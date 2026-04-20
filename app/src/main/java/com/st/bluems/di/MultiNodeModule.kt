package com.st.bluems.di

import com.st.bluems.AcquisitionServiceControllerImpl
import com.st.multinode.AcquisitionServiceController
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
}
