package cxsar.transformers.impl.name.util;

import sun.reflect.generics.visitor.Visitor;

import java.util.ArrayList;

public class ClassEntry {

    // Name of the class
    String name;

    // Original name of class
    String originalName;

    // Parent class
    ClassEntry parentClass;

    // The parent package entry
    PackageEntry parent;

    // Potential sub classes
    ArrayList<ClassEntry> subClasses;

    public ClassEntry(String name) {
        this.name = name;
        this.originalName = name;
        this.subClasses = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PackageEntry getParent() {
        return parent;
    }

    public void setParent(PackageEntry parent) {
        this.parent = parent;
    }

    public boolean hasParentClass() {
        return this.parentClass != null;
    }

    public ClassEntry getParentClass() {
        return this.parentClass;
    }

    public void setParentClass(ClassEntry parentClass) {
        this.parentClass = parentClass;
    }

    public ArrayList<ClassEntry> getSubClasses() {
        return subClasses;
    }

    public void setSubClasses(ArrayList<ClassEntry> subClasses) {
        this.subClasses = subClasses;
    }
}
