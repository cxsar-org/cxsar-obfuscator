package cxsar.main;

import cxsar.Cxsar;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        Cxsar cxsar = new Cxsar(new File("test.jar"));

        cxsar.attemptObfuscation();
    }
}
