package cxsar.transformers.impl.name.util;

import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;

/**
 * Simple implementation to map using our formatting
 * field formatting: [owner][f][name][descriptor]
 * method formatting: [owner][m][name][descriptor]
 */
public class CustomNameRemapper extends SimpleRemapper {
    public CustomNameRemapper(Map<String, String> mapping) {
        super(mapping);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String remappedName = map(owner + ':' + 'F' + ':' + name + ':' + descriptor);
        return (remappedName != null) ? remappedName : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String remappedName = map(owner + ':' + 'M' + ':' + name + ':' + descriptor);
        return (remappedName != null) ? remappedName : name;
    }
}
