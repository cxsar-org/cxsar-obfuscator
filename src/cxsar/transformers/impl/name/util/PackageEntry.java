package cxsar.transformers.impl.name.util;

import java.util.ArrayList;

public class PackageEntry {
    // Package entry name
    String name;

    // Original package name
    String originalPackageName;

    // Potential parent
    PackageEntry parentEntry = null;

    // Sub entries
    ArrayList<PackageEntry> subEntries;

    // Get classEntry
    ArrayList<ClassEntry> classEntries;

    // Getters & setters
    public PackageEntry(String name) {
        this.name = name;
        this.originalPackageName = name;


        classEntries = new ArrayList<>();
        subEntries = new ArrayList<>();
    }

    public String getFullPath() {
        StringBuilder stringBuilder = new StringBuilder();

        PackageEntry entry = this;

        do {
            stringBuilder.insert(0, entry.name + "/");

            entry = entry.parentEntry;
        } while (entry != null);

        return stringBuilder.substring(1);
    }

    public PackageEntry findSubEntry(String name) {
        for(PackageEntry subEntry : this.subEntries)
            if(subEntry.originalPackageName.equals(name))
                return subEntry;

            return null;
    }

    public ClassEntry findClassEntry(String className) {
        for(ClassEntry classEntry : this.classEntries)
            if(classEntry.originalName.equals(className))
                return classEntry;

            return null;
    }

    public ArrayList<ClassEntry> getClassEntries() {
        return classEntries;
    }

    public void setClassEntries(ArrayList<ClassEntry> classEntries) {
        this.classEntries = classEntries;
    }

    public boolean hasParent() {
        return this.parentEntry != null;
    }

    public PackageEntry getParentEntry() {
        return this.parentEntry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParentEntry(PackageEntry parentEntry) {
        this.parentEntry = parentEntry;
    }

    public ArrayList<PackageEntry> getSubEntries() {
        return subEntries;
    }

    public void setSubEntries(ArrayList<PackageEntry> subEntries) {
        this.subEntries = subEntries;
    }

    public boolean visit(Visitor visitor) {
        if(visitor.accept(this))
            return true;

        for(PackageEntry entry : this.subEntries)
            if(entry.visit(visitor))
                return true;

            return false;
    }
}
