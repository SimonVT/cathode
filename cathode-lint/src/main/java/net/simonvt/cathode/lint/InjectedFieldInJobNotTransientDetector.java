package net.simonvt.cathode.lint;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import java.util.Collections;
import java.util.List;

public final class InjectedFieldInJobNotTransientDetector extends Detector
    implements Detector.JavaPsiScanner {

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

  @Override public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
    return Collections.<Class<? extends PsiElement>>singletonList(PsiClass.class);
  }

  @Override public JavaElementVisitor createPsiVisitor(final JavaContext context) {
    return new JavaElementVisitor() {
      @Override public void visitClass(PsiClass node) {
        node.accept(new R2UsageVisitor(context));
      }
    };
  }

  private static class R2UsageVisitor extends JavaRecursiveElementVisitor {
    private final JavaContext context;

    R2UsageVisitor(JavaContext context) {
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

    @Override public void visitField(PsiField field) {
      PsiModifierList modifierList = field.getModifierList();
      if (modifierList == null || modifierList.hasModifierProperty(PsiModifier.TRANSIENT)) {
        return;
      }

      if (!isInJob(field)) {
        return;
      }

      if (hasAnnotation(modifierList, JAVAX_INJECT)) {
        context.report(ISSUE, context.getLocation(field), LINT_ERROR_MESSAGE);
      }

      super.visitField(field);
    }
  }
}