/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.scalac.mesmer.otelextension;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class MesmerAkkaHttpServerInstrumentationModule extends InstrumentationModule {
  public MesmerAkkaHttpServerInstrumentationModule() {
    super("new-mesmer-akka-http", "new-mesmer-akka-http-server");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new MesmerHttpExtServerInstrumentation());
  }
}