package cxsar.transformers.impl.name.tree;

import cxsar.Cxsar;
import cxsar.transformers.utils.EntryWrapper;
import cxsar.transformers.utils.FieldWrapper;
import cxsar.transformers.utils.MethodWrapper;
import cxsar.utils.Logger;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Hierarchy {

    private HashMap<String, EntryTree> hierarchyMap;

    private Cxsar cxsarContext;

    public Hierarchy(Cxsar ctx) {
        // create the hierarchy map
        this.hierarchyMap = new HashMap<>();

        // set the context
        this.cxsarContext = ctx;
    }

    public HashMap<String, EntryTree> getHierarchyMap() {
        return hierarchyMap;
    }

    public EntryTree findEntryTree(String name) {
        if(!hierarchyMap.containsKey(name))
        {
            EntryWrapper wrapper = this.cxsarContext.classPath.get(name);

            if(wrapper == null)
                return null;

            this.createHierarchy(wrapper, null);
        }

        return hierarchyMap.get(name);
    }

    public void createHierarchy(EntryWrapper wrapper, EntryWrapper sub) {
        // Check if this key isn't in the hierarchy yet
        if(!hierarchyMap.containsKey(wrapper.getClassNode().name))
        {
            // If not create a new tree
            EntryTree entryTree = new EntryTree(wrapper);

            // If this class has super classes
            if(wrapper.getClassNode().superName != null)
            {
                // Add it to the parents
                entryTree.parentEntries.add(wrapper.getClassNode().superName);

                EntryWrapper superClass = this.cxsarContext.classPath.get(wrapper.getClassNode().superName);

                if (superClass != null) {
                    createHierarchy(superClass, wrapper);
                }
                //Logger.getInstance().handleException(new RuntimeException(String.format("Unable to _SUPER_ find class %s in the classpath", wrapper.getClassNode().superName)));


            }

            // Do the same for any inherited interfaces
            if(wrapper.getClassNode().interfaces != null && !wrapper.getClassNode().interfaces.isEmpty())
            {
                wrapper.getClassNode().interfaces.forEach(s -> {
                    entryTree.parentEntries.add(s);

                    EntryWrapper superClass = this.cxsarContext.classPath.get(s);

                    if (superClass != null) {
                        createHierarchy(superClass, wrapper);
                    }
                    //Logger.getInstance().handleException(new RuntimeException(String.format("Unable to find class %s in the classpath", wrapper.getClassNode().superName)));


                });
            }

            hierarchyMap.put(wrapper.getClassNode().name, entryTree);
        }

        // Add subclasses
        if(sub != null)
            hierarchyMap.get(wrapper.getClassNode().name).subEntries.add(sub.getClassNode().name);
    }

    public void remapMethodTree(HashMap<String, String> mappings, HashSet<EntryTree> transformed, MethodWrapper wrapper, String className, String generatedName) {
        EntryTree tree = this.findEntryTree(className);

        if(tree == null)
            Logger.getInstance().handleException(new RuntimeException("Somehow this entry doesn't even exist and I don't know why..."));

        // If it is a library don't remap it      or if it's already transformed
        if(!tree.entryWrapper.isLibraryClassNode() && !transformed.contains(tree))
        {
            // Add it to the mappings
            mappings.put(className + '.' + wrapper.originalName + wrapper.originalDecription, generatedName);

            // Set flag
            transformed.add(tree);

            // Also rename for parents & subs
            tree.parentEntries.forEach(s -> this.remapMethodTree(mappings, transformed, wrapper, s, generatedName));
            tree.subEntries.forEach(s -> this.remapMethodTree(mappings, transformed, wrapper, s, generatedName));
        }
    }

    public void remapFieldTree(HashMap<String, String> mappings, HashSet<EntryTree> transformed, FieldWrapper fieldWrapper, String className, String generatedName) {
        EntryTree tree = this.findEntryTree(className);

        if(tree == null)
            Logger.getInstance().handleException(new RuntimeException("Somehow this entry doesn't even exist and I don't know why..."));

        if(!tree.entryWrapper.isLibraryClassNode() && !transformed.contains(tree)) {
            // Add it to the mappings
            mappings.put(className + '.' + fieldWrapper.originalName + '.' + fieldWrapper.originalDecription, generatedName);

            // Add it to the transformed list
            transformed.add(tree);

            // Also rename for parents & subs
            tree.parentEntries.forEach(s -> this.remapFieldTree(mappings, transformed, fieldWrapper, s, generatedName));
            tree.subEntries.forEach(s -> this.remapFieldTree(mappings, transformed, fieldWrapper, s, generatedName));
        }
    }

    public boolean canRemapFieldTree(HashMap<String, String> mappings, HashSet<EntryTree> transformed, FieldWrapper wrapper, String owner)
    {
        EntryTree tree = this.findEntryTree(owner);

        if(tree == null)
            return false;

        if(transformed.contains(tree))
            return false;

        transformed.add(tree); // make sure to add it

        if(mappings.containsKey(owner + '.' + wrapper.originalName + wrapper.originalDecription))
            return false;

        if(wrapper.entryWrapper.getOriginalClassName().equals(owner) && tree.entryWrapper.isLibraryClassNode())
        {
            for (FieldNode fieldNode : tree.entryWrapper.getClassNode().fields) {
                if (wrapper.originalName.equals(fieldNode.name) && wrapper.originalDecription.equals(fieldNode.desc))
                    continue;

                return false;
            }
        }

        for(String parent : tree.parentEntries) {
            if (parent != null && !canRemapFieldTree(mappings, transformed, wrapper, parent))
                return false;
        }

        for(String sub : tree.subEntries) {
            if(sub != null && !canRemapFieldTree(mappings, transformed, wrapper, sub))
                return false;
        }

        return true;
    }

    public boolean canRemapMethodTree(HashMap<String, String> mappings, HashSet<EntryTree> transformed, MethodWrapper wrapper, String owner)
    {
        EntryTree tree = this.findEntryTree(owner);

        if(tree == null)
            return false;

        if(transformed.contains(tree))
            return false;

        transformed.add(tree); // make sure to add it

        if(Modifier.isNative(wrapper.methodNode.access))
            return false;

        if(mappings.containsKey(owner + '.' + wrapper.originalName + wrapper.originalDecription))
            return false;

        if(wrapper.originalName.contains("main"))
            return false;

        if (!wrapper.entryWrapper.getOriginalClassName().equals(owner) && tree.entryWrapper.isLibraryClassNode()) {
            for (MethodNode methodNode : tree.entryWrapper.getClassNode().methods) {
                if (wrapper.originalName.equals(methodNode.name) && wrapper.originalDecription.equals(methodNode.desc))
                    continue;

                return false;
            }
        }

        for(String parent : tree.parentEntries) {
            if (parent != null && !canRemapMethodTree(mappings, transformed, wrapper, parent))
                return false;
        }

        for(String sub : tree.subEntries) {
            if(sub != null && !canRemapMethodTree(mappings, transformed, wrapper, sub))
                return false;
        }

        return true;
    }
}
