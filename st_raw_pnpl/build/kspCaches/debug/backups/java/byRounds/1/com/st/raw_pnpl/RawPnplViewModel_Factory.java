package com.st.raw_pnpl;

import com.st.blue_sdk.BlueManager;
import com.st.preferences.StPreferences;
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
public final class RawPnplViewModel_Factory implements Factory<RawPnplViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<StPreferences> stPreferencesProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private RawPnplViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<StPreferences> stPreferencesProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.stPreferencesProvider = stPreferencesProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public RawPnplViewModel get() {
    return newInstance(blueManagerProvider.get(), stPreferencesProvider.get(), coroutineScopeProvider.get());
  }

  public static RawPnplViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<StPreferences> stPreferencesProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    return new RawPnplViewModel_Factory(blueManagerProvider, stPreferencesProvider, coroutineScopeProvider);
  }

  public static RawPnplViewModel newInstance(BlueManager blueManager, StPreferences stPreferences,
      CoroutineScope coroutineScope) {
    return new RawPnplViewModel(blueManager, stPreferences, coroutineScope);
  }
}
