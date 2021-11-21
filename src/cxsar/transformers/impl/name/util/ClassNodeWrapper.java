package cxsar.transformers.impl.name.util;

import org.objectweb.asm.tree.ClassNode;

public class ClassNodeWrapper {

    // Class node
    public ClassNode node;

    // ClassEntry
    public ClassEntry entry;


    public ClassNodeWrapper(ClassNode node, ClassEntry entry)
    {
        this.node = node;
        this.entry = entry;
    }
}
