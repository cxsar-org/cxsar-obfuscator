package cxsar.transformers.impl.name;

import cxsar.Cxsar;
import cxsar.transformers.impl.name.util.ClassEntry;
import cxsar.transformers.impl.name.util.PackageEntry;
import cxsar.transformers.impl.name.util.Visitor;
import cxsar.utils.Logger;
import cxsar.utils.Timer;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;


// Generate dictionary for the classpath
public class Dictionary {

    // Singleton stuff
    private static Dictionary instance = new Dictionary();

    // Keep track of packages and their used names, since they can't be identical
    private PackageEntry packageTree;

    public static Dictionary getInstance() {
        return instance;
    }

    // Generate a dictionary for the classpath in context
    public void generateDictionary(Cxsar cxsar) {

        // Add the base to the package tree
        packageTree = new PackageEntry("/");

        cxsar.classPath.forEach((path, classNode) -> {
            path = "/" + path; // "/" is the first package entry

            if(!isPackageTreePresent(path))
                createPath(path);

            getPackageTreeEntry(path);
        });

        AtomicInteger integer = new AtomicInteger(0);
        packageTree.visit(entry -> {
                    integer.addAndGet(entry.getClassEntries().size());
                    return false;
                });

        Logger.getInstance().log("%d entries\n", integer.get());

        // Fix innerClasses
        Timer timer = new Timer();

        fixInnerClasses();

        Logger.getInstance().log("Fixed inner classes in %dms", timer.end());
    }

    // Fix inner classes
    public void fixInnerClasses() {
        packageTree.visit(packageEntry -> {
            Iterator<ClassEntry> classEntryIterator = packageEntry.getClassEntries().iterator();

            while(classEntryIterator.hasNext())
            {
                ClassEntry entry = classEntryIterator.next();

                if(entry.getName().contains("$"))
                {
                    // get the name of the parent
                    String[] split = entry.getName().split("\\$");
                    String parentName = split[0].substring(split[0].lastIndexOf('/') + 1);

                    // Find the parent class and add the current entry to the sub classes
                    ClassEntry parentEntry = packageEntry.findClassEntry(parentName + ".class");

                    if(parentEntry != null)
                        parentEntry.getSubClasses().add(entry);

                    Logger.getInstance().log("Added %s as subclas to %s", entry.getName(), parentName);

                    // Remove original sub class from the list
                    classEntryIterator.remove();
                }
            }

            return false;
        });
    }

    // Create a path
    public void createPath(String path) {
        String[] entries = path.split("/");

        StringBuilder currentPath = new StringBuilder();
        currentPath.append('/');

        // Current entry
        PackageEntry currentEntry = packageTree;

        // Split the first one
        for(int i = 1; i < entries.length; ++i)
        {
            currentPath.append(entries[i]).append("/");

            if(entries[i].endsWith(".class"))
            {
                currentEntry.getClassEntries().add(new ClassEntry(entries[i]));
                break;
            }

            if(!isPackageTreePresent(currentPath.toString())) {
                PackageEntry newEntry = new PackageEntry(entries[i]);
                newEntry.setParentEntry(currentEntry);

                currentEntry.getSubEntries().add(newEntry);
                currentEntry = newEntry;
            } else {
                currentEntry = getPackageTreeEntry(currentPath.toString());
            }
        }
    }

    // Exists
    public PackageEntry getPackageTreeEntry(String path) {
        // is in the base class?
        if(path.equals(packageTree.getName()))
            return null;

        // First entry
        PackageEntry packageEntry = packageTree;

        String[] split = path.split("/");

        for(int i = 1; i < split.length; ++i)
        {
            if(!split[i].endsWith(".class")) {
                PackageEntry subEntry = packageEntry.findSubEntry(split[i]);

                if(subEntry == null)
                    return null;

                packageEntry = subEntry;
            } else {
                if(packageEntry.findClassEntry(split[i]) == null) {
                    packageEntry = packageTree;
                }
            }

        }

        return packageEntry == packageTree ? null : packageEntry;
    }

    // Does the current tree contain the package entry
    public boolean isPackageTreePresent(String path)
    {
        PackageEntry entry = this.getPackageTreeEntry(path);

        return entry != null;
    }

    // Get the package name of the entry
    public String getPackageOfEntry(String fullPath) {
        // So this would return something like
        // cxsar/main
        return fullPath.substring(0, fullPath.lastIndexOf('/'));
    }
}
