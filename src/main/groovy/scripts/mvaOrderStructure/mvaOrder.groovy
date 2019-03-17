package scripts.mvaOrderStructure

import com.softwood.utils.JsonUtils

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(4)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()


/**
 * class types that used to build order
 * probably need a builder at some point to make this easy
 *
 */


enum BssOrderType {
    NewProvide,
    ReProvide,
    Amend,
    cease,
    InternalShift,
    ExternalShift
}

//basic site for order structure
class Site {
    UUID siteId = UUID.randomUUID()
    String siteName
    String addressLine1
    String addressLine2
    String city
    String country
    String postalCode
    String geoLocationCode
}

class CustomerFacingService {
    UUID cfsId = UUID.randomUUID()
    String cfsName
    String cfsStatus //enum later when sorted state model
    UUID productId
    String productServiceName
    LocalDate requireByDateTime = LocalDate.now()
    Site owningSite
    Site remoteSite
}

class RfcRequestGroup {
    String name
    LocalDateTime createdDateTime = LocalDateTime.now()
    Collection rfcWorkOrders = []
}

class RfcWorkOrder {
    String organisationName
    String serviceName //optional
    BssOrderType bssOrderType = BssOrderType.NewProvide
    CustomerFacingService owningCfs
    UUID RfcWorkOrderId = UUID.randomUUID()
    String workOrderName
    LocalDateTime createdDateTime = LocalDateTime.now()
    LocalDateTime issuedDateTime = LocalDateTime.now()
    LocalDateTime rfcApprovedDateTime
    LocalDateTime requiredByDateTime = LocalDateTime.now() + 30
    LocalDateTime rfcStartedDateTime
    LocalDateTime rfcCompletedDateTime
    String rfcOrderStatus  //enum later
    //has array of order lines
    Collection<OrderLine> orderLines = new ConcurrentLinkedQueue<OrderLine>()

}

class OrderLine {
    Long orderLineNumber  //implicit as its in a queue?
    String orderLineStatus
    ResourceFacingService woRfs
}

enum ResourceFacingServiceType {
    PseudoWire,
    Vlan,
    Vpn,
    VirtualConnection
}

class ResourceFacingService {
    UUID rfsId = UUID.randomUUID()
    ResourceFacingServiceType type
    String rfsName
    String rfsOpStatus
    String rfsAdminStatus
    //optional list of related CFS that rely on this rfs
    Collection<CustomerFacingService> relatedCfs = new ConcurrentLinkedQueue<>()
    //related rfs for this rfs
    Collection<ResourceFacingService> relatedRfs = new ConcurrentLinkedQueue<>()
    Collection<Resource> resources =  new ConcurrentLinkedQueue<>()
    Collection<Property> rfsProperties = new ConcurrentLinkedQueue<>()
}

enum AdminstrativeStateType {
    Locked,
    Unlocked
}
enum OperationalStateType {
    Up,
    Down,
    Suspended,
    Active,
    Open,
    Closed
}

enum ResourceType {
    // logical types
    LogicalPort,
    LogicalInterface,
    LogicalSubInterface,
    LogicalCrossConnect,
    RouteTarget,
    RouteDescriptor,
    BridgeDomain,
    LinkAggregationGroup,
    ServiceDistributionPoint,
    QoS,

    //physical types
    EquipmentHolder,
    Node,
    Card,
    Module,
    PhysicalPort
}

class Resource {
    UUID resourceId = UUID.randomUUID()
    ResourceType type
    String resourceName
    String resourceOpStatus = OperationalStateType.Up
    String resourceAdminStatus = AdminstrativeStateType.Unlocked
    Collection<Property> resourceProperties = new ConcurrentLinkedQueue<>()

}

//generic property type
class Property {
    String groupName = "<default>"
    String name
    def value
    Collection<Object> valueList = new ConcurrentLinkedQueue<>()
    Class valueClassType
    //set to true if must be there in originating request
    Boolean required
}

/**
 * build an order
 */

RfcRequestGroup ordGroup = new RfcRequestGroup(name:"group #1")

RfcWorkOrder wo1 = new RfcWorkOrder(
        organisationName:  "ACME",
        serviceName: "myFirstEthernet",
        bssOrderType: BssOrderType.NewProvide,
        rfcOrderStatus: "Issued"    //sent from cramer
)

RfcWorkOrder wo2 = new RfcWorkOrder(
        organisationName:  "ACME",
        serviceName: "mySecondEthernet",
        bssOrderType: BssOrderType.NewProvide,
        rfcOrderStatus: "Issued"     //sent from cramer
)

OrderLine oline = new OrderLine(orderLineNumber: 1, orderLineStatus: "initial")
oline.woRfs = new ResourceFacingService(type: ResourceFacingServiceType.PseudoWire,
        rfsName: "pw#1",
        rfsOpStatus: OperationalStateType.Up,
        rfsAdminStatus: AdminstrativeStateType.Unlocked)
oline.woRfs.rfsProperties << new Property(name:"bandwidth", value:"10GB", valueClassType: String)
wo1.orderLines << oline

res  = jsonGenerator.toTmfJson([wo1, wo2]).encodePrettily()
println "tmfjson work order group  : $res"


res2  = jsonGenerator.toJsonApi([wo1, wo2]).encodePrettily()
println "jsonApi work order group  : $res2"