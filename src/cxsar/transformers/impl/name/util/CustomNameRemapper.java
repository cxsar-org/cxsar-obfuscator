package cxsar.transformers.impl.name.util;

import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;

/**
 * Simple implementation to map using our formatting
 * field formatting: [owner][name][descriptor]
 * method formatting: [owner][name][descriptor]
 */
public class CustomNameRemapper extends SimpleRemapper {
    public CustomNameRemapper(Map<String, String> mapping) {
        super(mapping);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String remappedName = map(owner + '.' + name);
        return (remappedName != null) ? remappedName : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String remappedName = map(owner + '.' + name + '.' + descriptor);
        return (remappedName != null) ? remappedName : name;
    }
}
