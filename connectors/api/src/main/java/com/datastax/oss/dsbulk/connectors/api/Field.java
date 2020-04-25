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
package com.datastax.oss.dsbulk.connectors.api;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A field in a record. Fields can be {@linkplain IndexedField indexed} or {@linkplain MappedField
 * mapped}.
 */
public interface Field {

  /**
   * @return a generic description of the field, mainly for error reporting purposes; usually its
   *     name or index.
   */
  @NonNull
  String getFieldDescription();
}
