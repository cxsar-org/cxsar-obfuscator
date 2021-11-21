package cxsar.transformers.impl.name;

import cxsar.Cxsar;
import cxsar.transformers.impl.name.util.ClassEntry;
import cxsar.transformers.impl.name.util.PackageEntry;
import cxsar.utils.Logger;
import cxsar.utils.Timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


// Generate dictionary for the classpath
public class Dictionary {

    // Singleton stuff
    private static Dictionary instance = new Dictionary();

    // List of classNames to use
    private List<String> classNames = new ArrayList<>();

    // Keep track of packages and their used names, since they can't be identical
    private PackageEntry packageTree;

    public static Dictionary getInstance() {
        return instance;
    }

    // Alphabet string (lol!)
    public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

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

        // Fix innerClasses
        Timer timer = new Timer();

        fixInnerClasses();

        Logger.getInstance().log("Fixed inner classes in %dms", timer.end());

        packageTree.visit(packageEntry -> {
           for(ClassEntry entry : packageEntry.getClassEntries())
           {
               Logger.getInstance().log("Full class: %s", entry.getFullPath());
               for(ClassEntry subEntry : entry.getSubClasses())
                   Logger.getInstance().log("Full sub-class path: %s", subEntry.getFullPath());
           }

           return false;
        });

        // list of threads
        List<Thread> threadList = new ArrayList<>();

        // random
        Random random = new Random();

        for(int i = 0; i < 5; ++i)
            threadList.add(new Thread(() -> {
                while(true) {
                    synchronized (classNames) {
                        if(classNames.size() >= 1000)
                            break;

                        StringBuilder builder = new StringBuilder();
                        for(int j = 0; j < 10; j++)
                            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));

                        classNames.add(builder.toString());
                    }
                }
            }));

        threadList.forEach(Thread::start);

        try {
            for (Thread thread : threadList) {
                thread.join();
            }
        } catch (Exception e) {
            Logger.getInstance().handleException(e);
        }
    }

    // Find any classentry
    public ClassEntry findAnyClassEntry(String path)
    {
        final ClassEntry[] res = new ClassEntry[1];
        packageTree.visit(entry -> {
            for(ClassEntry classEntry : entry.getClassEntries())
            {
                if(classEntry.getOriginalFullPath().equals(path))
                {
                    res[0] = classEntry;
                    return true;
                }

                for(ClassEntry sub : classEntry.getSubClasses())
                {
                    if(sub.getOriginalFullPath().equals(path))
                    {
                        res[0] = classEntry;
                        return true;
                    }
                }
            }

            return false;
        });

        return res[0];
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

                    // Set the parent class :D
                    entry.setParentClass(parentEntry);

                    // Set the package as well
                    entry.setParent(packageEntry);

                    // Fix the name
                    entry.setName(entry.getName().substring(entry.getName().lastIndexOf('$') + 1));

                    // And original name as well!
                    entry.setOriginalName(entry.getName().substring(entry.getName().lastIndexOf('$') + 1));

                    if(parentEntry != null)
                        parentEntry.getSubClasses().add(entry);

                    Logger.getInstance().log("Added %s as subclass to %s", entry.getName(), parentName);

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
                ClassEntry newEntry = new ClassEntry(entries[i]);
                newEntry.setParent(currentEntry);
                currentEntry.getClassEntries().add(newEntry);
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

    public String getGeneratedName(int idx) {
        if(idx > this.classNames.size() - 1)
        {
            Logger.getInstance().log("Idx: %d", idx);
            Logger.getInstance().handleException(new RuntimeException("Tried using more names than generated..."));
            return null;
        }

        return this.classNames.get(idx);
    }

    // Get the generated tree
    public PackageEntry getPackageTree() {
        return this.packageTree;
    }
}
