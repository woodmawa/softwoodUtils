package playpen.domains.customerDomain

import playpen.modelling.ModelBaseTrait
import playpen.modelling.ModelReferenceTrait

class EntityRef<ModelType> extends ModelReferenceTrait {
    String name

    Optional<ModelType> entity

    boolean isLoaded() {
        entity.isPresent()
    }

    def hydrate() {
        //lookup based on id
        ModelType ent //= ModelType.findById (id)
        entity = Optional.ofNullable(ent)
    }

    /**
     *
     * @param clos
     * @return
     */
    def with(Closure clos) {

        assert clos
        Closure cloned = clos.clone()
        cloned.resolveStrategy = Closure.DELEGATE_FIRST

        if (isLoaded()) {
            cloned.delegate = entity.get()
            cloned()
        } else {
            //do something
        }
    }

    String toString() {
        String refName
        refName = name ?: entity?.name + "(Ref)"
        "Reference: $refName, (entity:$entity)"
    }
}
