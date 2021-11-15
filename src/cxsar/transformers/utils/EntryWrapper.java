package cxsar.transformers.utils;

import jdk.internal.org.objectweb.asm.tree.ClassNode;

// Wrapper for the ClassNode's in the classpath, contains useful tools
public class EntryWrapper {

    // The target classnode
    private ClassNode classNode;

    // If it's a library class
    private boolean libraryClassNode;

    // Parse methods & fields
    // TODO: ^^^^

    public EntryWrapper(ClassNode node, boolean lib) {
        this.classNode = node;
        this.libraryClassNode = lib;
    }

    public EntryWrapper(ClassNode node) {
        this(node, false);
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }

    public boolean isLibraryClassNode() {
        return libraryClassNode;
    }

    public void setLibraryClassNode(boolean libraryClassNode) {
        this.libraryClassNode = libraryClassNode;
    }

}
