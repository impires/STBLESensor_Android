package com.st.ext_config.ui.fw_upgrade;

import android.content.Context;
import com.st.blue_sdk.BlueManager;
import com.st.ext_config.download.DownloadAPI;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class FwUpgradeViewModel_Factory implements Factory<FwUpgradeViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<DownloadAPI> downloadAPIProvider;

  private final Provider<Context> contextProvider;

  private FwUpgradeViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<DownloadAPI> downloadAPIProvider, Provider<Context> contextProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.downloadAPIProvider = downloadAPIProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public FwUpgradeViewModel get() {
    return newInstance(blueManagerProvider.get(), downloadAPIProvider.get(), contextProvider.get());
  }

  public static FwUpgradeViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<DownloadAPI> downloadAPIProvider, Provider<Context> contextProvider) {
    return new FwUpgradeViewModel_Factory(blueManagerProvider, downloadAPIProvider, contextProvider);
  }

  public static FwUpgradeViewModel newInstance(BlueManager blueManager, DownloadAPI downloadAPI,
      Context context) {
    return new FwUpgradeViewModel(blueManager, downloadAPI, context);
  }
}
