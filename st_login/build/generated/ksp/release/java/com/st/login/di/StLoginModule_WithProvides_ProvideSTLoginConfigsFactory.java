package com.st.login.di;

import com.st.login.STLoginConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.util.Map;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("com.st.login.di.LoginConfig")
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
public final class StLoginModule_WithProvides_ProvideSTLoginConfigsFactory implements Factory<STLoginConfig> {
  private final Provider<Map<String, STLoginConfig>> provideSTLoginConfigsProvider;

  private StLoginModule_WithProvides_ProvideSTLoginConfigsFactory(
      Provider<Map<String, STLoginConfig>> provideSTLoginConfigsProvider) {
    this.provideSTLoginConfigsProvider = provideSTLoginConfigsProvider;
  }

  @Override
  public STLoginConfig get() {
    return provideSTLoginConfigs(provideSTLoginConfigsProvider.get());
  }

  public static StLoginModule_WithProvides_ProvideSTLoginConfigsFactory create(
      Provider<Map<String, STLoginConfig>> provideSTLoginConfigsProvider) {
    return new StLoginModule_WithProvides_ProvideSTLoginConfigsFactory(provideSTLoginConfigsProvider);
  }

  public static STLoginConfig provideSTLoginConfigs(
      Map<String, STLoginConfig> provideSTLoginConfigs) {
    return Preconditions.checkNotNullFromProvides(StLoginModule.WithProvides.INSTANCE.provideSTLoginConfigs(provideSTLoginConfigs));
  }
}
