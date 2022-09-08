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
package org.projectnessie.services.rest;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;
import org.projectnessie.api.http.HttpTreeApi;
import org.projectnessie.api.params.CommitLogParams;
import org.projectnessie.api.params.DiffParams;
import org.projectnessie.api.params.EntriesParams;
import org.projectnessie.api.params.GetReferenceParams;
import org.projectnessie.api.params.ReferencesParams;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.Content;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.DiffResponse;
import org.projectnessie.model.EntriesResponse;
import org.projectnessie.model.GetMultipleContentsRequest;
import org.projectnessie.model.GetMultipleContentsResponse;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.Merge;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Operations;
import org.projectnessie.model.Reference;
import org.projectnessie.model.ReferencesResponse;
import org.projectnessie.model.Transplant;

/** REST endpoint for the tree-API. */
@RequestScoped
@Path("v2/trees")
public class RestV2TreeResource implements HttpTreeApi {
  @Override
  public Branch getDefaultBranch() throws NessieNotFoundException {
    return null;
  }

  @Override
  public ReferencesResponse getAllReferences(ReferencesParams params) {
    return null;
  }

  @Override
  public Reference createReference(String name, Reference.ReferenceType type, Reference reference)
      throws NessieNotFoundException, NessieConflictException {
    return null;
  }

  @Override
  public Reference getReferenceByName(GetReferenceParams params) throws NessieNotFoundException {
    return null;
  }

  @Override
  public EntriesResponse getEntries(String ref, EntriesParams params)
      throws NessieNotFoundException {
    return null;
  }

  @Override
  public LogResponse getCommitLog(String ref, CommitLogParams params)
      throws NessieNotFoundException {
    return null;
  }

  @Override
  public DiffResponse getDiff(DiffParams params) throws NessieNotFoundException {
    return null;
  }

  @Override
  public void assignReference(Reference.ReferenceType type, String ref, Reference assignTo)
      throws NessieNotFoundException, NessieConflictException {}

  @Override
  public void deleteReference(Reference.ReferenceType type, String ref)
      throws NessieConflictException, NessieNotFoundException {}

  @Override
  public Content getContent(ContentKey key, String ref) throws NessieNotFoundException {
    return null;
  }

  @Override
  public GetMultipleContentsResponse getMultipleContents(
      String ref, GetMultipleContentsRequest request) throws NessieNotFoundException {
    return null;
  }

  @Override
  public MergeResponse transplantCommitsIntoBranch(String branch, Transplant transplant)
      throws NessieNotFoundException, NessieConflictException {
    return null;
  }

  @Override
  public MergeResponse mergeRefIntoBranch(String branch, Merge merge)
      throws NessieNotFoundException, NessieConflictException {
    return null;
  }

  @Override
  public Branch commitMultipleOperations(String branch, Operations operations)
      throws NessieNotFoundException, NessieConflictException {
    return null;
  }
}
