package cxsar.transformers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
public @interface RegisterTransformer {

    String name() default "None";
    boolean enabled() default false;

}
