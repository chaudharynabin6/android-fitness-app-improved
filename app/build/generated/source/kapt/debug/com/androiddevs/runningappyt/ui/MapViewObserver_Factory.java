package com.androiddevs.runningappyt.ui;

import android.os.Bundle;
import androidx.lifecycle.Lifecycle;

import com.androiddevs.runningappyt.observers.MapViewObserver;
import com.google.android.gms.maps.MapView;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes"
})
public final class MapViewObserver_Factory implements Factory<MapViewObserver> {
  private final Provider<Lifecycle> lifecycleProvider;

  private final Provider<MapView> mapViewProvider;

  private final Provider<Bundle> savedInstanceStateProvider;

  public MapViewObserver_Factory(Provider<Lifecycle> lifecycleProvider,
      Provider<MapView> mapViewProvider, Provider<Bundle> savedInstanceStateProvider) {
    this.lifecycleProvider = lifecycleProvider;
    this.mapViewProvider = mapViewProvider;
    this.savedInstanceStateProvider = savedInstanceStateProvider;
  }

  @Override
  public MapViewObserver get() {
    return newInstance(lifecycleProvider.get(), mapViewProvider.get(), savedInstanceStateProvider.get());
  }

  public static MapViewObserver_Factory create(Provider<Lifecycle> lifecycleProvider,
      Provider<MapView> mapViewProvider, Provider<Bundle> savedInstanceStateProvider) {
    return new MapViewObserver_Factory(lifecycleProvider, mapViewProvider, savedInstanceStateProvider);
  }

  public static MapViewObserver newInstance(Lifecycle lifecycle, MapView mapView,
      Bundle savedInstanceState) {
    return new MapViewObserver(lifecycle, mapView, savedInstanceState);
  }
}
