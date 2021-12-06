package cxsar.transformers;

import cxsar.Cxsar;
import org.objectweb.asm.tree.ClassNode;

public interface ITransformer {
    // Pre file transformation, use this for naming conventions
    void preTransform(Cxsar cxsar);

    // File transformation to change actual bytecode
    void transform(Cxsar cxsar, ClassNode node);
}
