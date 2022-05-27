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
import org.projectnessie.versioned.ReferenceNotFoundException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "set-global-pointer",
    mixinStandardHelpOptions = true,
    description = "Fix global pointer")
public class SetGlobalPointer extends BaseCommand {

  @Option(
      names = {"-B", "--backup"},
      description = "Backup current pointer under a new key")
  String backupKey;

  @Option(
      names = {"-S", "--show"},
      description = "Show global state pointer with non-standard ID")
  String ptrKey;

  @Option(
      names = {"-P", "--parent"},
      description = "The new global log parent")
  String newParent;

  @Override
  public Integer call() throws ReferenceNotFoundException, InvalidProtocolBufferException {
    warnOnInMemory();

    PrintWriter out = spec.commandLine().getOut();

    if (backupKey != null) {
      out.printf("Backing up current pointer under ID %s%n", backupKey);
      databaseAdapter.diagnosticBackupGlobalPointer(backupKey);
    }

    if (ptrKey != null) {
      out.printf("Getting pointer under ID %s%n", ptrKey);
      databaseAdapter.diagnosticShowGlobalPointer(ptrKey, out);
    }

    if (newParent != null) {
      out.printf("Updating pointer pointer with parent %s%n", newParent);
      databaseAdapter.diagnosticSetGlobalPointerParent(newParent);
    }

    return 0;
  }
}
