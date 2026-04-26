package com.st.switch_demo;

import com.st.blue_sdk.BlueManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata
@QualifierMetadata
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
public final class SwitchDemoViewModel_Factory implements Factory<SwitchDemoViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private SwitchDemoViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public SwitchDemoViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static SwitchDemoViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    return new SwitchDemoViewModel_Factory(blueManagerProvider, coroutineScopeProvider);
  }

  public static SwitchDemoViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope) {
    return new SwitchDemoViewModel(blueManager, coroutineScope);
  }
}
