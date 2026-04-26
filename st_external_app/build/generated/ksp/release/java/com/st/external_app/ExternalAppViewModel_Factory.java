package com.st.external_app;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ExternalAppViewModel_Factory implements Factory<ExternalAppViewModel> {
  @Override
  public ExternalAppViewModel get() {
    return newInstance();
  }

  public static ExternalAppViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ExternalAppViewModel newInstance() {
    return new ExternalAppViewModel();
  }

  private static final class InstanceHolder {
    static final ExternalAppViewModel_Factory INSTANCE = new ExternalAppViewModel_Factory();
  }
}
