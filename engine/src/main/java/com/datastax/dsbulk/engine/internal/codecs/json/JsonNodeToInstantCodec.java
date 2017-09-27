/*
 * Copyright (C) 2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dsbulk.engine.internal.codecs.json;

import com.datastax.driver.core.exceptions.InvalidTypeException;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class JsonNodeToInstantCodec extends JsonNodeToTemporalCodec<Instant> {

  public JsonNodeToInstantCodec(DateTimeFormatter parser) {
    super(InstantCodec.instance, parser);
  }

  @Override
  public Instant convertFrom(JsonNode node) {
    TemporalAccessor temporal = parseAsTemporalAccessor(node);
    if (temporal == null) {
      return null;
    }
    try {
      return ZonedDateTime.from(temporal).toInstant();
    } catch (DateTimeException e) {
      throw new InvalidTypeException("Cannot parse instant:" + node, e);
    }
  }
}
