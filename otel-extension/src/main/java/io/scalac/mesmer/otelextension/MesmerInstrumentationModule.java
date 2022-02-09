package io.scalac.mesmer.otelextension;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class MesmerInstrumentationModule extends InstrumentationModule {
  public MesmerInstrumentationModule() {
    super("mesmer-akka");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new AkkaHttpRequestsTypeInstrumentation());
  }
}