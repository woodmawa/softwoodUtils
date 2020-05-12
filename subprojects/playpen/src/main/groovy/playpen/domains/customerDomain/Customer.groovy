package playpen.domains.customerDomain

import playpen.modelling.ModelBaseTrait

class Customer implements ModelBaseTrait{
    String name

    EntityRef<Party> party
    EntityRef<Agreement> agreement
}
