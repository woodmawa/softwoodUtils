package scripts.modelling

import playpen.domains.customerDomain.Agreement
import playpen.domains.customerDomain.Customer
import playpen.domains.customerDomain.Party
import playpen.domains.customerDomain.EntityRef


Party p = new Party(name: 'NatWest')
println p

Agreement ag = new Agreement(name: 'my contract', signedDate: new Date())
EntityRef<Party> pr = new EntityRef(entity: Optional.of(p))

Customer c = new Customer(name: 'HSBC', party: pr, agreement: new EntityRef(entity: Optional.of(ag)))

println c
println c.getUrlName()

