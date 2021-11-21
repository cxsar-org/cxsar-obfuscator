package cxsar;

import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.TransformerPriority;
import cxsar.transformers.impl.name.Dictionary;
import cxsar.utils.Logger;
import cxsar.utils.Timer;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Cxsar {

    // Target jar
    File targetFile;

    // All 'active' transformers
    public LinkedList<ITransformer> transformerList;

    // The entire loaded classpath
    public HashMap<String, ClassNode> classPath;

    // All resources
    public HashMap<String, byte[]> resources;

    //  Initiate Cxsar with the target JAR file
    public Cxsar(File targetFile) {
        this.targetFile = targetFile;

        // Initiate all the fields we need
        transformerList = new LinkedList<>();
        classPath = new HashMap<>();
        resources = new HashMap<>();
    }

    // Obfuscate the target jar file
    public void obfuscateTarget() throws Exception {
        // Add transformers
        this.populateTransformerList();

        // Create performance timer
        Timer timer = new Timer();

        Logger.getInstance().log("Loading JAR file...");

        // Open the zipfile
        ZipFile zipFile = new ZipFile(this.targetFile);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
            ZipEntry zipEntry = entries.nextElement();

            try {
                if (zipEntry.getName().endsWith(".class")) {
                    // Create classreader and a new node
                    ClassReader reader = new ClassReader(zipFile.getInputStream(zipEntry));
                    ClassNode node = new ClassNode();

                    // Visit the source file
                    reader.accept(node, 0);

                    // Log
                    Logger.getInstance().log("Added entry: %s", zipEntry.getName());

                    // Add it
                    classPath.put(zipEntry.getName(), node);
                } else {
                    // Create a buffer
                    byte[] fileBuffer = new byte[(int) zipEntry.getSize()];

                    // Read to the buffer
                    zipFile.getInputStream(zipEntry).read(fileBuffer, 0, fileBuffer.length);

                    resources.put(zipEntry.getName(), fileBuffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Debug info
        Logger.getInstance().log("Loaded %d classes", classPath.size());

        // Done
        Logger.getInstance().log("Successfully loaded zip file in %dms", timer.end());

        timer.begin();
        Logger.getInstance().log("Doing pre-transformation");

        // Classpath is now loaded properly, do preTransforms
        this.transformerList.forEach(iTransformer -> iTransformer.preTransform(this));

        Logger.getInstance().log("Succesfully did pre-transformation in %dms", timer.end());

        // Write to new file
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("out.jar"));

        // Lists...
        List<Thread> threads = new ArrayList<>();
        final LinkedList<Map.Entry<String, ClassNode>> classQueue = new LinkedList<>(classPath.entrySet());

        HashMap<String, byte[]> toWrite = new HashMap<>();

        for(int i = 0; i < 5; ++i)
        {
            threads.add(new Thread(() -> {
                while(true)
                {
                    Map.Entry<String, ClassNode> stringClassNodeEntry;

                    synchronized (classQueue) {
                        stringClassNodeEntry = classQueue.poll();
                    }

                    // Check if its null
                    if(stringClassNodeEntry == null)
                        break;

                    ClassNode classNode = stringClassNodeEntry.getValue();

                    // Create the writer
                    ClassWriter writer = new ClassWriter(0);
                    classNode.accept(writer);

                    // Convert to the data
                    byte[] newEntryData;
                    newEntryData = writer.toByteArray();

                    synchronized (toWrite)
                    {
                        toWrite.put(stringClassNodeEntry.getKey(), newEntryData);
                    }
                }
            }));
        }

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }

        // Write back the classes
        toWrite.entrySet().forEach(stringEntry -> {
            try {
                writeEntry(zipOutputStream, stringEntry.getKey(), stringEntry.getValue(), false);
            } catch (IOException e) {
                Logger.getInstance().handleException(e);
            }
        });

        // Write back all old resources
        resources.forEach((s, bytes) -> {
            try {
                writeEntry(zipOutputStream, s, bytes, false);
            } catch (IOException e) {
                Logger.getInstance().handleException(e);
            }
        });

        zipOutputStream.close();
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

        // Amount forced to the front
        final int[] forceCount = {0};

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

                Class<ITransformer> transformerClass = (Class<ITransformer>) classInfo.loadClass(false);

                try {
                    transformerList.add(forceCount[0], transformerClass.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Logger.getInstance().log("Succesfully parsed %d Transformers", transformerList.size());
    }

    public Map.Entry<String, byte[]> findManifestEntry() {
        AtomicReference<Map.Entry<String, byte[]>> res = null;

        this.resources.entrySet().forEach(stringEntry -> {
            if(stringEntry.getKey().endsWith("MANIFEST.MF"))
                res.set(stringEntry);
        });

        return res.get();
    }
}
