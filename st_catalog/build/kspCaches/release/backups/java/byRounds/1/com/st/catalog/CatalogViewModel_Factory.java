package com.st.catalog;

import com.st.blue_sdk.BlueManager;
import com.st.core.api.ApplicationAnalyticsService;
import com.st.preferences.StPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.util.Set;
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
public final class CatalogViewModel_Factory implements Factory<CatalogViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<StPreferences> stPreferencesProvider;

  private final Provider<Set<ApplicationAnalyticsService>> appAnalyticsServiceProvider;

  private CatalogViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<StPreferences> stPreferencesProvider,
      Provider<Set<ApplicationAnalyticsService>> appAnalyticsServiceProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.stPreferencesProvider = stPreferencesProvider;
    this.appAnalyticsServiceProvider = appAnalyticsServiceProvider;
  }

  @Override
  public CatalogViewModel get() {
    return newInstance(blueManagerProvider.get(), stPreferencesProvider.get(), appAnalyticsServiceProvider.get());
  }

  public static CatalogViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<StPreferences> stPreferencesProvider,
      Provider<Set<ApplicationAnalyticsService>> appAnalyticsServiceProvider) {
    return new CatalogViewModel_Factory(blueManagerProvider, stPreferencesProvider, appAnalyticsServiceProvider);
  }

  public static CatalogViewModel newInstance(BlueManager blueManager, StPreferences stPreferences,
      Set<ApplicationAnalyticsService> appAnalyticsService) {
    return new CatalogViewModel(blueManager, stPreferences, appAnalyticsService);
  }
}
