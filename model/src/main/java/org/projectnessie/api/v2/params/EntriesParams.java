/*
 * Copyright (C) 2022 Dremio
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
package org.projectnessie.api.v2.params;

import java.util.Objects;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.immutables.builder.Builder.Constructor;

/**
 * The purpose of this class is to include optional parameters that can be passed to {@code
 * HttpTreeApi#getEntries(String, EntriesParams)}.
 *
 * <p>For easier usage of this class, there is {@link EntriesParams#builder()}, which allows
 * configuring/setting the different parameters.
 */
public class EntriesParams extends AbstractParams<EntriesParams> {

  @Nullable
  @Parameter(
      description =
          "A Common Expression Language (CEL) expression. An intro to CEL can be found at https://github.com/google/cel-spec/blob/master/doc/intro.md.\n"
              + "Usable variables within the expression are 'entry.namespace' (string) & 'entry.contentType' (string)",
      examples = {
        @ExampleObject(ref = "expr_by_namespace"),
        @ExampleObject(ref = "expr_by_contentType"),
        @ExampleObject(ref = "expr_by_namespace_and_contentType")
      })
  @QueryParam("filter")
  private String filter;

  public EntriesParams() {}

  @Constructor
  EntriesParams(@Nullable Integer maxRecords, @Nullable String pageToken, @Nullable String filter) {
    super(maxRecords, pageToken);
    this.filter = filter;
  }

  public static EntriesParamsBuilder builder() {
    return new EntriesParamsBuilder();
  }

  public static EntriesParams empty() {
    return builder().build();
  }

  @Nullable
  public String filter() {
    return filter;
  }

  @Override
  public EntriesParams forNextPage(String pageToken) {
    return new EntriesParams(maxRecords(), pageToken, filter);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", EntriesParams.class.getSimpleName() + "[", "]")
        .add("maxRecords=" + maxRecords())
        .add("pageToken='" + pageToken() + "'")
        .add("filter='" + filter + "'")
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntriesParams)) {
      return false;
    }
    EntriesParams that = (EntriesParams) o;
    return Objects.equals(maxRecords(), that.maxRecords())
        && Objects.equals(pageToken(), that.pageToken())
        && Objects.equals(filter, that.filter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxRecords(), pageToken(), filter);
  }
}
