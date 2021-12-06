package cxsar.main;

import cxsar.Cxsar;

import java.io.File;

public class Main {

    private static String[] launchArgs;

    private static String getOutPath() {
        String res = null;
        for(int i = 0; i < launchArgs.length; ++i)
            if(launchArgs[i].equals("-out")) {
                res = launchArgs[i + 1];
                break;
            }

        return (res == null) ? "out.jar" : res;
    }

    private static String getTargetPath() {
        String res = null;
        for(int i = 0; i < launchArgs.length; ++i)
            if(launchArgs[i].equals("-in")) {
                res = launchArgs[i + 1];
                System.out.println(res);
                break;
            }

        return (res == null) ? "test.jar" : res;
    }

    public static void main(String[] args) {
        launchArgs = args;

        Cxsar cxsar = new Cxsar(new File(getTargetPath()), getOutPath());

        cxsar.attemptObfuscation();
    }
}
