package com.st.bluems.di

import com.st.bluems.BuildConfig
import com.st.bluems.util.ENVIRONMENT
import com.st.login.STLoginConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import androidx.core.net.toUri

@InstallIn(SingletonComponent::class)
@Module(includes = [BlueMsModule.WithProvides::class])
abstract class BlueMsModule {

    @Module
    @InstallIn(SingletonComponent::class)
    object WithProvides {

        @Provides
        @IntoMap
        @StringKey("stLoginConfig")
        fun provideSTLoginConfig(): STLoginConfig {
            return STLoginConfig(
                "stblesensor://callback".toUri(),
                if (BuildConfig.VESPUCCI_ENVIRONMENT != ENVIRONMENT.DEV.name) {
                    com.st.login.R.raw.prod_auth_config_vespucci
                } else {
                    com.st.login.R.raw.dev_auth_config_vespucci
                },
                if (BuildConfig.VESPUCCI_ENVIRONMENT != ENVIRONMENT.DEV.name) {
                    "https://www.st.com/cas/logout?service=https%3A%2F%2Fstaiotcraft.st.com%2Fsvc%2Fwebtomobile%2Fstblesensor".toUri()
                } else {
                    "".toUri()
                },
                BuildConfig.VESPUCCI_ENVIRONMENT == ENVIRONMENT.PROD.name
            )
        }
    }
}