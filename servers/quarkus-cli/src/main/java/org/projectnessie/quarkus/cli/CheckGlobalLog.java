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
import com.mongodb.client.MongoClient;
import io.quarkus.arc.Arc;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClients;
import java.io.PrintWriter;
import java.util.List;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.projectnessie.server.store.TableCommitMetaStoreWorker;
import org.projectnessie.versioned.ReferenceNotFoundException;
import org.projectnessie.versioned.persist.mongodb.MongoClientConfig;
import org.projectnessie.versioned.persist.mongodb.MongoDatabaseAdapter;
import org.projectnessie.versioned.persist.mongodb.MongoDatabaseClient;
import org.projectnessie.versioned.persist.nontx.ImmutableAdjustableNonTransactionalDatabaseAdapterConfig;
import org.projectnessie.versioned.persist.nontx.NonTransactionalDatabaseAdapterConfig;
import picocli.CommandLine.Command;

@Command(
    name = "check-global-log",
    mixinStandardHelpOptions = true,
    description = "Find repositories with errors in global log")
public class CheckGlobalLog extends BaseCommand {
  @Inject NonTransactionalDatabaseAdapterConfig config;

  @Inject
  @ConfigProperty(name = "quarkus.mongodb.database")
  String databaseName;

  @Override
  public Integer call() throws InvalidProtocolBufferException, ReferenceNotFoundException {
    warnOnInMemory();

    PrintWriter out = spec.commandLine().getOut();

    TableCommitMetaStoreWorker worker = new TableCommitMetaStoreWorker();

    List<String> repos = databaseAdapter.diagnosticListRepoIds();
    out.printf("Found %s tenants%n", repos.size());

    for (String repo : repos) {

      ImmutableAdjustableNonTransactionalDatabaseAdapterConfig cfg =
          ImmutableAdjustableNonTransactionalDatabaseAdapterConfig.builder()
              .from(config)
              .repositoryId(repo)
              .build();

      MongoClients mongoClients = Arc.container().instance(MongoClients.class).get();
      MongoClient mongoClient =
          mongoClients.createMongoClient(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME);

      MongoDatabaseClient client = new MongoDatabaseClient();
      client.configure(MongoClientConfig.of(mongoClient).withDatabaseName(databaseName));
      client.initialize();

      MongoDatabaseAdapter ma = new MongoDatabaseAdapter(cfg, client, worker);

      if (ma.diagnosticCheckGlobalPointer()) {
        out.printf("Tenant: %s OK%n", repo);
      } else {
        out.printf("Tenant: %s ERROR%n", repo);
      }
    }

    return 0;
  }
}
