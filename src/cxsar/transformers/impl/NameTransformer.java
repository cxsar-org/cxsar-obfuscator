package cxsar.transformers.impl;

import cxsar.Cxsar;
import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.TransformerPriority;
import cxsar.transformers.impl.name.Dictionary;
import cxsar.transformers.impl.name.util.ClassEntry;
import cxsar.transformers.impl.name.util.ClassNodeWrapper;
import cxsar.transformers.impl.name.util.CustomNameRemapper;
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

        // Debugging
        Dictionary.getInstance().excludePackageEntryAndSubsequentSubEntries(Dictionary.getInstance().getPackageTreeEntry("/org/fife"));

        // New mappings
        HashMap<String, String> generatedMappings = new HashMap<>();

        // Wrapper
        ArrayList<ClassNodeWrapper> wrapperArrayList = new ArrayList<>();

        // Keep track of generation time
        Timer timer = new Timer();

        // Iterate the entire package tree & generate names
        Dictionary.getInstance().getPackageTree().visit(entry -> {

            if(Dictionary.getInstance().isExcluded(entry))
                return false;

            // Main classes
            for(ClassEntry classEntry : entry.getClassEntries()) {
                classEntry.setNameTransformed(Dictionary.getInstance().getGeneratedName(entry.getGeneratedNameCount()));
                classEntry.generateMethodAndFieldMapping(generatedMappings);

                generatedMappings.put(classEntry.getOriginalFullPath().replace(".class", ""), classEntry.getFullPath().replace(".class", ""));


                for(ClassEntry subEntries : classEntry.getSubClasses()) {
                    subEntries.setNameTransformed(Dictionary.getInstance().getGeneratedName(entry.getGeneratedNameCount()));
                    subEntries.generateMethodAndFieldMapping(generatedMappings);

                    generatedMappings.put(subEntries.getOriginalFullPath().replace(".class", ""), subEntries.getFullPath().replace(".class", ""));
                }
            }

            // Keep on going baby
            return false;
        });

        //generatedMappings.keySet().forEach(s -> wrapperArrayList.add(new ClassNodeWrapper(cxsar.classPath.get(s), Dictionary.getInstance().findAnyClassEntry(s))));

        Logger.getInstance().log("Generated mappings in %dms", timer.end());

        Remapper remapper = new CustomNameRemapper(generatedMappings);
        HashMap<String, ClassNode> copyClassPath = (HashMap<String, ClassNode>) cxsar.classPath.clone();

        timer.begin();

        copyClassPath.forEach((s, classNode) -> {
            ClassNode copy = new ClassNode();
            classNode.accept(new ClassRemapper(copy, remapper));

            ClassWriter writer = new ClassWriter(0);
            copy.accept(writer);

            // Append .class
            String className = copy.name;
            className += ".class";

            cxsar.classPath.remove(s);
            cxsar.classPath.put(className, copy);
        });

        Logger.getInstance().log("Name transformation done in %dms", timer.end());
        Dictionary.getInstance().setUsedMappings(generatedMappings); // copy mappings to the dictionary
    }

    @Override
    public void transform(Cxsar cxsar, ClassNode node) {

    }
}
