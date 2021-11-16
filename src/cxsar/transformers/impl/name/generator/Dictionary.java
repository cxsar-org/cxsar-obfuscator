package cxsar.transformers.impl.name.generator;

import cxsar.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Dictionary {

    // A list of generated method names
    private List<String> generatedNames = new ArrayList<>();

    // A size of names to generate during the first iteration
    private static final int FIRST_ITERATION_SIZE = 999;

    // The amount of threads to generate random names
    private static final int FIRST_ITERATION_THREAD_COUNT = 5;

    // The length of generated names
    private static final int FIRST_ITERATION_NAME_LENGTH = 8;

    // random shit
    private Random random = new Random();

    // Keep a list of used names
    ArrayList<String> usedNamed = new ArrayList<>();

    // Keep count of number of fields and methods
    private int renamedMethods = 0;

    // Keep count of number of fields
    private int renamedFields = 0;

    // Keep count of number of classes
    private int renamedClasses = 0;

    // Generate a dictionary here
    public Dictionary()
    {
        List<Thread> threadList = new ArrayList<>();

        // Generate names
        for(int _ = 0; _ < FIRST_ITERATION_THREAD_COUNT; ++_)
        {
            threadList.add(new Thread(() -> {
                    while (generatedNames.size() < FIRST_ITERATION_SIZE) {
                        StringBuilder nameBuilder = new StringBuilder();

                        for (int i = 0; i < FIRST_ITERATION_NAME_LENGTH; ++i)
                            nameBuilder.append(random.nextBoolean() ? 'I' : 'i');

                        generatedNames.add(nameBuilder.toString());
                    }
            }));
        }

        // Start every thread
        threadList.forEach(Thread::start);

        // Wait for the threads to finish
        threadList.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Remove potential duplicates
        generatedNames = generatedNames.stream()
                .distinct()
                .collect(Collectors.toList());

        Logger.getInstance().log("Succesfully generated %d names", generatedNames.size());
    }

    public String retrieveName(int idx) {
        return generatedNames.get(idx);
    }

    public String retrieveMethodName() {
        if(renamedMethods >= generatedNames.size())
            renamedMethods = 0;

        return retrieveName(renamedMethods++);
    }

    public String retrieveFieldName() {
        if(renamedMethods >= generatedNames.size())
            renamedMethods = 0;

        return retrieveName(renamedFields++);
    }

    String alphabet = "abcdefghijklmnopqrstuvwxyz";

    public String retrieveClassName(String fullPath) {
        return "";
    }
}
