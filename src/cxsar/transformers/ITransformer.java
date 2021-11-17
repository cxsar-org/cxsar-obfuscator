package cxsar.transformers;

import cxsar.Cxsar;

public interface ITransformer {
    // Pre file transformation, use this for naming conventions
    void preTransform(Cxsar cxsar);

    // File transformation to change actual bytecode
    void transform(Cxsar cxsar);
}
