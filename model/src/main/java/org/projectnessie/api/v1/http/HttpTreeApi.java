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
package org.projectnessie.api.v1.http;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.projectnessie.api.v1.TreeApi;
import org.projectnessie.api.v1.params.CommitLogParams;
import org.projectnessie.api.v1.params.EntriesParams;
import org.projectnessie.api.v1.params.GetReferenceParams;
import org.projectnessie.api.v1.params.Merge;
import org.projectnessie.api.v1.params.ReferencesParams;
import org.projectnessie.api.v1.params.Transplant;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.EntriesResponse;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Operations;
import org.projectnessie.model.Reference;
import org.projectnessie.model.ReferencesResponse;

@Tag(name = "v1")
@Consumes(value = MediaType.APPLICATION_JSON)
@Path("trees")
public interface HttpTreeApi extends TreeApi {

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get all references")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Returned references.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = {
                  @ExampleObject(ref = "referencesResponse"),
                  @ExampleObject(ref = "referencesResponseWithMetadata")
                },
                schema = @Schema(implementation = ReferencesResponse.class))),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
  })
  ReferencesResponse getAllReferences(@BeanParam ReferencesParams params);

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("tree")
  @Operation(summary = "Get default branch for commits and reads")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Returns name and latest hash of the default branch.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = {@ExampleObject(ref = "refObj")},
                schema = @Schema(implementation = Branch.class))),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(responseCode = "404", description = "Default branch not found.")
  })
  Branch getDefaultBranch() throws NessieNotFoundException;

  @Override
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("tree")
  @Operation(
      summary = "Create a new reference",
      description =
          "The type of 'refObj', which can be either a 'Branch' or 'Tag', determines "
              + "the type of the reference to be created.\n"
              + "\n"
              + "'Reference.name' defines the the name of the reference to be created,"
              + "'Reference.hash' is the hash of the created reference, the HEAD of the "
              + "created reference. 'sourceRefName' is the name of the reference which contains "
              + "'Reference.hash', and must be present if 'Reference.hash' is present.\n"
              + "\n"
              + "Specifying no 'Reference.hash' means that the new reference will be created "
              + "\"at the beginning of time\".")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Created successfully.",
        content = {
          @Content(
              mediaType = MediaType.APPLICATION_JSON,
              examples = {@ExampleObject(ref = "refObjNew")},
              schema = @Schema(implementation = Reference.class))
        }),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(responseCode = "403", description = "Not allowed to create reference"),
    @APIResponse(responseCode = "409", description = "Reference already exists"),
  })
  Reference createReference(
      @Parameter(description = "Source named reference") @QueryParam("sourceRefName")
          String sourceRefName,
      @RequestBody(
              description = "Reference to create.",
              content = {
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    examples = {@ExampleObject(ref = "refObjNew")})
              })
          Reference reference)
      throws NessieNotFoundException, NessieConflictException;

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("tree/{ref}")
  @Operation(summary = "Fetch details of a reference")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Found and returned reference.",
        content = {
          @Content(
              mediaType = MediaType.APPLICATION_JSON,
              examples = {@ExampleObject(ref = "refObj")},
              schema = @Schema(implementation = Reference.class))
        }),
    @APIResponse(responseCode = "400", description = "Invalid input, ref name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(responseCode = "403", description = "Not allowed to view the given reference"),
    @APIResponse(responseCode = "404", description = "Ref not found")
  })
  Reference getReferenceByName(@BeanParam GetReferenceParams params) throws NessieNotFoundException;

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("tree/{ref}/entries")
  @Operation(
      summary = "Fetch all entries for a given reference",
      description =
          "Retrieves objects for a ref, potentially truncated by the backend.\n"
              + "\n"
              + "Retrieves up to 'maxRecords' entries for the "
              + "given named reference (tag or branch) or the given hash. "
              + "The backend may respect the given 'max' records hint, but return less or more entries. "
              + "Backends may also cap the returned entries at a hard-coded limit, the default "
              + "REST server implementation has such a hard-coded limit.\n"
              + "\n"
              + "To implement paging, check 'hasMore' in the response and, if 'true', pass the value "
              + "returned as 'token' in the next invocation as the 'pageToken' parameter.\n"
              + "\n"
              + "The content and meaning of the returned 'token' is \"private\" to the implementation,"
              + "treat is as an opaque value.\n"
              + "\n"
              + "It is wrong to assume that invoking this method with a very high 'maxRecords' value "
              + "will return all commit log entries.\n"
              + "\n"
              + "The 'filter' parameter allows for advanced filtering capabilities using the Common Expression Language (CEL).\n"
              + "An intro to CEL can be found at https://github.com/google/cel-spec/blob/master/doc/intro.md.\n"
              + "\n"
              + "The 'namespaceDepth' parameter returns only the ContentKey components up to the depth of 'namespaceDepth'.\n"
              + "For example they key 'a.b.c.d' with a depth of 3 will return 'a.b.c'. The operation is guaranteed to not return \n"
              + "duplicates and therefore will never page.")
  @APIResponses({
    @APIResponse(
        description = "all objects for a reference",
        content = {
          @Content(
              mediaType = MediaType.APPLICATION_JSON,
              examples = {@ExampleObject(ref = "entriesResponse")},
              schema = @Schema(implementation = EntriesResponse.class))
        }),
    @APIResponse(responseCode = "200", description = "Returned successfully."),
    @APIResponse(responseCode = "400", description = "Invalid input, ref name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(
        responseCode = "403",
        description = "Not allowed to view the given reference or fetch entries for it"),
    @APIResponse(responseCode = "404", description = "Ref not found")
  })
  EntriesResponse getEntries(
      @Parameter(
              description = "name of ref to fetch from",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("ref")
          String refName,
      @BeanParam EntriesParams params)
      throws NessieNotFoundException;

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("tree/{ref}/log")
  @Operation(
      summary = "Get commit log for a reference",
      description =
          "Retrieve the commit log for a ref, potentially truncated by the backend.\n"
              + "\n"
              + "Retrieves up to 'maxRecords' commit-log-entries starting at the HEAD of the "
              + "given named reference (tag or branch) or the given hash. "
              + "The backend may respect the given 'max' records hint, but return less or more entries. "
              + "Backends may also cap the returned entries at a hard-coded limit, the default "
              + "REST server implementation has such a hard-coded limit.\n"
              + "\n"
              + "To implement paging, check 'hasMore' in the response and, if 'true', pass the value "
              + "returned as 'token' in the next invocation as the 'pageToken' parameter.\n"
              + "\n"
              + "The content and meaning of the returned 'token' is \"private\" to the implementation,"
              + "treat is as an opaque value.\n"
              + "\n"
              + "It is wrong to assume that invoking this method with a very high 'maxRecords' value "
              + "will return all commit log entries.\n"
              + "\n"
              + "The 'filter' parameter allows for advanced filtering capabilities using the Common Expression Language (CEL).\n"
              + "An intro to CEL can be found at https://github.com/google/cel-spec/blob/master/doc/intro.md.\n")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Returned commits.",
        content = {
          @Content(
              mediaType = MediaType.APPLICATION_JSON,
              examples = {
                @ExampleObject(ref = "logResponseAdditionalInfo"),
                @ExampleObject(ref = "logResponseSimple")
              },
              schema = @Schema(implementation = LogResponse.class))
        }),
    @APIResponse(responseCode = "400", description = "Invalid input, ref name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(
        responseCode = "403",
        description = "Not allowed to view the given reference or get commit log for it"),
    @APIResponse(responseCode = "404", description = "Ref doesn't exists")
  })
  LogResponse getCommitLog(
      @Parameter(
              description = "ref to show log from",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("ref")
          String ref,
      @BeanParam CommitLogParams params)
      throws NessieNotFoundException;

  @Override
  @PUT
  @Path("{referenceType}/{referenceName}")
  @Operation(
      summary = "Set a named reference to a specific hash via a named-reference.",
      description =
          "This operation takes the name of the named reference to reassign and the hash and the name of a "
              + "named-reference via which the caller has access to that hash.")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Assigned successfully"),
    @APIResponse(responseCode = "400", description = "Invalid input, ref/hash name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(responseCode = "403", description = "Not allowed to view or assign reference"),
    @APIResponse(responseCode = "404", description = "One or more references don't exist"),
    @APIResponse(responseCode = "409", description = "Update conflict")
  })
  void assignReference(
      @Parameter(
              description = "Reference type to reassign",
              examples = {@ExampleObject(ref = "referenceType")})
          @PathParam("referenceType")
          Reference.ReferenceType referenceType,
      @Parameter(
              description = "Reference name to reassign",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("referenceName")
          String referenceName,
      @Parameter(
              description = "Expected previous hash of reference",
              examples = {@ExampleObject(ref = "hash")})
          @QueryParam("expectedHash")
          String expectedHash,
      @RequestBody(
              description =
                  "Reference hash to which 'referenceName' shall be assigned to. This must be either a "
                      + "'Transaction', 'Branch' or 'Tag' via which the hash is visible to the caller.",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      examples = {@ExampleObject(ref = "refObj"), @ExampleObject(ref = "tagObj")}))
          Reference assignTo)
      throws NessieNotFoundException, NessieConflictException;

  @Override
  @DELETE
  @Path("{referenceType}/{referenceName}")
  @Operation(summary = "Delete a reference endpoint")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Deleted successfully."),
    @APIResponse(responseCode = "400", description = "Invalid input, ref/hash name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(responseCode = "403", description = "Not allowed to view or delete reference"),
    @APIResponse(responseCode = "404", description = "Ref doesn't exists"),
    @APIResponse(responseCode = "409", description = "update conflict"),
  })
  void deleteReference(
      @Parameter(
              description = "Reference type to delete",
              examples = {@ExampleObject(ref = "referenceType")})
          @PathParam("referenceType")
          Reference.ReferenceType referenceType,
      @Parameter(
              description = "Reference name to delete",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("referenceName")
          String referenceName,
      @Parameter(
              description = "Expected hash of tag",
              examples = {@ExampleObject(ref = "hash")})
          @QueryParam("expectedHash")
          String expectedHash)
      throws NessieConflictException, NessieNotFoundException;

  @Override
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("branch/{branchName}/transplant")
  @Operation(
      summary = "Transplant commits from 'transplant' onto 'branchName'",
      description =
          "This is done as an atomic operation such that only the last of the sequence is ever "
              + "visible to concurrent readers/writers. The sequence to transplant must be "
              + "contiguous and in order.")
  @APIResponses({
    @APIResponse(
        responseCode = "204",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = {
                  @ExampleObject(ref = "mergeResponseSuccess"),
                  @ExampleObject(ref = "mergeResponseFail")
                },
                schema = @Schema(implementation = MergeResponse.class)),
        description =
            "Transplant operation completed. "
                + "The actual transplant might have failed and reported as successful=false, "
                + "if the client asked to return a conflict as a result instead of returning an error. "
                + "Note: the 'commonAncestor' field in a response will always be null for a transplant."),
    @APIResponse(responseCode = "400", description = "Invalid input, ref/hash name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(
        responseCode = "403",
        description = "Not allowed to view the given reference or transplant commits"),
    @APIResponse(responseCode = "404", description = "Ref doesn't exists"),
    @APIResponse(responseCode = "409", description = "update conflict")
  })
  MergeResponse transplantCommitsIntoBranch(
      @Parameter(
              description = "Branch to transplant into",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("branchName")
          String branchName,
      @Parameter(
              description = "Expected hash of tag.",
              examples = {@ExampleObject(ref = "hash")})
          @QueryParam("expectedHash")
          String expectedHash,
      @Parameter(
              description = "commit message",
              examples = {@ExampleObject(ref = "commitMessage")})
          @QueryParam("message")
          String message,
      @RequestBody(
              description = "Hashes to transplant",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      examples = {@ExampleObject(ref = "transplant")}))
          Transplant transplant)
      throws NessieNotFoundException, NessieConflictException;

  @Override
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("branch/{branchName}/merge")
  @Operation(
      summary = "Merge commits from 'mergeRef' onto 'branchName'.",
      description =
          "Merge items from an existing hash in 'mergeRef' into the requested branch. "
              + "The merge is always a rebase + fast-forward merge and is only completed if the "
              + "rebase is conflict free. The set of commits added to the branch will be all of "
              + "those until we arrive at a common ancestor. Depending on the underlying "
              + "implementation, the number of commits allowed as part of this operation may be limited.")
  @APIResponses({
    @APIResponse(
        responseCode = "204",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = {
                  @ExampleObject(ref = "mergeResponseSuccess"),
                  @ExampleObject(ref = "mergeResponseFail")
                },
                schema = @Schema(implementation = MergeResponse.class)),
        description =
            "Merge operation completed. "
                + "The actual merge might have failed and reported as successful=false, "
                + "if the client asked to return a conflict as a result instead of returning an error."),
    @APIResponse(responseCode = "400", description = "Invalid input, ref/hash name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(
        responseCode = "403",
        description = "Not allowed to view the given reference or merge commits"),
    @APIResponse(responseCode = "404", description = "Ref doesn't exists"),
    @APIResponse(responseCode = "409", description = "update conflict")
  })
  MergeResponse mergeRefIntoBranch(
      @Parameter(
              description = "Branch to merge into",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("branchName")
          String branchName,
      @Parameter(
              description = "Expected current HEAD of 'branchName'",
              examples = {@ExampleObject(ref = "hash")})
          @QueryParam("expectedHash")
          String expectedHash,
      @RequestBody(
              description =
                  "Merge operation that defines the source reference name and an optional hash. "
                      + "If 'fromHash' is not present, the current 'sourceRef's HEAD will be used.",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      examples = {@ExampleObject(ref = "merge")}))
          Merge merge)
      throws NessieNotFoundException, NessieConflictException;

  @Override
  @POST
  @Path("branch/{branchName}/commit")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Commit multiple operations against the given branch expecting that branch to have "
              + "the given hash as its latest commit. The hash in the successful response contains the hash of the "
              + "commit that contains the operations of the invocation.")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Updated successfully.",
        content = {
          @Content(
              mediaType = MediaType.APPLICATION_JSON,
              examples = {@ExampleObject(ref = "refObj")},
              schema = @Schema(implementation = Branch.class))
        }),
    @APIResponse(responseCode = "400", description = "Invalid input, ref/hash name not valid"),
    @APIResponse(responseCode = "401", description = "Invalid credentials provided"),
    @APIResponse(
        responseCode = "403",
        description = "Not allowed to view the given reference or perform commits"),
    @APIResponse(responseCode = "404", description = "Provided ref doesn't exists"),
    @APIResponse(responseCode = "409", description = "Update conflict")
  })
  Branch commitMultipleOperations(
      @Parameter(
              description = "Branch to change, defaults to default branch.",
              examples = {@ExampleObject(ref = "ref")})
          @PathParam("branchName")
          String branchName,
      @Parameter(
              description = "Expected hash of branch.",
              examples = {@ExampleObject(ref = "hash")})
          @QueryParam("expectedHash")
          String expectedHash,
      @RequestBody(
              description = "Operations",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      examples = {@ExampleObject(ref = "operations")}))
          Operations operations)
      throws NessieNotFoundException, NessieConflictException;
}
