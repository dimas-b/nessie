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

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.PrintWriter;
import java.util.stream.Stream;
import org.projectnessie.versioned.persist.adapter.ContentId;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "global-keys",
    mixinStandardHelpOptions = true,
    description = "Diagnostic info about content IDs in the global log")
public class GlobalKeys extends BaseCommand {

  @CommandLine.Option(
      names = {"-p", "--global-pointer"},
      description = "Fetch global pointer")
  boolean fetchPointer;

  @CommandLine.Option(
      names = {"-d", "--dump-global-log"},
      description = "Raw global log data")
  boolean dumpLog;

  @CommandLine.Option(
      names = {"-a", "--find-dangling-parents"},
      description = "Find dangling parent references")
  boolean analyse;

  @Override
  public Integer call() throws InvalidProtocolBufferException {
    warnOnInMemory();

    PrintWriter out = spec.commandLine().getOut();

    if (fetchPointer) {
      out.printf("Global pointer: %s%n", databaseAdapter.diagnosticFetchGlobalPointer());
    }
    if (dumpLog) {
      databaseAdapter.diagnosticDumpGlobalLog(out);
    } else if (analyse) {
      databaseAdapter.diagnosticAnalyseGlobalLog(out);
    } else {
      try (Stream<ContentId> stream = databaseAdapter.globalKeys()) {
        stream.forEach(
            k -> {
              out.printf("Global content ID: %s%n", k);
            });
      }
    }

    return 0;
  }
}
