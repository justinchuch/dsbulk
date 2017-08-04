/*
 * Copyright (C) 2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.loader.engine.internal.codecs;

import static com.datastax.loader.engine.internal.Assertions.assertThat;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.junit.Test;

public class StringToBigIntegerCodecTest {

  private StringToBigIntegerCodec codec =
      new StringToBigIntegerCodec(
          ThreadLocal.withInitial(
              () -> new DecimalFormat("#,###.##", DecimalFormatSymbols.getInstance(Locale.US))));

  @Test
  public void should_convert_from_valid_input() throws Exception {
    assertThat(codec)
        .convertsFrom("0")
        .to(BigInteger.ZERO)
        .convertsFrom("-1,234")
        .to(new BigInteger("-1234"))
        .convertsFrom(null)
        .to(null);
  }

  @Test
  public void should_convert_to_valid_input() throws Exception {
    assertThat(codec)
        .convertsTo(BigInteger.ZERO)
        .from("0")
        .convertsTo(new BigInteger("-1234"))
        .from("-1,234")
        .convertsTo(null)
        .from(null);
  }

  @Test
  public void should_not_convert_from_invalid_input() throws Exception {
    assertThat(codec).cannotConvertFrom("-1.234").cannotConvertFrom("not a valid biginteger");
  }
}