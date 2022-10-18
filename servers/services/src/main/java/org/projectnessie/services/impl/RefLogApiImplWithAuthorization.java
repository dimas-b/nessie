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
package org.projectnessie.services.impl;

import java.security.Principal;
import org.projectnessie.apiv1.params.RefLogParams;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.RefLogResponse;
import org.projectnessie.services.authz.Authorizer;
import org.projectnessie.services.config.ServerConfig;
import org.projectnessie.versioned.VersionStore;

/** Does authorization check (if enabled) on the {@link RefLogApiImpl}. */
public class RefLogApiImplWithAuthorization extends RefLogApiImpl {

  public RefLogApiImplWithAuthorization(
      ServerConfig config, VersionStore store, Authorizer authorizer, Principal principal) {
    super(config, store, authorizer, principal);
  }

  @Override
  public RefLogResponse getRefLog(RefLogParams params) throws NessieNotFoundException {
    startAccessCheck().canViewRefLog().checkAndThrow();
    return super.getRefLog(params);
  }
}
