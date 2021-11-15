package cxsar.transformers;

import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

public interface ITransformer {

    // Use this for ClassNames and such
    void preTransform(HashMap<String, ClassNode> classNodeHashMap);

    // Use this for actual class transformation
    void transform(HashMap<String, ClassNode> classNodeHashMap);

}
