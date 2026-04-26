package com.st.binary_content;

import android.app.Application;
import com.st.blue_sdk.BlueManager;
import com.st.preferences.StPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class BinaryContentViewModel_Factory implements Factory<BinaryContentViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<StPreferences> stPreferencesProvider;

  private final Provider<Application> applicationProvider;

  private BinaryContentViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<StPreferences> stPreferencesProvider, Provider<Application> applicationProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.stPreferencesProvider = stPreferencesProvider;
    this.applicationProvider = applicationProvider;
  }

  @Override
  public BinaryContentViewModel get() {
    return newInstance(blueManagerProvider.get(), stPreferencesProvider.get(), applicationProvider.get());
  }

  public static BinaryContentViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<StPreferences> stPreferencesProvider, Provider<Application> applicationProvider) {
    return new BinaryContentViewModel_Factory(blueManagerProvider, stPreferencesProvider, applicationProvider);
  }

  public static BinaryContentViewModel newInstance(BlueManager blueManager,
      StPreferences stPreferences, Application application) {
    return new BinaryContentViewModel(blueManager, stPreferences, application);
  }
}
