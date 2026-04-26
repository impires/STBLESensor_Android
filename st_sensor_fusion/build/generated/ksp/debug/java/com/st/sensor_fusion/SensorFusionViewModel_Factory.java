package com.st.sensor_fusion;

import com.st.blue_sdk.BlueManager;
import com.st.blue_sdk.services.calibration.CalibrationService;
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
public final class SensorFusionViewModel_Factory implements Factory<SensorFusionViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CalibrationService> calibrationServiceProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private SensorFusionViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CalibrationService> calibrationServiceProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.calibrationServiceProvider = calibrationServiceProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public SensorFusionViewModel get() {
    return newInstance(blueManagerProvider.get(), calibrationServiceProvider.get(), coroutineScopeProvider.get());
  }

  public static SensorFusionViewModel_Factory create(Provider<BlueManager> blueManagerProvider,
      Provider<CalibrationService> calibrationServiceProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    return new SensorFusionViewModel_Factory(blueManagerProvider, calibrationServiceProvider, coroutineScopeProvider);
  }

  public static SensorFusionViewModel newInstance(BlueManager blueManager,
      CalibrationService calibrationService, CoroutineScope coroutineScope) {
    return new SensorFusionViewModel(blueManager, calibrationService, coroutineScope);
  }
}
