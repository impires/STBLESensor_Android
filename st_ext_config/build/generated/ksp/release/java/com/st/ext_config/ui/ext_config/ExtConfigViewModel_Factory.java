package com.st.ext_config.ui.ext_config;

import android.content.Context;
import com.st.blue_sdk.BlueManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ExtConfigViewModel_Factory implements Factory<ExtConfigViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private final Provider<Context> contextProvider;

  private ExtConfigViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider, Provider<Context> contextProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ExtConfigViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get(), contextProvider.get());
  }

  public static ExtConfigViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider, Provider<Context> contextProvider) {
    return new ExtConfigViewModel_Factory(blueManagerProvider, coroutineScopeProvider, contextProvider);
  }

  public static ExtConfigViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope, Context context) {
    return new ExtConfigViewModel(blueManager, coroutineScope, context);
  }
}
