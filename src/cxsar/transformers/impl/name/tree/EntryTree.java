package cxsar.transformers.impl.name.tree;

import cxsar.transformers.utils.EntryWrapper;

import java.util.ArrayList;
import java.util.HashSet;

public class EntryTree {

    // Associated wrapper
    public EntryWrapper entryWrapper;

    // Names of the entries that this entry inherits from
    public HashSet<String> parentEntries = new HashSet<>();

    // Names of the entries that this entry is used by
    public HashSet<String> subEntries = new HashSet<>();

    // Constructor
    public EntryTree(EntryWrapper wrapper) {
        this.entryWrapper = wrapper;
    }

}
