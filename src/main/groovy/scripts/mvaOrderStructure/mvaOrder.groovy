package scripts.mvaOrderStructure

import com.softwood.utils.JsonUtils

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(10)
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
    ExternalShift,
    Create
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
    UUID productId = UUID.randomUUID()
    String productServiceName
    LocalDate requireByDate = LocalDate.now()
    LocalDate contractedDeliveryDate = LocalDate.now()
    Site owningSite
    Site remoteSite
}

class OrderGroup {
    String name
    String projectName
    LocalDateTime createdDateTime = LocalDateTime.now()
    Collection configurationOrders = []
}

class ConfigurationOrder {
    String organisationName
    String serviceName //optional
    BssOrderType bssOrderType = BssOrderType.Create
    CustomerFacingService owningCfs
    UUID configurationOrderId = UUID.randomUUID()
    String orderName
    LocalDateTime createdDateTime = LocalDateTime.now()
    LocalDateTime issuedDateTime = LocalDateTime.now()
    LocalDateTime rfcApprovedDateTime
    LocalDateTime requiredByDateTime = LocalDateTime.now() + 30
    LocalDateTime rfcStartedDateTime
    LocalDateTime rfcCompletedDateTime
    String orderStatus  //enum later
    //has array of order lines
    Collection<OrderLine> orderLines = new ConcurrentLinkedQueue<OrderLine>()

}

//need to figure out the action types
enum OrderLineActionType {
    Provide,
    Add,
    Delete,
    Amend,
    Create
}

class OrderLine {
    String jobRef
    Long orderLineNumber  //implicit as its in a queue?
    String orderLineStatus
    List<Long> dependsOnOrderLines //optional - if a sequence dependency for task - put list there
    OrderLineActionType orderLineAction
    ResourceFacingService orderLineRfs
}

enum ResourceFacingServiceType {
    PseudoWire,
    Vlan,
    Vpn,
    VirtualConnection,
    ETHERNET_CROSS_CONNECT,
    ETHERNET_PSEUDOWIRE_ENDPOINT,
    PSEUDOWIRE_ENDPOINT

}

class ResourceFacingService {
    UUID rfsId = UUID.randomUUID()
    ResourceFacingServiceType type
    String rfsName
    String rfsDescription
    String rfsOpStatus
    String rfsAdminStatus
    //optional list of related CFS that rely on this rfs
    Collection<CustomerFacingService> relatedCfs = new ConcurrentLinkedQueue<>()
    //related rfs for this rfs
    Collection<ResourceFacingService> relatedRfs = new ConcurrentLinkedQueue<>()
    Collection<Resource> resources =  new ConcurrentLinkedQueue<>()
    Collection<Property> rfsProperties = new ConcurrentLinkedQueue<>()
    Device configuredDevice
    Device remoteDevice
}

enum AdminstrativeStateType {
    Locked,
    LockedEnabled,
    LockedDiasabled,
    LockedMaintenance,
    Unlocked,
    UnlockedEnabled,
    UnlockedDisabled,
    UnlockedAutomaticInService

}
enum OperationalStateType {
    Up,
    Down,
    AdministrativelyDown,
    Suspended,
    Active,
    Open,
    Closed,
    Disabled
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

enum ResourceRoleType {
    Router,
    Switch,
    Firewall,
    Other
}

class Device {
    UUID deviceId = UUID.randomUUID()
    ResourceRoleType roleType  //should really be list but we are using one of roles for order
    String deviceName
    String managedHostname
    String managementIpAddress
    String deviceOpStatus = OperationalStateType.Up
    String deviceAdminStatus = AdminstrativeStateType.Unlocked
    Collection<Property> properties = new ConcurrentLinkedQueue<>()

}

//generic property type
class Property {
    String groupName = "<default>"
    String name
    def value
    Collection<Object> valueList //= new ConcurrentLinkedQueue<>()
    String valueClassType
    //set to true if must be there in originating request
    Boolean required
}

/**
 * build an order
 */

//associate the RFS to configure for the order line with the managed configuredDevice
//query can same RFS span multiple devices on same order line?  - e.g for a cross connect?
//actual xconnect details must be matched with similar to remote end however
//so suspecting xconnect services requires two matched config on dev A and Dev B - hence two orderlines
Device cisco903a = new Device (deviceName: 'fred',  managedHostname: 'wesbn1-agn-a1', managementIpAddress : '194.159.100.86')

Device cisco903z = new Device (deviceName: 'fred',  managedHostname: 'wesbn1-agn-a1', managementIpAddress : '194.159.100.86')

Site custSite = new Site (siteName: "LSE campus (Janet)  ", city:"london", country:"UK", postalCode: "WC2A 2AE")
Site remoteSite = new Site (siteName: "Imperial campus (Janet) ", city:"london", country:"UK", postalCode: "SW7 2BX")

CustomerFacingService ethcfs = new CustomerFacingService(cfsName:"2C03636667", productServiceName: "Ethernet Wire-Line", cfsStatus: "new provide",
owningSite: custSite, remoteSite: remoteSite)

CustomerFacingService cfs2 = new CustomerFacingService(cfsName:"XXX", productServiceName: "ip vpn", cfsStatus: "new provide",
        owningSite: custSite)

OrderGroup ordGroup = new OrderGroup(name:"group #1", projectName: "JANET project" )

ConfigurationOrder wo1 = new ConfigurationOrder(
        organisationName:  "JANET UK",
        owningCfs: ethcfs,
        serviceName: "2C03636667",
        bssOrderType: BssOrderType.Create,
        orderStatus: "Issued"    //sent from cramer
)

ConfigurationOrder wo2 = new ConfigurationOrder(
        organisationName:  "JANET UK",
        owningCfs: cfs2,
        serviceName: "mySecondEthernet",
        bssOrderType: BssOrderType.Create,
        orderStatus: "Issued"     //sent from cramer
)

OrderLine oline1 = new OrderLine(jobRef: 704851143, orderLineNumber: 1, orderLineStatus: "initial", orderLineAction: OrderLineActionType.Create)
oline1.orderLineRfs = new ResourceFacingService(type: ResourceFacingServiceType.ETHERNET_CROSS_CONNECT,
        rfsName: "eth-xconnect-#1",
        rfsDescription: "vf=EWL:cn=JANET:tl=2C03636667",
        rfsOpStatus: OperationalStateType.Down,
        rfsAdminStatus: AdminstrativeStateType.Unlocked,
        relatedCfs: [ethcfs],
        configuredDevice:cisco903a)
oline1.orderLineRfs.rfsProperties << new Property(name:"interface", value:"GigabitEthernet0/2/0", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"description", value:"vf=EWL:cn=JANET:tl=2C03636667", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"owning_device", value:"194.159.100.86", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"matched_vlans", value:12, valueClassType: Integer.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"matched_type", value:"dot1q", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"operational_state", value:"down", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"rewrite", value:"pop 1", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"iosxe_efp_type", value:"serviceinstance", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"iosxe_efp_instance_id", value:1213, valueClassType: Integer.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"l2cp_params", value:"forward", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"ingress_qos", value:"Ethernet-IngressQoS-Template1-Standard", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(name:"egress_qos", value:"Ethernet-Parent-EgressQoS-Template2", valueClassType: String.typeName)

oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"direction", value:"egress", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"cir", value:"1000000000", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"cbs", value:"", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"eir", value:"", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"ebs", value:"", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"colour_mode", value:"colour-blind", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:class-default", name:"coupling_flag", value:false, valueClassType: Boolean.typeName)

oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"direction", value:"egress", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"cir", value:"1000000000", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"cbs", value:"", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"eir", value:"", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egressQoS class:STANDARD-QG", name:"ebs", value:"", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"colour_mode", value:"colour-blind", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"coupling_flag", value:false, valueClassType: Boolean.typeName)

oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"direction", value:"ingress", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"cir", value:"1000000000", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"cbs", value:"12500000", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"eir", value:"1000000000", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"ebs", value:"12500000", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"colour_mode", value:"colour-blind", valueClassType: String.typeName)
oline1.orderLineRfs.rfsProperties << new Property(groupName: "ingress QoS class:class-default", name:"coupling_flag", value:false, valueClassType: Boolean.typeName)

wo1.orderLines << oline1

OrderLine oline2 = new OrderLine(jobRef: 704851143, orderLineNumber: 2, orderLineStatus: "initial", orderLineAction: OrderLineActionType.Create)
oline2.orderLineRfs = new ResourceFacingService(type: ResourceFacingServiceType.ETHERNET_PSEUDOWIRE_ENDPOINT,
        rfsName: "eth-pw-ep#1",
        rfsDescription: "vf=EWL:cn=JANET:tl=2C03636667",
        rfsOpStatus: OperationalStateType.Down,
        rfsAdminStatus: AdminstrativeStateType.Unlocked,
        configuredDevice:cisco903a)

oline2.orderLineRfs.rfsProperties << new Property(name:"owning_device", value:"194.159.100.86", valueClassType: String)

OrderLine oline3 = new OrderLine(jobRef: 704851143, orderLineNumber: 2, orderLineStatus: "initial", orderLineAction: OrderLineActionType.Create)
oline3.orderLineRfs = new ResourceFacingService(type: ResourceFacingServiceType.PSEUDOWIRE_ENDPOINT,
        rfsName: "pw-ep#2",
        rfsDescription: "--- missing in example---",
        rfsOpStatus: OperationalStateType.Down,
        rfsAdminStatus: AdminstrativeStateType.Unlocked,
        configuredDevice:cisco903a)

oline3.orderLineRfs.rfsProperties << new Property(name:"owning_device", value:"194.159.100.86", valueClassType: String)
oline3.orderLineRfs.rfsProperties << new Property(name:"pw_id", value:"3007297536", valueClassType: String)
oline3.orderLineRfs.rfsProperties << new Property(name:"priority", value:0, valueClassType: Integer)
oline3.orderLineRfs.rfsProperties << new Property(name:"remote_peer", value:"194.159.102.88", valueClassType: String)
oline3.orderLineRfs.rfsProperties << new Property(name:"pw_mtu", value:"2000", valueClassType: String)

//link asr 903 configuredDevice to RFS on the order line
oline1.orderLineRfs.configuredDevice = cisco903a
oline2.orderLineRfs.configuredDevice = cisco903a
oline3.orderLineRfs.configuredDevice = cisco903a

ordGroup.configurationOrders = [wo1, wo2]


//res  = jsonGenerator.toTmfJson([wo1, wo2]).encodePrettily()
res  = jsonGenerator.toTmfJson(ordGroup).encodePrettily()

println "tmfjson work order group  : $res"


//res2  = jsonGenerator.toJsonApi([wo1, wo2]).encodePrettily()  - breaks with an array
res2  = jsonGenerator.toJsonApi(ordGroup).encodePrettily()
println "\n\njsonApi work order group  : $res2"

