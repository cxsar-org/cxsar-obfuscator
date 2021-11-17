package cxsar.transformers.impl;

import cxsar.Cxsar;
import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.TransformerPriority;
import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

@RegisterTransformer(priority = TransformerPriority.FORCE, name = "NameTransformer", enabled = true)
public class NameTransformer implements ITransformer {
    @Override
    public void preTransform(Cxsar cxsar) {

    }

    @Override
    public void transform(Cxsar cxsar) {

    }
}
