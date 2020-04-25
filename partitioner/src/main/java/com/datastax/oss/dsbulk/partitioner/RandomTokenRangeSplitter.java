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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

class RandomTokenRangeSplitter implements TokenRangeSplitter<BigInteger, Token<BigInteger>> {

  public static final RandomTokenRangeSplitter INSTANCE = new RandomTokenRangeSplitter();

  private RandomTokenRangeSplitter() {}

  @Override
  public List<TokenRange<BigInteger, Token<BigInteger>>> split(
      TokenRange<BigInteger, Token<BigInteger>> tokenRange, int splitCount) {
    BigInteger rangeSize = tokenRange.size();
    BigInteger val = BigInteger.valueOf(splitCount);
    // If the range size is lesser than the number of splits,
    // use the range size as number of splits and yield (size-of-range) splits of size 1
    BigInteger splitPointsCount = rangeSize.compareTo(val) < 0 ? rangeSize : val;
    BigInteger start = tokenRange.start().value();
    List<Token<BigInteger>> splitPoints = new ArrayList<>();
    for (BigInteger i = ZERO; i.compareTo(splitPointsCount) < 0; i = i.add(ONE)) {
      // instead of applying a fix increment we multiply and
      // divide again at each step to compensate for non-integral
      // increment sizes and thus to create splits of sizes as even as
      // possible (iow, to minimize the split sizes variance).
      BigInteger increment = rangeSize.multiply(i).divide(splitPointsCount);
      RandomToken splitPoint = new RandomToken(wrap(start.add(increment)));
      splitPoints.add(splitPoint);
    }
    splitPoints.add(tokenRange.end());
    List<TokenRange<BigInteger, Token<BigInteger>>> splits = new ArrayList<>();
    for (int i = 0; i < splitPoints.size() - 1; i++) {
      List<Token<BigInteger>> window = splitPoints.subList(i, i + 2);
      TokenRange<BigInteger, Token<BigInteger>> split =
          new TokenRange<>(
              window.get(0), window.get(1), tokenRange.replicas(), tokenRange.tokenFactory());
      splits.add(split);
    }
    return splits;
  }

  private BigInteger wrap(BigInteger token) {
    return token.compareTo(RandomTokenFactory.MAX_TOKEN_VALUE) <= 0
        ? token
        : token.subtract(RandomTokenFactory.MAX_TOKEN_VALUE);
  }
}
