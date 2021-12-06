package cxsar.transformers.impl.name;

import cxsar.Cxsar;
import cxsar.transformers.impl.name.util.ClassEntry;
import cxsar.transformers.impl.name.util.PackageEntry;
import cxsar.utils.Logger;
import cxsar.utils.Timer;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


// Generate dictionary for the classpath
public class Dictionary {

    // Singleton stuff
    private static Dictionary instance = new Dictionary();

    // List of classNames to use
    private final List<String> classNames = new ArrayList<>();

    // List of completely random letters
    private final String[] completelyRandomNames = new String[1000];

    // HashMap of used mappings
    private HashMap<String, String> usedMappings = new HashMap<>();

    // Keep track of packages and their used names, since they can't be identical
    private PackageEntry packageTree;

    // Exclusions
    private ArrayList<PackageEntry> exclusionArrayList = new ArrayList<>();

    // Generated
    public boolean generated = false;

    public static Dictionary getInstance() {
        return instance;
    }

    // Alphabet string (lol!)
    public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    // Generate a dictionary for the classpath in context
    public void generateDictionary(Cxsar cxsar) {

        if(generated)
            return;

        // Add the base to the package tree
        packageTree = new PackageEntry("/");

        cxsar.classPath.forEach((path, classNode) -> {
            path = "/" + path; // "/" is the first package entry

            if(!isPackageTreePresent(path))
                createPath(path, classNode);

            getPackageTreeEntry(path);
        });

        // Fix innerClasses
        Timer timer = new Timer();

        // Fix the innter classes
        fixInnerClasses();

        Logger.getInstance().log("Fixed inner classes in %dms", timer.end());

        // Speaks for itself
        generateClassNameDictionary();

        // Generate dictionary
        generateAlphabetDictionary();

        generated = true;
    }

    // Generate dictionary
    public void generateClassNameDictionary() {
        // random
        Random random = new Random();
        // list of threads
        List<Thread> threadList = new ArrayList<>();
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

    // Just another dicitonary full of random strings
    public void generateAlphabetDictionary() {
        // list of threads
        List<Thread> threadList = new ArrayList<>();

        // count
        AtomicInteger nameCount = new AtomicInteger();

        for(int i = 0; i < 5; ++i)
            threadList.add(new Thread(() -> {
                while(true) {
                    synchronized (completelyRandomNames) {
                        // check if its filled
                        if(completelyRandomNames[completelyRandomNames.length - 1] != null)
                            break;

                        StringBuilder builder = new StringBuilder();
                        if(nameCount.get() < 26)
                            builder.append(ALPHABET.charAt(nameCount.get()));
                        else
                            for(int j = nameCount.get(); j > 0; j -= 25)
                                builder.append(ALPHABET.charAt(j % 26));

                            completelyRandomNames[nameCount.get()] = builder.toString();
                            nameCount.getAndIncrement();

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

    static int count = 0;
    public String getNextAlphabetDictionaryEntry() {
        if(count > completelyRandomNames.length - 1)
            count = 0;

        String res = completelyRandomNames[count];
        count++;
        return res;
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

                    // Remove original sub class from the list
                    classEntryIterator.remove();
                }
            }

            return false;
        });
    }

    // Create a path
    public void createPath(String path, ClassNode node) {
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
                newEntry.setNode(node);
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

    public void excludePackageEntryAndSubsequentSubEntries(PackageEntry entry)
    {
        entry.visit(entry1 -> {
            this.exclusionArrayList.add(entry1);
            Logger.getInstance().log("Excluded %s", entry1.getFullPath());
            return false;
        });
    }

    public boolean isExcluded(PackageEntry entry)
    {
        return this.exclusionArrayList.contains(entry);
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

    public HashMap<String, String> getUsedMappings() {
        return usedMappings;
    }

    public void setUsedMappings(HashMap<String, String> usedMappings) {
        this.usedMappings = usedMappings;
    }

    // Get the generated tree
    public PackageEntry getPackageTree() {
        return this.packageTree;
    }
}
