package cxsar.transformers.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

// Wrapper for the ClassNode's in the classpath, contains useful tools
public class EntryWrapper {

    // The target classnode
    private ClassNode classNode;

    // All method wrappers
    private ArrayList<MethodWrapper> methodWrapperArrayList = new ArrayList<>();

    // All field wrappers
    private ArrayList<FieldWrapper> fieldWrapperArrayList = new ArrayList<>();

    // If it's a library class
    private boolean libraryClassNode;

    // Original classname
    private final String originalClassName;

    public EntryWrapper(ClassNode node, boolean lib) {
        this.classNode = node;
        this.libraryClassNode = lib;
        this.originalClassName = node.name;

        this.classNode.methods.forEach(methodNode -> methodWrapperArrayList.add(new MethodWrapper(this, methodNode)));
        this.classNode.fields.forEach(fieldNode -> fieldWrapperArrayList.add(new FieldWrapper(this, fieldNode)));
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

    // Make fields & methods accessible
    public void makeAccessible() {
        this.classNode.methods.forEach(methodNode -> {
            if(Modifier.isPrivate(methodNode.access) || Modifier.isProtected(methodNode.access))
                return;

            // Remove flags
            methodNode.access &= ~Opcodes.ACC_PRIVATE;
            methodNode.access &= ~Opcodes.ACC_PROTECTED;
            methodNode.access |= Opcodes.ACC_PUBLIC;
        });

        this.classNode.fields.forEach(fieldNode -> {
            if(Modifier.isPrivate(fieldNode.access) || Modifier.isProtected(fieldNode.access))
                return;

            // Remove flags
            fieldNode.access &= ~Opcodes.ACC_PRIVATE;
            fieldNode.access &= ~Opcodes.ACC_PROTECTED;
            fieldNode.access |= Opcodes.ACC_PUBLIC;
        });
    }

    public ArrayList<FieldWrapper> getFieldWrapperArrayList() {
        return fieldWrapperArrayList;
    }

    public ArrayList<MethodWrapper> getMethodWrapperArrayList() {
        return methodWrapperArrayList;
    }

    public String getOriginalClassName() {
        return originalClassName;
    }
}
