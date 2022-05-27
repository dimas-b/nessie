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
package org.projectnessie.quarkus.cli;

import com.google.protobuf.ByteString;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.projectnessie.model.Content;
import org.projectnessie.server.store.TableCommitMetaStoreWorker;
import org.projectnessie.versioned.GetNamedRefsParams;
import org.projectnessie.versioned.Hash;
import org.projectnessie.versioned.Key;
import org.projectnessie.versioned.ReferenceInfo;
import org.projectnessie.versioned.ReferenceNotFoundException;
import org.projectnessie.versioned.persist.adapter.ContentAndState;
import org.projectnessie.versioned.persist.adapter.KeyFilterPredicate;
import org.projectnessie.versioned.persist.adapter.KeyListEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "validate-global-state",
    mixinStandardHelpOptions = true,
    description = "Validate content's global state")
public class ValidateContent extends BaseCommand {

  @Option(
      names = {"-E", "--errors-only"},
      description = "Output only keys with errors")
  boolean errorsOnly;

  @Option(
      names = {"-r", "--ref"},
      description = "Reference name to scan (default: main)")
  String ref = "main";

  @Override
  public Integer call() throws ReferenceNotFoundException {
    warnOnInMemory();

    TableCommitMetaStoreWorker worker = new TableCommitMetaStoreWorker();

    PrintWriter out = spec.commandLine().getOut();

    out.printf("Scanning all keys in %s...%n", ref);

    ReferenceInfo<ByteString> namedRef = databaseAdapter.namedRef(ref, GetNamedRefsParams.DEFAULT);
    Hash head = namedRef.getHash();
    out.printf("Reference HEAD: %s%n", head);

    Stream<KeyListEntry> keys = databaseAdapter.keys(head, KeyFilterPredicate.ALLOW_ALL);
    keys.forEach(
        k -> {
          ContentAndState<ByteString> content;
          try {
            Map<Key, ContentAndState<ByteString>> values =
                databaseAdapter.values(head, List.of(k.getKey()), KeyFilterPredicate.ALLOW_ALL);
            content = values.get(k.getKey());
          } catch (ReferenceNotFoundException e) {
            throw new IllegalStateException(e);
          }

          String error = "";
          String info = "";
          try {
            Content c = worker.valueFromStore(content.getRefState(), content::getGlobalState);
            info = "" + c;
          } catch (Exception e) {
            error = e.toString();
          }

          if (!errorsOnly || !"".equals(error)) {
            String status = "".equals(error) ? "OK" : "ERROR";
            out.printf("%s: %s %s: %s %s%n", status, k.getContentId(), k.getKey(), info, error);
          }
        });

    return 0;
  }
}
