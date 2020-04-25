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
package com.datastax.oss.dsbulk.codecs.text.json;

import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.dsbulk.codecs.util.CodecUtils;
import com.datastax.oss.dsbulk.codecs.util.TemporalFormat;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.List;

public class JsonNodeToLocalTimeCodec extends JsonNodeToTemporalCodec<LocalTime> {

  private final ZoneId timeZone;

  public JsonNodeToLocalTimeCodec(
      TemporalFormat parser, ZoneId timeZone, List<String> nullStrings) {
    super(TypeCodecs.TIME, parser, nullStrings);
    this.timeZone = timeZone;
  }

  @Override
  public LocalTime externalToInternal(JsonNode s) {
    TemporalAccessor temporal = parseTemporalAccessor(s);
    if (temporal == null) {
      return null;
    }
    return CodecUtils.toLocalTime(temporal, timeZone);
  }
}
