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
import org.projectnessie.server.store.TableCommitMetaStoreWorker;
import org.projectnessie.server.store.proto.ObjectTypes;
import org.projectnessie.versioned.GetNamedRefsParams;
import org.projectnessie.versioned.Hash;
import org.projectnessie.versioned.Key;
import org.projectnessie.versioned.ReferenceInfo;
import org.projectnessie.versioned.ReferenceNotFoundException;
import org.projectnessie.versioned.persist.adapter.ContentAndState;
import org.projectnessie.versioned.persist.adapter.KeyFilterPredicate;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "content",
    mixinStandardHelpOptions = true,
    description = "Diagnostic info about a particular content object")
public class ContentInfo extends BaseCommand {

  @Option(
      names = {"-k"},
      description = "Conent key elements (one or more)")
  List<String> keyElements;

  @Option(
      names = {"-r", "--ref"},
      description = "Reference name (default: main)")
  String ref = "main";

  @Override
  public Integer call() throws ReferenceNotFoundException {
    warnOnInMemory();

    TableCommitMetaStoreWorker worker = new TableCommitMetaStoreWorker();

    PrintWriter out = spec.commandLine().getOut();

    ReferenceInfo<ByteString> namedRef = databaseAdapter.namedRef(ref, GetNamedRefsParams.DEFAULT);
    Hash head = namedRef.getHash();
    out.printf("Reference HEAD: %s%n", head);

    Key key = Key.of(keyElements);
    ContentAndState<ByteString> content;
    try {
      Map<Key, ContentAndState<ByteString>> values =
          databaseAdapter.values(head, List.of(key), KeyFilterPredicate.ALLOW_ALL);
      content = values.get(key);
    } catch (ReferenceNotFoundException e) {
      throw new IllegalStateException(e);
    }

    if (content == null) {
      out.printf("Content not found for %s%n", key);
      return 1;
    }

    try {
      ObjectTypes.Content refState = ObjectTypes.Content.parseFrom(content.getRefState());
      out.printf("Ref state: %s%n", refState);

      if (content.getGlobalState() == null) {
        out.printf("Global state: NULL%n");
      } else {
        ObjectTypes.Content globalState = ObjectTypes.Content.parseFrom(content.getRefState());
        out.printf("Global state: %s%n", globalState);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    return 0;
  }
}
