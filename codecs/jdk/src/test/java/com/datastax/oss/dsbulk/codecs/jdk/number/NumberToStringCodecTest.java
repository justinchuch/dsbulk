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
package com.datastax.oss.dsbulk.codecs.jdk.number;

import static com.datastax.oss.dsbulk.tests.assertions.TestAssertions.assertThat;

import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.dsbulk.codecs.CommonConversionContext;
import com.datastax.oss.dsbulk.codecs.ConvertingCodecFactory;
import org.junit.jupiter.api.Test;

class NumberToStringCodecTest {

  private NumberToStringCodec<Long> codec =
      (NumberToStringCodec<Long>)
          new ConvertingCodecFactory(
                  new CommonConversionContext().setNullStrings("NULL").setFormatNumbers(true))
              .<Long, String>createConvertingCodec(DataTypes.TEXT, GenericType.LONG, true);

  @Test
  void should_convert_when_valid_input() {
    assertThat(codec)
        .convertsFromExternal(123456L)
        .toInternal("123,456")
        .convertsFromExternal(null)
        .toInternal(null)
        .convertsFromInternal(null)
        .toExternal(null)
        .convertsFromInternal("")
        .toExternal(null);
  }
}
