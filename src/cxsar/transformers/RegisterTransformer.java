package cxsar.transformers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterTransformer {

    String name() default "Transformer";
    boolean enabled() default false;
    TransformerPriority priority() default TransformerPriority.NORMAL;

}
