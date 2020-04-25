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
package com.datastax.oss.dsbulk.workflow.api.error;

import edu.umd.cs.findbugs.annotations.NonNull;

public class UnlimitedErrorThreshold implements ErrorThreshold {

  public static final UnlimitedErrorThreshold INSTANCE = new UnlimitedErrorThreshold();

  private UnlimitedErrorThreshold() {}

  @Override
  public boolean checkThresholdExceeded(long errorCount, @NonNull Number totalItems) {
    return false;
  }

  @Override
  public String thresholdAsString() {
    return "unlimited";
  }
}
