package com.st.ext_config.download;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import retrofit2.Retrofit;

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
public final class ExtConfigModule_ProvideDownloadApiFactory implements Factory<DownloadAPI> {
  private final Provider<Retrofit> retrofitClientProvider;

  private ExtConfigModule_ProvideDownloadApiFactory(Provider<Retrofit> retrofitClientProvider) {
    this.retrofitClientProvider = retrofitClientProvider;
  }

  @Override
  public DownloadAPI get() {
    return provideDownloadApi(retrofitClientProvider.get());
  }

  public static ExtConfigModule_ProvideDownloadApiFactory create(
      Provider<Retrofit> retrofitClientProvider) {
    return new ExtConfigModule_ProvideDownloadApiFactory(retrofitClientProvider);
  }

  public static DownloadAPI provideDownloadApi(Retrofit retrofitClient) {
    return Preconditions.checkNotNullFromProvides(ExtConfigModule.INSTANCE.provideDownloadApi(retrofitClient));
  }
}
