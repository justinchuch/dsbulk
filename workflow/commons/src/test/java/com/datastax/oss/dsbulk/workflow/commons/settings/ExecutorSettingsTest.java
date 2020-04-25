/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.dsbulk.workflow.commons.settings;

import static com.datastax.oss.dsbulk.tests.assertions.TestAssertions.assertThat;
import static com.datastax.oss.dsbulk.tests.utils.ReflectionUtils.getInternalState;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.datastax.dse.driver.api.core.DseProtocolVersion;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.shaded.guava.common.util.concurrent.RateLimiter;
import com.datastax.oss.dsbulk.executor.api.reader.ReactiveBulkReader;
import com.datastax.oss.dsbulk.executor.api.writer.ReactiveBulkWriter;
import com.datastax.oss.dsbulk.executor.reactor.ContinuousReactorBulkExecutor;
import com.datastax.oss.dsbulk.executor.reactor.DefaultReactorBulkExecutor;
import com.datastax.oss.dsbulk.tests.driver.DriverUtils;
import com.datastax.oss.dsbulk.tests.logging.LogCapture;
import com.datastax.oss.dsbulk.tests.logging.LogInterceptingExtension;
import com.datastax.oss.dsbulk.tests.logging.LogInterceptor;
import com.datastax.oss.dsbulk.tests.utils.TestConfigUtils;
import com.typesafe.config.Config;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LogInterceptingExtension.class)
class ExecutorSettingsTest {

  private CqlSession session;

  @BeforeEach
  void setUp() {
    session = DriverUtils.mockSession();
  }

  @Test
  void should_create_non_continuous_executor_when_write_workflow() {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor");
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkWriter executor = settings.newWriteExecutor(session, null);
    assertThat(executor).isNotNull().isInstanceOf(DefaultReactorBulkExecutor.class);
  }

  @Test
  void should_create_non_continuous_executor_when_read_workflow_and_session_not_dse(
      @LogCapture LogInterceptor logs) {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor");
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    assertThat(executor).isNotNull().isInstanceOf(DefaultReactorBulkExecutor.class);
    assertThat(logs)
        .hasMessageContaining(
            "Continuous paging is not available, read performance will not be optimal");
  }

  @Test
  void should_create_non_continuous_executor_when_read_workflow_and_wrong_CL(
      @LogCapture LogInterceptor logs) {
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("TWO");
    when(session.getContext().getProtocolVersion()).thenReturn(DseProtocolVersion.DSE_V1);
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor");
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    assertThat(executor).isNotNull().isInstanceOf(DefaultReactorBulkExecutor.class);
    assertThat(logs)
        .hasMessageContaining(
            "Continuous paging is not available, read performance will not be optimal");
  }

  @Test
  void should_create_continuous_executor_when_read_workflow_and_session_dse() {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor");
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("LOCAL_ONE");
    when(session.getContext().getProtocolVersion()).thenReturn(DseProtocolVersion.DSE_V1);
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    assertThat(executor).isNotNull().isInstanceOf(ContinuousReactorBulkExecutor.class);
  }

  @Test
  void should_create_non_continuous_executor_when_read_workflow_and_not_enabled() {
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("ONE");
    when(session.getContext().getProtocolVersion()).thenReturn(DseProtocolVersion.DSE_V1);
    Config config =
        TestConfigUtils.createTestConfig(
            "dsbulk.executor", "continuousPagingOptions.enabled", false);
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    assertThat(executor).isNotNull().isInstanceOf(DefaultReactorBulkExecutor.class);
  }

  @Test
  void should_create_non_continuous_executor_when_read_workflow_and_search_query(
      @LogCapture LogInterceptor logs) {
    Config config =
        TestConfigUtils.createTestConfig(
            "dsbulk.executor", "continuousPagingOptions.enabled", false);
    when(session.getContext().getProtocolVersion()).thenReturn(DseProtocolVersion.DSE_V1);
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, true);
    assertThat(executor).isNotNull().isInstanceOf(DefaultReactorBulkExecutor.class);
    assertThat(logs)
        .hasMessageContaining(
            "Continuous paging is enabled but is not compatible with search queries; disabling");
  }

  @Test
  void should_enable_maxPerSecond() {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor", "maxPerSecond", 100);
    ExecutorSettings settings = new ExecutorSettings(config);
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("ONE");
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    assertThat(((RateLimiter) getInternalState(executor, "rateLimiter")).getRate()).isEqualTo(100);
  }

  @Test
  void should_disable_maxPerSecond() {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor", "maxPerSecond", 0);
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("ONE");
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    assertThat(getInternalState(executor, "rateLimiter")).isNull();
  }

  @Test
  void should_throw_exception_when_maxPerSecond_not_a_number() {
    Config config =
        TestConfigUtils.createTestConfig("dsbulk.executor", "maxPerSecond", "NotANumber");
    ExecutorSettings settings = new ExecutorSettings(config);
    assertThatThrownBy(settings::init)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "Invalid value for dsbulk.executor.maxPerSecond, expecting NUMBER, got STRING");
  }

  @Test
  void should_enable_maxInFlight() {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor", "maxInFlight", 100);
    ExecutorSettings settings = new ExecutorSettings(config);
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("ONE");
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    Semaphore maxConcurrentRequests =
        (Semaphore) getInternalState(executor, "maxConcurrentRequests");
    assertThat(maxConcurrentRequests.availablePermits()).isEqualTo(100);
  }

  @Test
  void should_disable_maxInFlight() {
    Config config = TestConfigUtils.createTestConfig("dsbulk.executor", "maxInFlight", 0);
    ExecutorSettings settings = new ExecutorSettings(config);
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("ONE");
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    Semaphore maxConcurrentRequests =
        (Semaphore) getInternalState(executor, "maxConcurrentRequests");
    assertThat(maxConcurrentRequests).isNull();
  }

  @Test
  void should_throw_exception_when_maxInFlight_not_a_number() {
    Config config =
        TestConfigUtils.createTestConfig("dsbulk.executor", "maxInFlight", "NotANumber");
    ExecutorSettings settings = new ExecutorSettings(config);
    assertThatThrownBy(settings::init)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "Invalid value for dsbulk.executor.maxInFlight, expecting NUMBER, got STRING");
  }

  @Test
  void should_enable_maxConcurrentQueries() {
    Config config =
        TestConfigUtils.createTestConfig(
            "dsbulk.executor", "continuousPaging.maxConcurrentQueries", 100);
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("LOCAL_ONE");
    when(session.getContext().getProtocolVersion()).thenReturn(DseProtocolVersion.DSE_V1);
    ExecutorSettings settings = new ExecutorSettings(config);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    Semaphore maxConcurrentQueries = (Semaphore) getInternalState(executor, "maxConcurrentQueries");
    assertThat(maxConcurrentQueries.availablePermits()).isEqualTo(100);
  }

  @Test
  void should_disable_maxConcurrentQueries() {
    Config config =
        TestConfigUtils.createTestConfig(
            "dsbulk.executor", "continuousPaging.maxConcurrentQueries", 0);
    ExecutorSettings settings = new ExecutorSettings(config);
    DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();
    when(profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).thenReturn("ONE");
    when(session.getContext().getProtocolVersion()).thenReturn(DseProtocolVersion.DSE_V1);
    settings.init();
    ReactiveBulkReader executor = settings.newReadExecutor(session, null, false);
    Semaphore maxConcurrentQueries = (Semaphore) getInternalState(executor, "maxConcurrentQueries");
    assertThat(maxConcurrentQueries).isNull();
  }

  @Test
  void should_throw_exception_when_maxConcurrentQueries_not_a_number() {
    Config config =
        TestConfigUtils.createTestConfig(
            "dsbulk.executor", "continuousPaging.maxConcurrentQueries", "NotANumber");
    ExecutorSettings settings = new ExecutorSettings(config);
    assertThatThrownBy(settings::init)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "Invalid value for dsbulk.executor.continuousPaging.maxConcurrentQueries, expecting NUMBER, got STRING");
  }
}
