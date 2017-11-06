package net.simonvt.jobqueue.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import dagger.Binds;
import dagger.Module;
import dagger.Subcomponent;
import dagger.multibindings.IntoMap;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class JobProcessor extends AbstractProcessor {

  private static final ClassName JOB = ClassName.get("net.simonvt.cathode.jobqueue", "Job");

  private static final ClassName ANDROID_INJECTOR =
      ClassName.get("dagger.android", "AndroidInjector");
  private static final ClassName ANDROID_INJECTOR_BUILDER =
      ClassName.get("dagger.android", "AndroidInjector", "Builder");
  private static final ClassName ANDROID_INJECTOR_FACTORY =
      ClassName.get("dagger.android", "AndroidInjector", "Factory");

  private Elements elements;
  private Filer filer;

  private List<String> parsedJobs = new ArrayList<>();

  private List<JobSubcomponent> subcomponents = new ArrayList<>();

  @Override public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    elements = env.getElementUtils();
    filer = env.getFiler();
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton("*");
  }

  static class JobSubcomponent {

    ClassName jobClass;
    ClassName subcomponent;
    ClassName builder;

    JobSubcomponent(ClassName jobClass, ClassName subcomponent, ClassName builder) {
      this.jobClass = jobClass;
      this.subcomponent = subcomponent;
      this.builder = builder;
    }
  }

  @Override public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {

    // Get all classes that extend Job and is not abstract.
    Set<Element> jobs = env.getRootElements()
        .stream()
        .filter(e -> e.getKind().isClass())
        .filter(e -> !e.getModifiers().contains(Modifier.ABSTRACT))
        .filter(this::isJob)
        .filter(e -> !parsedJobs.contains(e.asType().toString()))
        .collect(Collectors.toCollection(HashSet::new));

    for (Element job : jobs) {
      parsedJobs.add(job.asType().toString());
      try {
        String packageName = getPackageName((TypeElement) job);
        String className = job.getSimpleName().toString();

        String subcomponentName = className + "Subcomponent";

        ClassName jobClass = ClassName.get(packageName, className);
        ClassName subcomponent = ClassName.get(packageName, subcomponentName);
        ClassName subcomponentBuilder = ClassName.get(packageName, subcomponentName, "Builder");

        subcomponents.add(new JobSubcomponent(jobClass, subcomponent, subcomponentBuilder));

        ParameterizedTypeName androidInjectorBuilder =
            ParameterizedTypeName.get(ANDROID_INJECTOR_BUILDER, ClassName.get(job.asType()));
        ParameterizedTypeName androidInjector =
            ParameterizedTypeName.get(ANDROID_INJECTOR, ClassName.get(job.asType()));

        // Create subcomponent builder
        TypeSpec.Builder jobSubcomponentBuilder = TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT);
        jobSubcomponentBuilder.superclass(androidInjectorBuilder);
        jobSubcomponentBuilder.addAnnotation(Subcomponent.Builder.class);

        // Create subcomponent
        TypeSpec.Builder jobSubcomponent =
            TypeSpec.interfaceBuilder(subcomponentName).addModifiers(Modifier.PUBLIC);
        jobSubcomponent.addSuperinterface(androidInjector);
        jobSubcomponent.addAnnotation(Subcomponent.class);
        jobSubcomponent.addType(jobSubcomponentBuilder.build());

        // Write to file
        JavaFile javaFile = JavaFile.builder(packageName, jobSubcomponent.build()).build();
        JavaFileObject jfo = filer.createSourceFile(packageName + "." + subcomponentName);
        Writer out = jfo.openWriter();
        javaFile.writeTo(out);
        out.flush();
        out.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (env.processingOver()) {
      try {
        // Create module
        final String moduleName = "JobModule";

        TypeSpec.Builder jobModule =
            TypeSpec.classBuilder(moduleName).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

        // Add Module annotation to JobModule
        CodeBlock.Builder jobBlock = CodeBlock.builder();
        for (JobSubcomponent subcomponent : subcomponents) {
          if (!jobBlock.isEmpty()) {
            jobBlock.add(", ");
          } else {
            jobBlock.add("{");
          }

          jobBlock.add("$T.class", subcomponent.subcomponent);
        }
        jobBlock.add("}");

        AnnotationSpec moduleSpec = AnnotationSpec.builder(Module.class)
            .addMember("subcomponents", jobBlock.build())
            .build();
        jobModule.addAnnotation(moduleSpec);

        // Create bind method for each subcomponent
        for (JobSubcomponent subcomponent : subcomponents) {
          ParameterizedTypeName androidInjectorFactory =
              ParameterizedTypeName.get(ANDROID_INJECTOR_FACTORY, WildcardTypeName.subtypeOf(JOB));
          MethodSpec bindMethod =
              MethodSpec.methodBuilder("bind" + subcomponent.subcomponent.simpleName())
                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                  .addAnnotation(Binds.class)
                  .addAnnotation(IntoMap.class)
                  .addAnnotation(AnnotationSpec.builder(
                      ClassName.get("net.simonvt.cathode.jobqueue", "JobKey"))
                      .addMember("value", "$T.class", subcomponent.jobClass)
                      .build())
                  .addParameter(subcomponent.builder, "builder")
                  .returns(androidInjectorFactory)
                  .build();
          jobModule.addMethod(bindMethod);
        }

        // Write to file
        JavaFile javaFile =
            JavaFile.builder("net.simonvt.cathode.remote", jobModule.build()).build();
        JavaFileObject jfo =
            filer.createSourceFile("net.simonvt.cathode.remote" + "." + moduleName);
        Writer out = jfo.openWriter();
        javaFile.writeTo(out);
        out.flush();
        out.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return false;
  }

  private boolean isJob(Element element) {
    TypeMirror superType = ((TypeElement) element).getSuperclass();
    while (!(superType instanceof NoType)) {
      Element superTypeElement = ((DeclaredType) superType).asElement();
      if (JOB.equals(ClassName.get(superTypeElement.asType()))) {
        return true;
      }

      superType = ((TypeElement) superTypeElement).getSuperclass();
    }

    return false;
  }

  private String getPackageName(TypeElement type) {
    return elements.getPackageOf(type).getQualifiedName().toString();
  }
}
