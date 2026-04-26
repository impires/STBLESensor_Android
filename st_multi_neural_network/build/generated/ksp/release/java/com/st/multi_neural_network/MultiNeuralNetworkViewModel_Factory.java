package com.st.multi_neural_network;

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
public final class MultiNeuralNetworkViewModel_Factory implements Factory<MultiNeuralNetworkViewModel> {
  private final Provider<BlueManager> blueManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  private MultiNeuralNetworkViewModel_Factory(Provider<BlueManager> blueManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.blueManagerProvider = blueManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public MultiNeuralNetworkViewModel get() {
    return newInstance(blueManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static MultiNeuralNetworkViewModel_Factory create(
      Provider<BlueManager> blueManagerProvider, Provider<CoroutineScope> coroutineScopeProvider) {
    return new MultiNeuralNetworkViewModel_Factory(blueManagerProvider, coroutineScopeProvider);
  }

  public static MultiNeuralNetworkViewModel newInstance(BlueManager blueManager,
      CoroutineScope coroutineScope) {
    return new MultiNeuralNetworkViewModel(blueManager, coroutineScope);
  }
}
