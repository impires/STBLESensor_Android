package com.st.flow_demo;

import com.st.blue_sdk.BlueManager;
import com.st.core.api.ApplicationAnalyticsService;
import com.st.ext_config.download.DownloadAPI;
import com.st.preferences.StPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.util.Set;
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
public final class FlowDemoViewModel_Factory implements Factory<FlowDemoViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private final Provider<StPreferences> stPreferencesProvider;

  private final Provider<DownloadAPI> downloadAPIProvider;

  private final Provider<Set<ApplicationAnalyticsService>> appAnalyticsServiceProvider;

  private FlowDemoViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider,
      Provider<StPreferences> stPreferencesProvider, Provider<DownloadAPI> downloadAPIProvider,
      Provider<Set<ApplicationAnalyticsService>> appAnalyticsServiceProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
    this.stPreferencesProvider = stPreferencesProvider;
    this.downloadAPIProvider = downloadAPIProvider;
    this.appAnalyticsServiceProvider = appAnalyticsServiceProvider;
  }

  @Override
  public FlowDemoViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get(), stPreferencesProvider.get(), downloadAPIProvider.get(), appAnalyticsServiceProvider.get());
  }

  public static FlowDemoViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider,
      Provider<StPreferences> stPreferencesProvider, Provider<DownloadAPI> downloadAPIProvider,
      Provider<Set<ApplicationAnalyticsService>> appAnalyticsServiceProvider) {
    return new FlowDemoViewModel_Factory(blueManagerProvider, coroutineScopeProvider, stPreferencesProvider, downloadAPIProvider, appAnalyticsServiceProvider);
  }

  public static FlowDemoViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope, StPreferences stPreferences, DownloadAPI downloadAPI,
      Set<ApplicationAnalyticsService> appAnalyticsService) {
    return new FlowDemoViewModel(blueManager, coroutineScope, stPreferences, downloadAPI, appAnalyticsService);
  }
}
