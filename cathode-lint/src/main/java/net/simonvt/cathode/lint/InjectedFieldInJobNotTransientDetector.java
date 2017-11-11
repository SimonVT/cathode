package net.simonvt.cathode.lint;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

public final class InjectedFieldInJobNotTransientDetector extends Detector
    implements Detector.UastScanner {

  private static final String ISSUE_ID = "InjectedFieldInJobNotTransientDetector";
  private static final String LINT_ERROR_TITLE = "Injected field not transient";
  private static final String LINT_ERROR_BODY =
      "Injected fields in Job subclasses must be transient.";
  private static final String LINT_ERROR_MESSAGE = "Injected field must be transient";

  private static final String JOBQUEUE_JOB = "net.simonvt.cathode.jobqueue.Job";
  private static final String JAVAX_INJECT = "javax.inject.Inject";

  static final Issue ISSUE =
      Issue.create(ISSUE_ID, LINT_ERROR_TITLE, LINT_ERROR_BODY, Category.CORRECTNESS, 10,
          Severity.ERROR,
          new Implementation(InjectedFieldInJobNotTransientDetector.class, Scope.JAVA_FILE_SCOPE));

  @Override public List<Class<? extends UElement>> getApplicableUastTypes() {
    return Collections.singletonList(UClass.class);
  }

  @Override public UElementHandler createUastHandler(final JavaContext context) {
    return new UElementHandler() {
      @Override public void visitClass(UClass node) {
        node.accept(new JobVisitor(context));
      }
    };
  }

  private static class JobVisitor extends AbstractUastVisitor {
    private final JavaContext context;

    JobVisitor(JavaContext context) {
      this.context = context;
    }

    private boolean isInJob(PsiField field) {
      JavaEvaluator evaluator = context.getEvaluator();
      if (evaluator.isMemberInSubClassOf(field, JOBQUEUE_JOB, true)) {
        return true;
      }

      return false;
    }

    private boolean hasAnnotation(PsiModifierList modifierList, String annotationName) {

      PsiAnnotation[] annotations = modifierList.getAnnotations();
      if (annotations != null && annotations.length > 0) {
        for (PsiAnnotation annotation : annotations) {
          if (annotationName.equals(annotation.getQualifiedName())) {
            return true;
          }
        }
      }

      return false;
    }

    @Override public boolean visitField(UField field) {
      PsiModifierList modifierList = field.getModifierList();
      if (modifierList == null || modifierList.hasModifierProperty(PsiModifier.TRANSIENT)) {
        return false;
      }

      if (!isInJob(field)) {
        return false;
      }

      if (hasAnnotation(modifierList, JAVAX_INJECT)) {
        context.report(ISSUE, context.getLocation(field), LINT_ERROR_MESSAGE);
      }

      return super.visitField(field);
    }
  }
}
