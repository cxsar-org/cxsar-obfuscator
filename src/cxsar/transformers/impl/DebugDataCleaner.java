package cxsar.transformers.impl;

import cxsar.Cxsar;
import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;

@RegisterTransformer(name = "Debug Data Cleaner", enabled = true)
public class DebugDataCleaner implements ITransformer {
    @Override
    public void preTransform(Cxsar cxsar) {
        // NOTHING
    }

    @Override
    public void transform(Cxsar cxsar, ClassNode classNode) {
        classNode.methods.forEach(node -> node.instructions.forEach(abstractInsnNode -> {
            // REMOVE LINE NUMBERS
            if(abstractInsnNode instanceof LineNumberNode)
            {
                LineNumberNode lineNumberNode = (LineNumberNode) abstractInsnNode;
                node.instructions.remove(lineNumberNode);
            }
        }));

        // Remove the source file debug attribute
        classNode.sourceFile = null;
    }
}
