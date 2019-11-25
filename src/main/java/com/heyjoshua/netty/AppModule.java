package com.heyjoshua.netty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import java.time.Duration;

public class AppModule extends AbstractModule {
  @Provides
  @Singleton
  MeterRegistry provideMetricsCollector() {
    MeterRegistry registry =
        new LoggingMeterRegistry(
            new LoggingRegistryConfig() {
              @Override
              public String get(String key) {
                return null;
              }

              @Override
              public Duration step() {
                return Duration.ofSeconds(10);
              }
            },
            Clock.SYSTEM);

    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);

    return registry;
  }
}
