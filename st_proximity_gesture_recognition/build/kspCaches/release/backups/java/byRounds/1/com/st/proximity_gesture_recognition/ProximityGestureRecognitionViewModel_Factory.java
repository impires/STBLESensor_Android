package com.st.proximity_gesture_recognition;

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
public final class ProximityGestureRecognitionViewModel_Factory implements Factory<ProximityGestureRecognitionViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private ProximityGestureRecognitionViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public ProximityGestureRecognitionViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static ProximityGestureRecognitionViewModel_Factory create(
      Provider<BlueManager> blueManagerProvider, Provider<CoroutineScope> coroutineScopeProvider) {
    return new ProximityGestureRecognitionViewModel_Factory(blueManagerProvider, coroutineScopeProvider);
  }

  public static ProximityGestureRecognitionViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope) {
    return new ProximityGestureRecognitionViewModel(blueManager, coroutineScope);
  }
}
