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

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.dsbulk.commons.config.ConfigUtils;
import com.datastax.oss.dsbulk.executor.api.batch.StatementBatcher;
import com.datastax.oss.dsbulk.executor.reactor.batch.ReactorStatementBatcher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchSettings {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchSettings.class);

  private enum BatchMode {
    DISABLED {
      @Override
      StatementBatcher.BatchMode asStatementBatcherMode() {
        throw new IllegalStateException("Batching is disabled");
      }
    },
    PARTITION_KEY {
      @Override
      StatementBatcher.BatchMode asStatementBatcherMode() {
        return StatementBatcher.BatchMode.PARTITION_KEY;
      }
    },
    REPLICA_SET {
      @Override
      StatementBatcher.BatchMode asStatementBatcherMode() {
        return StatementBatcher.BatchMode.REPLICA_SET;
      }
    };

    abstract StatementBatcher.BatchMode asStatementBatcherMode();
  }

  private static final String MODE = "mode";
  private static final String MAX_BATCH_SIZE = "maxBatchSize";
  private static final String MAX_SIZE_IN_BYTES = "maxSizeInBytes";
  private static final String MAX_BATCH_STATEMENTS = "maxBatchStatements";
  private static final String BUFFER_SIZE = "bufferSize";

  private final Config config;

  private BatchMode mode;
  private long maxSizeInBytes;
  private int maxBatchStatements;
  private int bufferSize;

  BatchSettings(Config config) {
    this.config = config;
  }

  public void init() {
    try {
      mode = config.getEnum(BatchMode.class, MODE);
      maxSizeInBytes = config.getLong(MAX_SIZE_IN_BYTES);

      if (config.hasPath(MAX_BATCH_SIZE)) {

        if (config.hasPath(MAX_BATCH_STATEMENTS)) {
          throw new IllegalArgumentException(
              "Settings batch.maxBatchStatements and batch.maxBatchSize "
                  + "cannot be both defined; "
                  + "consider using batch.maxBatchStatements exclusively, "
                  + "because batch.maxBatchSize is deprecated.");
        } else {
          LOGGER.warn(
              "Setting batch.maxBatchSize is deprecated, "
                  + "please use batch.maxBatchStatements instead.");
          maxBatchStatements = config.getInt(MAX_BATCH_SIZE);
        }

      } else {
        maxBatchStatements = config.getInt(MAX_BATCH_STATEMENTS);
      }

      if (maxSizeInBytes <= 0 && maxBatchStatements <= 0) {
        throw new IllegalArgumentException(
            "At least one of batch.maxSizeInBytes or batch.maxBatchStatements must be positive. "
                + "See settings.md for more information.");
      }

      int bufferConfig = config.getInt(BUFFER_SIZE);
      bufferSize = bufferConfig > 0 ? bufferConfig : 4 * maxBatchStatements;

      if (maxBatchStatements <= 0 && bufferSize <= 0) {
        throw new IllegalArgumentException(
            String.format(
                "Value for batch.bufferSize (%d) must be positive if "
                    + "batch.maxBatchStatements is negative or zero. "
                    + "See settings.md for more information.",
                bufferSize));
      }

      if (bufferSize < maxBatchStatements) {
        throw new IllegalArgumentException(
            String.format(
                "Value for batch.bufferSize (%d) must be greater than or equal to "
                    + "batch.maxBatchStatements OR batch.maxBatchSize (%d). "
                    + "See settings.md for more information.",
                bufferSize, maxBatchStatements));
      }
    } catch (ConfigException e) {
      throw ConfigUtils.convertConfigException(e, "dsbulk.batch");
    }
  }

  public boolean isBatchingEnabled() {
    return mode != BatchMode.DISABLED;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public ReactorStatementBatcher newStatementBatcher(CqlSession session) {
    return new ReactorStatementBatcher(
        session,
        mode.asStatementBatcherMode(),
        DefaultBatchType.UNLOGGED,
        maxBatchStatements,
        maxSizeInBytes);
  }
}
