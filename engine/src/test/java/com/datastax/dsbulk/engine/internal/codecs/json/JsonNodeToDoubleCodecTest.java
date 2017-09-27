/*
 * Copyright (C) 2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dsbulk.engine.internal.codecs.json;

import static com.datastax.dsbulk.engine.internal.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.junit.Test;

public class JsonNodeToDoubleCodecTest {

  private JsonNodeToDoubleCodec codec =
      new JsonNodeToDoubleCodec(
          ThreadLocal.withInitial(
              () -> new DecimalFormat("#,###.##", DecimalFormatSymbols.getInstance(Locale.US))));

  @Test
  public void should_convert_from_valid_input() throws Exception {
    assertThat(codec)
        .convertsFrom(JsonNodeFactory.instance.numberNode(0))
        .to(0d)
        .convertsFrom(JsonNodeFactory.instance.numberNode(1234.56d))
        .to(1234.56d)
        .convertsFrom(JsonNodeFactory.instance.numberNode(1.7976931348623157E308d))
        .to(Double.MAX_VALUE)
        .convertsFrom(JsonNodeFactory.instance.numberNode(4.9E-324d))
        .to(Double.MIN_VALUE)
        .convertsFrom(JsonNodeFactory.instance.textNode("0"))
        .to(0d)
        .convertsFrom(JsonNodeFactory.instance.textNode("1234.56"))
        .to(1234.56d)
        .convertsFrom(JsonNodeFactory.instance.textNode("1,234.56"))
        .to(1234.56d)
        .convertsFrom(JsonNodeFactory.instance.textNode("1.7976931348623157E308"))
        .to(Double.MAX_VALUE)
        .convertsFrom(JsonNodeFactory.instance.textNode("4.9E-324"))
        .to(Double.MIN_VALUE)
        .convertsFrom(null)
        .to(null)
        .convertsFrom(JsonNodeFactory.instance.textNode(""))
        .to(null);
  }

  @Test
  public void should_convert_to_valid_input() throws Exception {
    assertThat(codec)
        .convertsTo(0d)
        .from(JsonNodeFactory.instance.numberNode(0d))
        .convertsTo(1234.56d)
        .from(JsonNodeFactory.instance.numberNode(1234.56d))
        .convertsTo(0.001d)
        .from(JsonNodeFactory.instance.numberNode(0.001d))
        .convertsTo(null)
        .from(JsonNodeFactory.instance.nullNode());
  }

  @Test
  public void should_not_convert_from_invalid_input() throws Exception {
    assertThat(codec).cannotConvertFrom(JsonNodeFactory.instance.textNode("not a valid double"));
  }
}
