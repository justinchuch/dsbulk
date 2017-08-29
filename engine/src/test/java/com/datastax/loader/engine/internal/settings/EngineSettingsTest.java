/*
 * Copyright (C) 2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.loader.engine.internal.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.loader.commons.config.DefaultLoaderConfig;
import com.datastax.loader.commons.config.LoaderConfig;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

/** */
public class EngineSettingsTest {

  @Test
  public void should_create_default_engine_settings() throws Exception {
    LoaderConfig config =
        new DefaultLoaderConfig(ConfigFactory.load().getConfig("datastax-loader.engine"));
    EngineSettings settings = new EngineSettings(config);
    assertThat(settings.getMaxMappingThreads())
        .isEqualTo(Runtime.getRuntime().availableProcessors());
    assertThat(settings.getMaxConcurrentReads()).isEqualTo(4);
  }

  @Test
  public void should_create_custom_engine_settings() throws Exception {
    LoaderConfig config =
        new DefaultLoaderConfig(
            ConfigFactory.parseString("maxMappingThreads = 8C, maxConcurrentReads = 4C")
                .withFallback(ConfigFactory.load().getConfig("datastax-loader.engine")));
    EngineSettings settings = new EngineSettings(config);
    assertThat(settings.getMaxMappingThreads())
        .isEqualTo(Runtime.getRuntime().availableProcessors() * 8);
    assertThat(settings.getMaxConcurrentReads())
        .isEqualTo(Runtime.getRuntime().availableProcessors() * 4);
  }
}