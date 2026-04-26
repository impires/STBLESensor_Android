package com.st.plot;

import com.st.blue_sdk.BlueManager;
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
public final class PlotViewModel_Factory implements Factory<PlotViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private PlotViewModel_Factory(Provider<BlueManager> blueManagerProvider) {
    this.blueManagerProvider = blueManagerProvider;
  }

  @Override
  public PlotViewModel get() {
    return newInstance(blueManagerProvider.get());
  }

  public static PlotViewModel_Factory create(Provider<BlueManager> blueManagerProvider) {
    return new PlotViewModel_Factory(blueManagerProvider);
  }

  public static PlotViewModel newInstance(BlueManager blueManager) {
    return new PlotViewModel(blueManager);
  }
}
