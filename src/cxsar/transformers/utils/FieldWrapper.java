package cxsar.transformers.utils;


import org.objectweb.asm.tree.FieldNode;

public class FieldWrapper {

    // Parent class
    public EntryWrapper entryWrapper;

    // Method node
    public FieldNode fieldNode;

    // Other stuff
    public String originalName;
    public String originalDecription;

    // Constructor
    public FieldWrapper(EntryWrapper entry, FieldNode node)
    {
        this.entryWrapper = entry;
        this.fieldNode = node;

        this.originalDecription = node.desc;
        this.originalName = node.name;
    }
}
