package com.st.ext_config.ui.fw_download;

import com.st.blue_sdk.BlueManager;
import com.st.blue_sdk.board_catalog.BoardCatalogRepo;
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
public final class FwDownloadViewModel_Factory implements Factory<FwDownloadViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private final Provider<StPreferences> stPreferencesProvider;

  private final Provider<BoardCatalogRepo> catalogProvider;

  private FwDownloadViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider,
      Provider<StPreferences> stPreferencesProvider, Provider<BoardCatalogRepo> catalogProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
    this.stPreferencesProvider = stPreferencesProvider;
    this.catalogProvider = catalogProvider;
  }

  @Override
  public FwDownloadViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get(), stPreferencesProvider.get(), catalogProvider.get());
  }

  public static FwDownloadViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider,
      Provider<StPreferences> stPreferencesProvider, Provider<BoardCatalogRepo> catalogProvider) {
    return new FwDownloadViewModel_Factory(blueManagerProvider, coroutineScopeProvider, stPreferencesProvider, catalogProvider);
  }

  public static FwDownloadViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope, StPreferences stPreferences, BoardCatalogRepo catalog) {
    return new FwDownloadViewModel(blueManager, coroutineScope, stPreferences, catalog);
  }
}
