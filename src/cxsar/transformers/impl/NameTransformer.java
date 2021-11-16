package cxsar.transformers.impl;

import cxsar.Cxsar;
import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.impl.name.generator.Dictionary;
import cxsar.transformers.impl.name.tree.EntryTree;
import cxsar.transformers.impl.name.tree.Hierarchy;
import cxsar.transformers.impl.name.tree.MemberRemapper;
import cxsar.transformers.utils.EntryWrapper;
import cxsar.utils.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@RegisterTransformer(name = "Name Transformer", enabled = true)
public class NameTransformer implements ITransformer {

    private final ArrayList<EntryWrapper> wrapperArrayList = new ArrayList<>();
    HashMap<String, String> generatedMappings;
    HashSet<EntryTree> visitedTrees;

    private Hierarchy hierarchy;
    private Dictionary dictionary;

    private int classCount = 0;

    @Override
    //TODO: Fix this transformer, for some reason, we get a bunch of errors with class not found
    //      I am pretty sure it's due to inner classes not being handeled properly
    public void preTransform(Cxsar ctx, HashMap<String, ClassNode> classNodeHashMap) {

        if(hierarchy == null)
            hierarchy = new Hierarchy(ctx);

        if(dictionary == null)
            dictionary = new Dictionary();

        generatedMappings = new HashMap<>();
        visitedTrees = new HashSet<>();

        // Build a hierarchy
        classNodeHashMap.values().forEach(classNode -> {
            EntryWrapper wrapper = new EntryWrapper(classNode, false);

            wrapperArrayList.add(wrapper);

            hierarchy.createHierarchy(wrapper, null);
        });

        wrapperArrayList.forEach(wrapper -> {

            EntryTree tree = hierarchy.findEntryTree(wrapper.getOriginalClassName());

            if(tree.subEntries.size() > 0)
                return;

            if(wrapper.getOriginalClassName().contains("Luyten") || wrapper.getOriginalClassName().contains("$"))
                return;

//            // Make sure fields & methods are accessible
//            wrapper.makeAccessible();
//
//            AtomicBoolean nativeMethodsPresent = new AtomicBoolean(false);
//
//            wrapper.getMethodWrapperArrayList()
//                    .stream()
//                    .filter(methodWrapper -> !methodWrapper.methodNode.name.contains("main") && !methodWrapper.methodNode.name.contains("premain") && !methodWrapper.methodNode.name.startsWith("<"))
//                    .forEach(methodWrapper ->  {
//                        // Do not rename native methods
//                        if(Modifier.isNative(methodWrapper.methodNode.access)) {
//                            nativeMethodsPresent.set(true);
//                        }
//
//                        // rename!
//                        // TODO: Generate mappings
//                        if(hierarchy.canRemapMethodTree(generatedMappings, new HashSet<>(), methodWrapper, wrapper.getOriginalClassName()))
//                            hierarchy.remapMethodTree(generatedMappings, new HashSet<>(), methodWrapper, wrapper.getOriginalClassName(), dictionary.retrieveMethodName());
//            });

//            wrapper.getFieldWrapperArrayList()
//                    .stream()
//                    .filter(fieldWrapper -> hierarchy.canRemapFieldTree(generatedMappings, new HashSet<>(), fieldWrapper, wrapper.getOriginalClassName()))
//                    .forEach(fieldWrapper -> hierarchy.remapFieldTree(generatedMappings, new HashSet<>(), fieldWrapper, wrapper.getOriginalClassName(), dictionary.retrieveFieldName()));

//            wrapper.getClassNode().access &= ~Opcodes.ACC_PRIVATE;
//            wrapper.getClassNode().access &= ~Opcodes.ACC_PROTECTED;
//            wrapper.getClassNode().access |= Opcodes.ACC_PUBLIC;

           // String newName = wrapper.getOriginalClassName().substring(0, wrapper.getOriginalClassName().lastIndexOf('/') + 1) + dictionary.retrieveClassName();

            String newName = dictionary.retrieveClassName(wrapper.getOriginalClassName());

            Logger.getInstance().log("Parsing classname: %s -> %s", wrapper.getOriginalClassName(), newName);

            generatedMappings.put(wrapper.getOriginalClassName(), newName);
            classCount++;
        });

        Remapper remapper = new MemberRemapper(generatedMappings);

        for (EntryWrapper wrapper : wrapperArrayList) {
            ClassNode classNode = wrapper.getClassNode();

            ClassNode copy = new ClassNode();
            classNode.accept(new ClassRemapper(copy, remapper));

            for (int i = 0; i < copy.methods.size(); ++i)
                wrapper.getMethodWrapperArrayList().get(i).methodNode = copy.methods.get(i);

            if (copy.fields != null)
                for (int i = 0; i < copy.fields.size(); ++i)
                    wrapper.getFieldWrapperArrayList().get(i).fieldNode = copy.fields.get(i);

            wrapper.setClassNode(copy);

            ctx.parsedClasses.remove(wrapper.getOriginalClassName() + ".class");
            ctx.parsedClasses.put(wrapper.getClassNode().name + ".class", wrapper.getClassNode());

            Logger.getInstance().log("Transformed %s to %s", wrapper.getOriginalClassName(), wrapper.getClassNode().name);

            ClassWriter writer = new ClassWriter(0);
            wrapper.getClassNode().accept(writer);
        }
    }

    @Override
    public void transform(HashMap<String, ClassNode> classNodeHashMap) {
        // the NameTransformer does not access this
    }
}
