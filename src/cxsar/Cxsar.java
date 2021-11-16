package cxsar;

import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.utils.EntryWrapper;
import cxsar.utils.LogLevel;
import cxsar.utils.Logger;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.Buffer;
import java.util.*;
import java.util.zip.*;

public class Cxsar {

    // Target jar
    File targetFile;

    // Library files
    private ArrayList<File> libraries;

    // All parsed class files
    public HashMap<String, ClassNode> parsedClasses;

    // All other files, considered as 'resources'
    public HashMap<String, byte[]> parsedResources;

    // Entire classpath
    public HashMap<String, EntryWrapper> classPath;

    // All 'active' transformers
    public HashMap<String, ITransformer> transformerList;

    //  Initiate Cxsar with the target JAR file
    public Cxsar(File targetFile) {
        this.targetFile = targetFile;

        // Initiate all the fields we need
        parsedClasses = new HashMap<>();
        parsedResources = new HashMap<>();
        transformerList = new HashMap<>();
        classPath = new HashMap<>();
        libraries = new ArrayList<>();
    }

    // Obfuscate the target jar file
    public void obfuscateTarget() throws Exception {
        ZipFile inputStream = new ZipFile(targetFile);
        ZipOutputStream outputStream;

        // Set the output stream
        outputStream = new ZipOutputStream(new FileOutputStream("out.jar"));
        outputStream.setMethod(ZipOutputStream.DEFLATED);

        HashMap<String, byte[]> dataMap = new HashMap<>();

        Enumeration<? extends ZipEntry> entries = inputStream.entries();
        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if(entry == null)
                break;

            if(entry.isDirectory()) {
                outputStream.putNextEntry(entry);
                continue;
            }


            String entryName = entry.getName();

            byte[] bytes = new byte[(int) entry.getSize()];
            inputStream.getInputStream(entry).read(bytes);

            try {
                if (entryName.endsWith(".class") && !entryName.startsWith("META-INF")) {
                    Logger.getInstance().log("Loading entry: %s", entryName);

                    ClassReader classReader = new ClassReader(inputStream.getInputStream(entry));
                    ClassNode classNode = new ClassNode();


                    classReader.accept(classNode, 0);
                    parsedClasses.put(entryName, classNode);
                    dataMap.put(entryName, bytes);

                } else {
                    //TODO: MANIFEST PARSER
                    parsedResources.put(entryName, bytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Logger.getInstance().log("Successfully %d classes", parsedClasses.size());

        // Populate the ClassNodes
        parsedClasses.forEach((key, value) -> classPath.put(key.replace(".class", ""), new EntryWrapper(value)));

        // Load all transformers
        populateTransformerList();

        // Do name transformation first
        transformerList.forEach((name, transformer) -> transformer.preTransform(this, parsedClasses));

        List<Thread> threads = new ArrayList<>();
        final LinkedList<Map.Entry<String, ClassNode>> classQueue = new LinkedList<>(parsedClasses.entrySet());

        HashMap<String, byte[]> toWrite = new HashMap<>();

        for(int i = 0; i < 5; ++i) {
            threads.add(new Thread(() -> {
                while(true)
                {
                    Map.Entry<String, ClassNode> stringClassNodeEntry;

                    synchronized (classQueue) {
                        stringClassNodeEntry = classQueue.poll();
                    }

                    if (stringClassNodeEntry == null) break;
                    String entryName = stringClassNodeEntry.getKey();

                    byte[] entryData;
                    ClassNode cn = stringClassNodeEntry.getValue();

                    ClassWriter writer = new ClassWriter(0);
                    cn.accept(writer);

                    entryData = writer.toByteArray();

                    synchronized (toWrite)
                    {
                        toWrite.put(entryName, entryData);
                    }
                }
            }));
        }

        threads.forEach(Thread::start);

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        for (Map.Entry<String, byte[]> stringEntry : toWrite.entrySet()) {
            writeEntry(outputStream, stringEntry.getKey(), stringEntry.getValue(), false);
        }

        // TODO: Fix manifest automatically
        parsedResources.forEach((s, bytes) -> {
            try {
                writeEntry(outputStream, s, bytes, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        outputStream.close();
    }

    public void writeEntry(ZipOutputStream outJar, String name, byte[] value, boolean stored) throws IOException {
        ZipEntry newEntry = new ZipEntry(name);

        if (stored) {
            CRC32 crc = new CRC32();
            crc.update(value);

            newEntry.setSize(value.length);
            newEntry.setCrc(crc.getValue());
        }

        outJar.putNextEntry(newEntry);
        outJar.write(value);
    }

    // Wrapper for the obfuscateTarget function
    public void attemptObfuscation() {
        try {
            obfuscateTarget();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Find all transformers
    public void populateTransformerList() {
        String pkg = "cxsar.transformers";
        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .whitelistPackages(pkg)
                .scan()) {
            scanResult.getClassesWithAnnotation(RegisterTransformer.class.getName()).forEach(classInfo -> {
                AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(RegisterTransformer.class.getName());

                if (annotationInfo == null)
                    return;

                boolean transformerEnabled = (boolean) annotationInfo.getParameterValues().getValue("enabled");

                if (!transformerEnabled)
                    return;

                String transformerName = annotationInfo.getParameterValues().getValue("name").toString();
                Class<ITransformer> transformerClass = (Class<ITransformer>) classInfo.loadClass(false);

                try {
                    transformerList.put(transformerName, transformerClass.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Logger.getInstance().log("Succesfully parsed %d Transformers", transformerList.size());
    }
}
