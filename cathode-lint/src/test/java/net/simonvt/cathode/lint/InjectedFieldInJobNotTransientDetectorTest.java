/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.lint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.TestFiles;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class InjectedFieldInJobNotTransientDetectorTest extends LintDetectorTest {

  private static final TestFile JAVAX_INJECT = TestFiles.java(""
      + "package javax.inject;\n"
      + "\n"
      + "import java.lang.annotation.Documented;\n"
      + "import java.lang.annotation.ElementType;\n"
      + "import java.lang.annotation.Retention;\n"
      + "import java.lang.annotation.RetentionPolicy;\n"
      + "import java.lang.annotation.Target;\n"
      + "\n"
      + "@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})\n"
      + "@Retention(RetentionPolicy.RUNTIME)\n"
      + "@Documented\n"
      + "public @interface Inject {\n"
      + "}");

  private static final TestFile JOB = TestFiles.java(""
      + "package net.simonvt.cathode.jobqueue;\n"
      + "\n"
      + "import javax.inject.Inject;\n"
      + "\n"
      + "public class Job {\n"
      + "\n"
      + "}\n");

  private static final TestFile BASE_JOB = TestFiles.java(""
      + "package net.simonvt.cathode.lint;\n"
      + "\n"
      + "import net.simonvt.cathode.jobqueue.Job;\n"
      + "\n"
      + "public class BaseJob extends Job {\n"
      + "}\n");

  private static final TestFile ERROR_JOB = TestFiles.java(""
      + "package net.simonvt.cathode.lint;\n"
      + "\n"
      + "import javax.inject.Inject;\n"
      + "import net.simonvt.cathode.lint.BaseJob;\n"
      + "\n"
      + "public class TestJob extends BaseJob {\n"
      + "\n"
      + "  @Inject Object app;\n"
      + "}\n");

  private static final TestFile OK_JOB = TestFiles.java(""
      + "package net.simonvt.cathode.lint;\n"
      + "\n"
      + "import javax.inject.Inject;\n"
      + "import net.simonvt.cathode.jobqueue.Job;\n"
      + "\n"
      + "public class TestJob extends Job {\n"
      + "\n"
      + "  @Inject transient Object app;\n"
      + "}\n");

  @Override protected Detector getDetector() {
    return new InjectedFieldInJobNotTransientDetector();
  }

  @Override protected List<Issue> getIssues() {
    return ImmutableList.of(InjectedFieldInJobNotTransientDetector.ISSUE);
  }

  public void testMissingTransient() throws Exception {
    assertNotSame("No warnings.", lintFiles(JAVAX_INJECT, JOB, BASE_JOB, ERROR_JOB));
    assertTrue(lintFiles(JAVAX_INJECT, JOB, BASE_JOB, ERROR_JOB).contains("1 errors, 0 warnings"));
  }

  public void testHasTransient() throws Exception {
    assertSame("No warnings.", lintFiles(JAVAX_INJECT, JOB, BASE_JOB, OK_JOB));
  }
}
