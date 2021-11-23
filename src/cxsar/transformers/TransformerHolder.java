package cxsar.transformers;

public class TransformerHolder {
    TransformerPriority transformerPriority;
    ITransformer transformer;
    String name;

    public TransformerHolder(ITransformer transformer, TransformerPriority priority, String name)
    {
        this.transformer = transformer;
        this.transformerPriority = priority;
        this.name = name;
    }

    public TransformerPriority getTransformerPriority() {
        return transformerPriority;
    }

    public ITransformer getTransformer() {
        return transformer;
    }

    public void setTransformerPriority(TransformerPriority transformerPriority) {
        this.transformerPriority = transformerPriority;
    }

    public void setTransformer(ITransformer transformer) {
        this.transformer = transformer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
