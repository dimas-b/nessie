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

import org.projectnessie.apiv1.http.HttpRefLogApi;
import org.projectnessie.apiv1.params.RefLogParams;
import org.projectnessie.client.builder.BaseGetRefLogBuilder;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.RefLogResponse;

final class HttpGetRefLog extends BaseGetRefLogBuilder<RefLogParams> {

  private final HttpRefLogApi api;

  HttpGetRefLog(HttpRefLogApi api) {
    super(RefLogParams::forNextPage);
    this.api = api;
  }

  @Override
  protected RefLogParams params() {
    return RefLogParams.builder()
        .startHash(untilHash)
        .endHash(fromHash)
        .filter(filter)
        .maxRecords(maxRecords)
        .build();
  }

  @SuppressWarnings("deprecation")
  @Override
  protected RefLogResponse get(RefLogParams p) throws NessieNotFoundException {
    return api.getRefLog(p);
  }
}
