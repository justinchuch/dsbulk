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
package com.datastax.oss.dsbulk.partitioner.assertions;

import static com.datastax.oss.dsbulk.partitioner.assertions.PartitionerAssertions.assertThat;

import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.dsbulk.partitioner.Token;
import com.datastax.oss.dsbulk.partitioner.TokenFactory;
import com.datastax.oss.dsbulk.partitioner.TokenRange;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;

@SuppressWarnings("UnusedReturnValue")
public class TokenRangeAssert<V extends Number, T extends Token<V>>
    extends AbstractObjectAssert<TokenRangeAssert<V, T>, TokenRange<V, T>> {

  TokenRangeAssert(TokenRange<V, T> actual) {
    super(actual, TokenRangeAssert.class);
  }

  public TokenRangeAssert<V, T> startsWith(Object value) {
    Assertions.assertThat(actual.start().value())
        .overridingErrorMessage(
            "Expecting %s to start with %s but it starts with %s", actual, value, actual.start())
        .isEqualTo(value);
    return this;
  }

  public TokenRangeAssert<V, T> endsWith(Object value) {
    Assertions.assertThat(actual.end().value())
        .overridingErrorMessage(
            "Expecting %s to end with %s but it ends with %s", actual, value, actual.end())
        .isEqualTo(value);
    return this;
  }

  public TokenRangeAssert<V, T> startsWith(Token<V> token) {
    Assertions.assertThat(actual.start())
        .overridingErrorMessage(
            "Expecting %s to start with %s but it starts with %s", actual, token, actual.start())
        .isEqualTo(token);
    return this;
  }

  public TokenRangeAssert<V, T> endsWith(Token<V> token) {
    Assertions.assertThat(actual.end())
        .overridingErrorMessage(
            "Expecting %s to end with %s but it ends with %s", actual, token, actual.end())
        .isEqualTo(token);
    return this;
  }

  public TokenRangeAssert<V, T> hasRange(Object start, Object end) {
    Assertions.assertThat(actual.start().value())
        .overridingErrorMessage(
            "Expecting %s to start with %s but it starts with %s", actual, start, actual.start())
        .isEqualTo(start);
    Assertions.assertThat(actual.end().value())
        .overridingErrorMessage(
            "Expecting %s to end with %s but it ends with %s", actual, end, actual.start())
        .isEqualTo(end);
    return this;
  }

  public TokenRangeAssert<V, T> hasSize(long size) {
    Assertions.assertThat(actual.size())
        .overridingErrorMessage(
            "Expecting %s to have size %d but it has size %d", actual, size, actual.size())
        .isEqualTo(size);
    return this;
  }

  public TokenRangeAssert<V, T> hasSize(BigInteger size) {
    Assertions.assertThat(actual.size())
        .overridingErrorMessage(
            "Expecting %s to have size %d but it has size %d", actual, size, actual.size())
        .isEqualTo(size);
    return this;
  }

  public TokenRangeAssert<V, T> hasFraction(double fraction) {
    Assertions.assertThat(actual.fraction())
        .overridingErrorMessage(
            "Expecting %s to have fraction %f but it has fraction %f",
            actual, fraction, actual.fraction())
        .isEqualTo(fraction);
    return this;
  }

  public TokenRangeAssert<V, T> hasFraction(double fraction, Offset<Double> offset) {
    Assertions.assertThat(actual.fraction())
        .overridingErrorMessage(
            "Expecting %s to have fraction %f (+- %s) but it has fraction %f",
            actual, fraction, offset, actual.fraction())
        .isEqualTo(fraction, offset);
    return this;
  }

  public TokenRangeAssert<V, T> hasReplicas(Node... hosts) {
    Set<EndPoint> expected =
        Arrays.stream(hosts).map(Node::getEndPoint).collect(Collectors.toSet());
    Assertions.assertThat(actual.replicas())
        .overridingErrorMessage(
            "Expecting %s to have replicas %s but it had %s", actual, expected, actual.replicas())
        .isEqualTo(expected);
    return this;
  }

  public TokenRangeAssert<V, T> isEmpty() {
    Assertions.assertThat(actual.isEmpty())
        .overridingErrorMessage("Expecting %s to be empty but it was not", actual)
        .isTrue();
    return this;
  }

  public TokenRangeAssert<V, T> isNotEmpty() {
    Assertions.assertThat(actual.isEmpty())
        .overridingErrorMessage("Expecting %s not to be empty but it was", actual)
        .isFalse();
    return this;
  }

  public TokenRangeAssert<V, T> isWrappedAround() {
    Assertions.assertThat(actual.isWrappedAround())
        .overridingErrorMessage("Expecting %s to wrap around but it did not", actual)
        .isTrue();
    TokenFactory<V, T> factory = actual.tokenFactory();
    List<TokenRange<V, T>> unwrapped = actual.unwrap();
    Assertions.assertThat(unwrapped.size())
        .overridingErrorMessage(
            "%s should unwrap to two ranges, but unwrapped to %s", actual, unwrapped)
        .isEqualTo(2);
    Iterator<TokenRange<V, T>> unwrappedIt = unwrapped.iterator();
    TokenRange<V, T> firstRange = unwrappedIt.next();
    assertThat(firstRange).endsWith(factory.minToken());
    TokenRange<V, T> secondRange = unwrappedIt.next();
    assertThat(secondRange).startsWith(factory.minToken());
    return this;
  }

  public TokenRangeAssert<V, T> isNotWrappedAround() {
    Assertions.assertThat(actual.isWrappedAround())
        .overridingErrorMessage("Expecting %s to not wrap around but it did", actual)
        .isFalse();
    Assertions.assertThat(actual.unwrap()).containsExactly(actual);
    return this;
  }

  @SafeVarargs
  public final TokenRangeAssert<V, T> unwrapsTo(TokenRange<V, T>... subRanges) {
    Assertions.assertThat(actual.unwrap()).containsExactly(subRanges);
    return this;
  }
}
