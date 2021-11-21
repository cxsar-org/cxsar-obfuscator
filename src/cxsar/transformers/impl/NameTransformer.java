package cxsar.transformers.impl;

import cxsar.Cxsar;
import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.TransformerPriority;
import cxsar.transformers.impl.name.Dictionary;
import cxsar.transformers.impl.name.util.ClassEntry;
import cxsar.transformers.impl.name.util.ClassNodeWrapper;
import cxsar.utils.Logger;
import cxsar.utils.Timer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;


@RegisterTransformer(priority = TransformerPriority.FORCE, name = "NameTransformer", enabled = true)
public class NameTransformer implements ITransformer {
    @Override
    public void preTransform(Cxsar cxsar) {
        // Generate dictionary for the current context (and hierarchy)
        Dictionary.getInstance().generateDictionary(cxsar);

        // New mappings
        HashMap<String, String> generatedMappings = new HashMap<>();

        // Wrapper
        ArrayList<ClassNodeWrapper> wrapperArrayList = new ArrayList<>();

        // Keep track of generation time
        Timer timer = new Timer();

        // Iterate the entire package tree & generate names
        Dictionary.getInstance().getPackageTree().visit(entry -> {

            // Main classes
            for(ClassEntry classEntry : entry.getClassEntries()) {
                classEntry.setNameTransformed(Dictionary.getInstance().getGeneratedName(entry.getGeneratedNameCount()));

                generatedMappings.put(classEntry.getOriginalFullPath(), classEntry.getFullPath());

                for(ClassEntry subEntries : classEntry.getSubClasses()) {
                    subEntries.setNameTransformed(Dictionary.getInstance().getGeneratedName(entry.getGeneratedNameCount()));

                    generatedMappings.put(subEntries.getOriginalFullPath(), subEntries.getFullPath());
                }
            }

            // Keep on going baby
            return false;
        });

        generatedMappings.keySet().forEach(s -> wrapperArrayList.add(new ClassNodeWrapper(cxsar.classPath.get(s), Dictionary.getInstance().findAnyClassEntry(s))));

        Logger.getInstance().log("Generated mappings in %dms", timer.end());

        Remapper remapper = new SimpleRemapper(generatedMappings);

        HashMap<String, ClassNode> copyClassPath = (HashMap<String, ClassNode>) cxsar.classPath.clone();

        for(ClassNodeWrapper wrapper : wrapperArrayList)
        {
            ClassNode node = wrapper.node;

            ClassNode copy = new ClassNode();
            node.accept(new ClassRemapper(copy, remapper));

            wrapper.node = copy;

            cxsar.classPath.remove(wrapper.entry.getOriginalFullPath());
            cxsar.classPath.put(wrapper.entry.getFullPath(), wrapper.node);

            Logger.getInstance().log("Transformed %s to %s", wrapper.entry.getOriginalFullPath(), wrapper.node.name);

            ClassWriter writer = new ClassWriter(0);
            wrapper.node.accept(writer);
        }
    }

    @Override
    public void transform(Cxsar cxsar) {

    }
}
