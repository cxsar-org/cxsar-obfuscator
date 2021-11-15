package cxsar.transformers;

public @interface RegisterTransformer {

    String name() default "None";
    boolean enabled() default false;

}
