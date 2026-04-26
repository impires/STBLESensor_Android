package com.st.source_localization;

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
public final class SourceLocalizationViewModel_Factory implements Factory<SourceLocalizationViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private SourceLocalizationViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public SourceLocalizationViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static SourceLocalizationViewModel_Factory create(
      Provider<BlueManager> blueManagerProvider, Provider<CoroutineScope> coroutineScopeProvider) {
    return new SourceLocalizationViewModel_Factory(blueManagerProvider, coroutineScopeProvider);
  }

  public static SourceLocalizationViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope) {
    return new SourceLocalizationViewModel(blueManager, coroutineScope);
  }
}
