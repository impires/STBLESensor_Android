package com.st.login.impl;

import android.content.Context;
import com.st.login.STLoginConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.st.login.di.LoginConfig"
})
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class StLoginManagerImpl_Factory implements Factory<StLoginManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<STLoginConfig> stLoginConfigProvider;

  private StLoginManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<STLoginConfig> stLoginConfigProvider) {
    this.contextProvider = contextProvider;
    this.stLoginConfigProvider = stLoginConfigProvider;
  }

  @Override
  public StLoginManagerImpl get() {
    return newInstance(contextProvider.get(), stLoginConfigProvider.get());
  }

  public static StLoginManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<STLoginConfig> stLoginConfigProvider) {
    return new StLoginManagerImpl_Factory(contextProvider, stLoginConfigProvider);
  }

  public static StLoginManagerImpl newInstance(Context context, STLoginConfig stLoginConfig) {
    return new StLoginManagerImpl(context, stLoginConfig);
  }
}
