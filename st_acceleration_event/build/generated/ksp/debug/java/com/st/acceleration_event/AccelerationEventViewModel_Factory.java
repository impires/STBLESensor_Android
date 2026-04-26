package com.st.acceleration_event;

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
public final class AccelerationEventViewModel_Factory implements Factory<AccelerationEventViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private AccelerationEventViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public AccelerationEventViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static AccelerationEventViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    return new AccelerationEventViewModel_Factory(blueManagerProvider, coroutineScopeProvider);
  }

  public static AccelerationEventViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope) {
    return new AccelerationEventViewModel(blueManager, coroutineScope);
  }
}
