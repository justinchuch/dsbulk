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
package com.datastax.oss.dsbulk.partitioner;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RandomTokenFactoryTest {

  private RandomTokenFactory factory = RandomTokenFactory.INSTANCE;

  @Test
  void should_create_token_from_string() {
    Assertions.assertThat(factory.tokenFromString("0")).isEqualTo(new RandomToken(ZERO));
    Assertions.assertThat(factory.tokenFromString("-1"))
        .isEqualTo(new RandomToken(BigInteger.valueOf(-1)));
    Assertions.assertThat(factory.tokenFromString("170141183460469231731687303715884105728"))
        .isEqualTo(new RandomToken(new BigInteger("170141183460469231731687303715884105728")));
  }

  @Test
  void should_calculate_distance_between_tokens_if_right_gt_left() {
    assertThat(factory.distance(new RandomToken(ZERO), new RandomToken(ONE))).isEqualTo(ONE);
  }

  @Test
  void should_calculate_distance_between_tokens_if_right_lte_left() {
    assertThat(factory.distance(new RandomToken(ZERO), new RandomToken(ZERO)))
        .isEqualTo(factory.totalTokenCount());
    assertThat(factory.distance(factory.maxToken(), factory.minToken())).isEqualTo(ZERO);
  }

  @Test
  void should_calculate_ring_fraction() {
    assertThat(factory.fraction(new RandomToken(ZERO), new RandomToken(ZERO))).isEqualTo(1.0);
    assertThat(factory.fraction(new RandomToken(ZERO), factory.maxToken())).isEqualTo(1.0);
    assertThat(factory.fraction(factory.maxToken(), factory.minToken())).isEqualTo(0.0);
    assertThat(
            factory.fraction(
                new RandomToken(ZERO),
                new RandomToken(factory.maxToken().value().divide(BigInteger.valueOf(2)))))
        .isEqualTo(0.5);
  }
}
