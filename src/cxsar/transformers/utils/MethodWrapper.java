package cxsar.transformers.utils;


import org.objectweb.asm.tree.MethodNode;

public class MethodWrapper {

    // Parent class
    public EntryWrapper entryWrapper;

    // Method node
    public MethodNode methodNode;

    // Other stuff
    public String originalName;
    public String originalDecription;

    // Constructor
    public MethodWrapper(EntryWrapper entry, MethodNode node)
    {
        this.entryWrapper = entry;
        this.methodNode = node;

        this.originalDecription = node.desc;
        this.originalName = node.name;
    }
}
