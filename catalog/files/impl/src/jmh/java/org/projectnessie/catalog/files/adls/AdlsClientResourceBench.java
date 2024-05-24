/*
 * Copyright (C) 2024 Dremio
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
package org.projectnessie.catalog.files.adls;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.projectnessie.catalog.files.BenchUtils.mockServer;

import com.azure.core.http.HttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.projectnessie.objectstoragemock.ObjectStorageMock;

/** Microbenchmark to identify the resource footprint when using {@link AdlsClientSupplier}. */
@Warmup(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = MILLISECONDS)
@Fork(1)
@Threads(4)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MICROSECONDS)
public class AdlsClientResourceBench {
  @State(Scope.Benchmark)
  public static class BenchmarkParam {
    ObjectStorageMock.MockServer server;

    AdlsClientSupplier clientSupplier;

    @Setup
    public void init() {
      server = mockServer(mock -> {});

      AdlsConfig adlsConfig = AdlsConfig.builder().build();
      HttpClient httpClient = AdlsClients.buildSharedHttpClient(adlsConfig);

      AdlsOptions<AdlsFileSystemOptions> adlsOptions =
          AdlsProgrammaticOptions.builder()
              .accountKeyRef("foo")
              .accountNameRef("foo")
              .sasTokenRef("foo")
              .endpoint(server.getAdlsGen2BaseUri().toString())
              .build();

      clientSupplier = new AdlsClientSupplier(httpClient, adlsOptions, secret -> "secret");
    }

    @TearDown
    public void tearDown() throws Exception {
      server.close();
    }
  }

  @Benchmark
  public void adlsClient(BenchmarkParam param, Blackhole bh) {
    bh.consume(param.clientSupplier.fileClientForLocation(URI.create("abfs://bucket@bar/key")));
  }

  @Benchmark
  public void adlsGet(BenchmarkParam param, Blackhole bh) throws IOException {
    AdlsObjectIO objectIO = new AdlsObjectIO(param.clientSupplier);
    try (InputStream in = objectIO.readObject(URI.create("abfs://bucket@bar/key"))) {
      bh.consume(in.readAllBytes());
    }
  }

  @Benchmark
  public void adlsGet250k(BenchmarkParam param, Blackhole bh) throws IOException {
    AdlsObjectIO objectIO = new AdlsObjectIO(param.clientSupplier);
    try (InputStream in = objectIO.readObject(URI.create("abfs://bucket@bar/s-256000"))) {
      bh.consume(in.readAllBytes());
    }
  }

  @Benchmark
  public void adlsGet4M(BenchmarkParam param, Blackhole bh) throws IOException {
    AdlsObjectIO objectIO = new AdlsObjectIO(param.clientSupplier);
    try (InputStream in = objectIO.readObject(URI.create("abfs://bucket@bar/s-4194304"))) {
      bh.consume(in.readAllBytes());
    }
  }
}
