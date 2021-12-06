package cxsar.transformers.impl.name.util;

import cxsar.transformers.impl.name.Dictionary;
import cxsar.utils.Logger;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClassEntry {

    // Name of the class
    String name;

    // Original name of class
    String originalName;

    // Original node
    ClassNode node;

    // Parent class
    ClassEntry parentClass;

    // The parent package entry
    PackageEntry parent;

    // Potential sub classes
    ArrayList<ClassEntry> subClasses;

    // Mappings
    HashMap<String, String> methodMappings, fieldMappings;

    // All used generated method names
    int generatedMethodNamesCount = 0;

    // All used generated field names
    int generatedFieldNamesCount = 0;

    public ClassEntry(String name) {
        this.name = name;
        this.originalName = name;
        this.subClasses = new ArrayList<>();
        this.methodMappings = this.fieldMappings = new HashMap<>();
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

    public ClassNode getNode() {
        return node;
    }

    public void setNode(ClassNode node) {
        this.node = node;
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

    public void generateMethodAndFieldMapping(HashMap<String, String> dictionary) {
        String originalStringSubbed = this.getOriginalFullPath().replace(".class", "");
        String newNameSubbed = this.getFullPath().replace(".class", "");

//        if(node.methods != null)
//            node.methods.stream().filter(m -> !m.name.startsWith("<") && !m.name.equals("main") && !m.attrs).forEach(method -> methodMappings.put(String.format("%s.%s.%s", originalStringSubbed, method.name, method.desc), nextMethodName()));

//        if(node.fields != null)
//            node.fields.forEach(field -> fieldMappings.put(String.format("%s.%s", originalStringSubbed, field.name), nextFieldName()));

        methodMappings.forEach(dictionary::put);
        fieldMappings.forEach(dictionary::put);
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
        return Dictionary.getInstance().getGeneratedName(this.generatedMethodNamesCount += 2);
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
}
