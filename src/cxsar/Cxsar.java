package cxsar;

import cxsar.transformers.ITransformer;
import cxsar.transformers.RegisterTransformer;
import cxsar.transformers.utils.EntryWrapper;
import cxsar.utils.Logger;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        ZipInputStream inputStream;
        ZipOutputStream outputStream;

        // Set the input stream
        inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(this.targetFile)));

        // Set the output stream
        outputStream = new ZipOutputStream(new ByteArrayOutputStream());
        outputStream.setMethod(ZipOutputStream.DEFLATED);

        HashMap<String, byte[]> dataMap = new HashMap<>();

        while(true) {
            ZipEntry entry = inputStream.getNextEntry();

            if(entry == null)
                break;

            if(entry.isDirectory()) {
                outputStream.putNextEntry(entry);
                continue;
            }

            byte[] buffer = new byte[1];
            ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();

            int length = inputStream.read(buffer);

            if(length > 0)
                entryBuffer.write(buffer, 0, length);

            byte[] entryData = entryBuffer.toByteArray();

            String entryName = entry.getName();

            if(entryName.endsWith(".class")) {
                ClassReader classReader = new ClassReader(entryData);
                ClassNode classNode = new ClassNode();

                classReader.accept(classNode, 0);
                parsedClasses.put(entryName, classNode);
                dataMap.put(entryName, entryData);
            } else {
                //TODO: MANIFEST PARSER
                parsedResources.put(entryName, entryData);
            }

            // Populate the ClassNodes
            parsedClasses.forEach((key, value) -> classPath.put(key.replace(".class", ""), new EntryWrapper(value)));
        }

        // Load all transformers
        populateTransformerList();
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

        Logger.getInstance().log(String.format("Succesfully parsed %d Transformers", transformerList.size()));
    }
}
