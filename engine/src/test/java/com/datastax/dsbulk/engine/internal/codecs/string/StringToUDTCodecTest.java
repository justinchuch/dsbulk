/*
 * Copyright DataStax, Inc.
 *
 * This software is subject to the below license agreement.
 * DataStax may make changes to the agreement from time to time,
 * and will post the amended terms at
 * https://www.datastax.com/terms/datastax-dse-bulk-utility-license-terms.
 */
package com.datastax.dsbulk.engine.internal.codecs.string;

import static com.datastax.dsbulk.engine.internal.codecs.CodecTestUtils.newCodecRegistry;
import static com.datastax.dsbulk.engine.tests.EngineAssertions.assertThat;

import com.datastax.dsbulk.engine.internal.codecs.ExtendedCodecRegistry;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.type.UserDefinedTypeBuilder;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringToUDTCodecTest {
  private final UserDefinedType udt1 =
      new UserDefinedTypeBuilder("ks", "udt")
          .withField("f1a", DataTypes.INT)
          .withField("f1b", DataTypes.mapOf(DataTypes.TEXT, DataTypes.DOUBLE))
          .build();

  private final UserDefinedType udt2 =
      new UserDefinedTypeBuilder("ks", "udt")
          .withField("f2a", udt1)
          .withField("f2b", DataTypes.listOf(DataTypes.DATE))
          .build();

  private final UdtValue udt1Empty = udt1.newValue().setToNull("f1a").setToNull("f1b");

  private final UdtValue udt2Empty = udt2.newValue().setToNull("f2a").setToNull("f2b");

  private final UdtValue udt1Value =
      udt1.newValue()
          .setInt("f1a", 42)
          .setMap("f1b", newMap("foo", 1234.56d, "", 0.12d), String.class, Double.class);

  private final UdtValue udt2Value =
      udt2.newValue()
          .setUdtValue("f2a", udt1Value)
          .set("f2b", newList(LocalDate.of(2017, 9, 22)), TypeCodecs.listOf(TypeCodecs.DATE));

  private StringToUDTCodec udtCodec1;

  private StringToUDTCodec udtCodec2;

  @BeforeEach
  void setUp() {
    ExtendedCodecRegistry codecRegistry = newCodecRegistry("nullStrings = [NULL]");
    udtCodec1 = (StringToUDTCodec) codecRegistry.codecFor(udt1, GenericType.STRING);
    udtCodec2 = (StringToUDTCodec) codecRegistry.codecFor(udt2, GenericType.STRING);
  }

  @Test
  void should_convert_from_valid_external() {
    assertThat(udtCodec1)
        .convertsFromExternal("{\"f1a\":42,\"f1b\":{\"foo\":1234.56,\"\":0.12}}")
        .toInternal(udt1Value)
        .convertsFromExternal("{'f1a':42,'f1b':{'foo':1234.56,'':0.12}}")
        .toInternal(udt1Value)
        .convertsFromExternal(
            "{ \"f1b\" :  { \"foo\" : \"1,234.56\" , \"\" : \"0000.12000\" } , \"f1a\" : \"42.00\" }")
        .toInternal(udt1Value)
        .convertsFromExternal("{ \"f1b\" :  { } , \"f1a\" :  null }")
        .toInternal(udt1Empty)
        .convertsFromExternal("{ \"f1b\" :  null , \"f1a\" :  null }")
        .toInternal(udt1Empty)
        .convertsFromExternal("")
        .toInternal(null)
        .convertsFromExternal(null)
        .toInternal(null)
        .convertsFromExternal("NULL")
        .toInternal(null)
        .convertsFromExternal("{}")
        .toInternal(udt1Empty)
        .convertsFromExternal("")
        .toInternal(null);
    assertThat(udtCodec2)
        .convertsFromExternal(
            "{\"f2a\":{\"f1a\":42,\"f1b\":{\"foo\":1234.56,\"\":0.12}},\"f2b\":[\"2017-09-22\"]}")
        .toInternal(udt2Value)
        .convertsFromExternal(
            "{'f2a':{'f1a':42,'f1b':{'foo':1234.56,'':0.12}},'f2b':['2017-09-22']}")
        .toInternal(udt2Value)
        .convertsFromExternal(
            "{ \"f2b\" :  [ \"2017-09-22\" ] , \"f2a\" : { \"f1b\" :  { \"foo\" : \"1,234.56\" , \"\" : \"0000.12000\" } , \"f1a\" : \"42.00\" } }")
        .toInternal(udt2Value)
        .convertsFromExternal("{ \"f2b\" :  null , \"f2a\" :  null }")
        .toInternal(udt2Empty)
        .convertsFromExternal(null)
        .toInternal(null)
        .convertsFromExternal("NULL")
        .toInternal(null)
        .convertsFromExternal("{}")
        .toInternal(udt2Empty)
        .convertsFromExternal("")
        .toInternal(null);
  }

  @Test
  void should_convert_from_valid_internal() {
    assertThat(udtCodec1)
        .convertsFromInternal(udt1Value)
        .toExternal("{\"f1a\":42,\"f1b\":{\"foo\":1234.56,\"\":0.12}}")
        .convertsFromInternal(udt1.newValue())
        .toExternal("{\"f1a\":null,\"f1b\":{}}")
        .convertsFromInternal(null)
        .toExternal("NULL");
  }

  @Test
  void should_not_convert_from_invalid_external() {
    assertThat(udtCodec1)
        .cannotConvertFromExternal("{\"f1a\":42}")
        .cannotConvertFromExternal("[42]")
        .cannotConvertFromExternal("{\"not a valid input\":\"foo\"}");
  }

  @SuppressWarnings("SameParameterValue")
  private static Map<String, Double> newMap(String k1, Double v1, String k2, Double v2) {
    Map<String, Double> map = new LinkedHashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    return map;
  }

  private static List<LocalDate> newList(LocalDate... elements) {
    return Arrays.asList(elements);
  }
}
