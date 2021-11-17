package cxsar.transformers;

public enum TransformerPriority {
    FORCE, // force first position
    NORMAL, // regular
    LATE;// late


    public int getValue() {
        switch(this) {
            case NORMAL:
                return 1;
            case LATE:
                return 3;
            case FORCE:
                return 0;
        }
        return 0;
    }
}
