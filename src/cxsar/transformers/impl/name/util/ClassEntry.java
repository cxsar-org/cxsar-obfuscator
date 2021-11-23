package cxsar.transformers.impl.name.util;

import cxsar.transformers.impl.name.Dictionary;
import cxsar.utils.Logger;
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

    // All used generated method names
    int generatedMethodNamesCount = 0;

    // All used generated field names
    int generatedFieldNamesCount = 0;

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

    public void setNameTransformed(String newName)
    {
        newName += ".class";
        if(this.parent.transformedNameAlreadyInUse(newName))
        {
            Logger.getInstance().log("Name %s already in use for package %s (skipping)", newName, this.parent.getFullPath());
            return;
        }

        this.setName(newName);
        this.parent.generatedNames.add(newName);
        this.parent.generatedNameCount++;
    }

    public String getOriginalFullPath() {
        StringBuilder nameBuilder = new StringBuilder();

        ClassEntry parent = this;
        do {
            if(parent != this)
                nameBuilder.insert(0, parent.originalName.substring(0, parent.originalName.length() - ".class".length()) + "$");
            else
                nameBuilder.insert(0, parent.originalName + "$");
            parent = parent.parentClass;
        } while(parent != null);

        nameBuilder.insert(0, this.parent.getOriginalFullPath());
        return nameBuilder.substring(1, nameBuilder.toString().length() - 1);
    }

    public String getFullPath() {
        StringBuilder nameBuilder = new StringBuilder();

        ClassEntry parent = this;

        do {
            if(parent != this)
                nameBuilder.insert(0, parent.getName().substring(0, parent.getName().length() - ".class".length()) + "$");
            else
                nameBuilder.insert(0, parent.getName() + "$");
            parent = parent.parentClass;
        } while(parent != null);

        nameBuilder.insert(0, this.parent.getFullPath());
        return nameBuilder.substring(1, nameBuilder.toString().length() - 1);
    }

    public String nextFieldName() {
        return Dictionary.getInstance().getGeneratedName(this.generatedFieldNamesCount++);
    }

    public String nextMethodName() {
        return Dictionary.getInstance().getGeneratedName(this.generatedMethodNamesCount++);
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
}
