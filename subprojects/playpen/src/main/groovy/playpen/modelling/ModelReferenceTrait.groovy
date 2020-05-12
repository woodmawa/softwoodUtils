package playpen.modelling

class ModelReferenceTrait {
    String referredType
    String href
    String id
    String name
    String description

    String toString() {
        "name:$name, (id:$id}, referredType:@$referredType"
    }
}
