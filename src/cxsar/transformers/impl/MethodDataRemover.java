package cxsar.transformers.impl;

import cxsar.Cxsar;
import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.impl.name.Dictionary;
import org.objectweb.asm.tree.ClassNode;

@RegisterTransformer(name = "Method Data Remover", enabled = true)
public class MethodDataRemover implements ITransformer {
    @Override
    public void preTransform(Cxsar cxsar) {}

    @Override
    public void transform(Cxsar cxsar, ClassNode node) {
        // Rename paramaters and localVariables :D
        node.methods.forEach(methodNode -> {
            if (methodNode.parameters != null)
                methodNode.parameters.forEach(parameterNode -> parameterNode.name = Dictionary.getInstance().getNextAlphabetDictionaryEntry());

            if(methodNode.localVariables != null)
                methodNode.localVariables.forEach(localVariableNode -> localVariableNode.name = Dictionary.getInstance().getNextAlphabetDictionaryEntry());
        });
    }
}
