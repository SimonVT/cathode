package net.simonvt.cathode.remote;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
public @interface PriorityQueue {
}
