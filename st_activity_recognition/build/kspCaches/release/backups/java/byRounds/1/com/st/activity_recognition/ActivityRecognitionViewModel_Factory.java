package com.st.activity_recognition;

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
public final class ActivityRecognitionViewModel_Factory implements Factory<ActivityRecognitionViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private ActivityRecognitionViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public ActivityRecognitionViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static ActivityRecognitionViewModel_Factory create(
      Provider<BlueManager> blueManagerProvider, Provider<CoroutineScope> coroutineScopeProvider) {
    return new ActivityRecognitionViewModel_Factory(blueManagerProvider, coroutineScopeProvider);
  }

  public static ActivityRecognitionViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope) {
    return new ActivityRecognitionViewModel(blueManager, coroutineScope);
  }
}
