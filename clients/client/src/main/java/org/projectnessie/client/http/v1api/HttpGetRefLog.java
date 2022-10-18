/*
 * Copyright (C) 2020 Dremio
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
package org.projectnessie.client.http.v1api;

import java.util.stream.Stream;
import org.projectnessie.apiv1.params.RefLogParams;
import org.projectnessie.apiv1.params.RefLogParamsBuilder;
import org.projectnessie.client.StreamingUtil;
import org.projectnessie.client.api.GetRefLogBuilder;
import org.projectnessie.client.http.NessieApiClient;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.RefLogResponse;
import org.projectnessie.model.RefLogResponse.RefLogResponseEntry;

final class HttpGetRefLog extends BaseHttpRequest implements GetRefLogBuilder {

  private final RefLogParamsBuilder params = RefLogParams.builder();

  HttpGetRefLog(NessieApiClient client) {
    super(client);
  }

  @Override
  public GetRefLogBuilder untilHash(String untilHash) {
    params.startHash(untilHash);
    return this;
  }

  @Override
  public GetRefLogBuilder fromHash(String fromHash) {
    params.endHash(fromHash);
    return this;
  }

  @Override
  public GetRefLogBuilder filter(String filter) {
    params.filter(filter);
    return this;
  }

  @Override
  public GetRefLogBuilder maxRecords(int maxRecords) {
    params.maxRecords(maxRecords);
    return this;
  }

  @Override
  public GetRefLogBuilder pageToken(String pageToken) {
    params.pageToken(pageToken);
    return this;
  }

  private RefLogParams params() {
    return params.build();
  }

  @Override
  public RefLogResponse get() throws NessieNotFoundException {
    return get(params());
  }

  private RefLogResponse get(RefLogParams p) throws NessieNotFoundException {
    return client.getRefLogApi().getRefLog(p);
  }

  @Override
  public Stream<RefLogResponseEntry> stream() throws NessieNotFoundException {
    RefLogParams p = params();
    return StreamingUtil.generateStream(
        RefLogResponse::getLogEntries, pageToken -> get(p.forNextPage(pageToken)));
  }
}
