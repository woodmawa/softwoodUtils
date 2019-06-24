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
class Site implements Cloneable {
    UUID siteId = UUID.randomUUID()
    String siteName
    String addressLine1
    String addressLine2
    String city
    String country
    String postalCode
    String geoLocationCode

    Site clone() {
        Site s = super.clone()
        s.siteId = siteId
        s
    }
}

class CustomerFacingService {
    UUID cfsId = UUID.randomUUID()
    String cfsName
    String cfsStatus //enum later when sorted state model
    UUID productId = UUID.randomUUID()
    String productType  // normally EWL, IPPVPN, DIA etc
    String productServiceName  //name of the service
    LocalDate requiredByDate = LocalDate.now() + 30
    LocalDate contractedDeliveryDate = LocalDate.now() + 28
    Site owningSite
    Site remoteSite
}

class OrderGroup {
    String name
    String projectName
    LocalDateTime createdDateTime = LocalDateTime.now()
    Collection orders = []
}

/* atomic unit of work  - all orderlines pass or all fail */
class Order {
    String organisationName
    String serviceName //optional
    BssOrderType bssOrderType = BssOrderType.Create
    CustomerFacingService owningCfs
    UUID orderId = UUID.randomUUID()
    String orderName
    LocalDateTime createdDateTime = LocalDateTime.now()
    LocalDateTime issuedDateTime = LocalDateTime.now()
    LocalDateTime approvedDateTime = LocalDateTime.now()
    LocalDateTime startedDateTime  //managed by cortex
    LocalDateTime completedDateTime //managed by cortex
    String orderStatus  //enum later
    //has array of order lines
    Collection<OrderLine> orderLines = new ConcurrentLinkedQueue<OrderLine>()
    List<Order> dependsOnOrders        //if there is an orders dependency express this here

}

//need to figure out the action types
enum OrderLineActionType {
    Provide,
    Add,
    Delete,
    Amend,
    Create
}

/* one order line per resourceFacingService configuration
   the RFS exists in cramer outside of any given job so the
   orderline is the sitruction to control the change for the
   rfs on this orderline.

   If this orderlines work is dependent on another orderline competing
   first this can be expressed using the dependsOnOrderLines array of orderderline.orderLineNumbers
 */
class OrderLine {
    String jobRef         // cramer ref to its internal work list
    Long orderLineNumber  //implicit as its in a queue?
    String orderLineStatus
    List<Long> dependsOnOrderLines //optional - if a sequence dependency for task - put list there
    OrderLineActionType orderLineAction
    ResourceFacingService rfs
    List<String> configSnippits = []        //can be passed back as result detail back to cramer
}

enum ServiceType {
    PseudoWire,
    Vlan,
    ManagementVlan,
    Vpn,
    VirtualConnection,
    ETHERNET_CROSS_CONNECT,
    ETHERNET_PSEUDOWIRE_ENDPOINT,
    PSEUDOWIRE_ENDPOINT

}

//base service
class Service {
    UUID sid //base type id leave unassigned if subclass defines an id
    ServiceType type
    String serviceName
    String serviceDescription
    Collection<Property> serviceProperties //optional

}

class ResourceFacingService extends Service {
    UUID rfsId = UUID.randomUUID()
    String rfsOpStatus
    String rfsAdminStatus
    //optional list of related CFS that rely on this rfs
    String relatedCfs //name of cfs this rfs is supporting
    //optional related/dependent on rfs for this rfs
    Collection<ResourceFacingService> relatedRfs = new ConcurrentLinkedQueue<>()
    //now under device : Collection<Resource> aEndResources =  new ConcurrentLinkedQueue<>()
    //Collection<Resource> zEndResources =  new ConcurrentLinkedQueue<>()
    Collection<Property> rfsProperties = new ConcurrentLinkedQueue<>()
    Device aEndDevice
    Device zEndDevice
}

enum AdminstrativeStateType {
    Up,
    Down,
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
    Policy,

    //physical types
    EquipmentHolder,
    Node,
    Card,
    Module,
    PhysicalPort,
    PhysicalInterface,

}

class Resource implements Cloneable {
    UUID resourceId = UUID.randomUUID()
    UUID deviceId
    UUID parentId
    String action
    ResourceType type
    String resourceName
    String resourceOpStatus
    String resourceAdminStatus
    String resourceDescription
    Collection<Property> referencedResources
    Collection<Property> resourceProperties = new ConcurrentLinkedQueue<>()

    Resource clone() {
        Resource r = super.clone()
        r.resourceId = resourceId
        r.resourceProperties = resourceProperties
        r.referencedResources = null
        r
    }
}

enum DeviceRoleType {
    Router,
    AggregationRouter,
    DistributionRouter,
    CustomerPremiseRouter,
    AccessEdgeRouter,
    Switch,
    Firewall,
    ProviderEdge,
    Other
}

/*
 this represents the router, switch etc nodes in Cramer.  These are
 managed entities with logical and physical resources
 Cramer is not expected to know the OperationalStatus, but cortex can pass
 this in the result if required
 */

class Device implements Cloneable {
    UUID deviceId = UUID.randomUUID()
    String deviceType  //should really be list but we are using one of roles for order
    DeviceRoleType roleType  //should really be list but we are using one of roles for order
    String deviceName
    String managedHostname
    String managementIpAddress
    String deviceOpStatus //= OperationalStateType.Up
    String deviceAdminStatus = AdminstrativeStateType.Unlocked
    Collection<Resource> impactedResources =  new ConcurrentLinkedQueue<>()
    Collection<Property> deviceProperties = new ConcurrentLinkedQueue<>()

    //used to avoid encoded once json encoding
    Device clone () {
        Device d = super.clone()
        d.deviceId = deviceId
        //start with new empty list
        d.impactedResources = new ConcurrentLinkedQueue<>()
        d.deviceProperties = deviceProperties
        return d
    }
}

//generic property type
//it is possible to use simple map values - but cant pass before /required nor type info if you do this
//this is the recommended construct as it allows type binding for code
//parsing the json payload
//attributes are assumed to be groups - if not specific <default> group is assumed
class Property {
    String groupName //= "<default>"
    String name
    def originalValue        //optional
    Collection<Object> originalValueList  //optional
    def value
    Collection<Object> valueList
    String valueClassType
    //set to true if must be there in originating request
    Boolean required = false
}

/**
 * build an order
 */

/**
 * associate the RFS to configure for the order line with the managed aEndDevice
 * query can same RFS span multiple devices on same order line?  - e.g for a cross connect?
 * actual xconnect details must be matched with similar to remote end however
 * so suspecting xconnect services requires two matched config on dev A and Dev B - hence two orderlines
*/

/**
 * build two distinct AGN cisco asr 903s in the ring
 * sample EWL will connect across the two agn using pseudowire rfs
 */
Device aend_nte = new Device (deviceName: 'nte1',  managedHostname: 'worthing1-nte-a1', managementIpAddress : '176.24.5.34', roleType: DeviceRoleType.AccessEdgeRouter, deviceType : "Nokia 7720")
Device aend_agn = new Device (deviceName: 'agn1',  managedHostname: 'wesbn1-agn-a1', managementIpAddress : '194.159.100.86', roleType: DeviceRoleType.AggregationRouter, deviceType : "Cisco ASR 903")


/*
 * for Zend access project just define the z end Agn as target for the pseudowire
 */
Device zend_agn = new Device (deviceName: 'agn2',  managedHostname: 'wesbn3-agn-b3', managementIpAddress : '194.159.100.88', roleType: DeviceRoleType.AggregationRouter, deviceType : "Cisco ASR 903")

//declare two customer sites - two campuses on Janet network
Site custSite = new Site (siteName: "LSE campus (Janet)  ", city:"london", country:"UK", postalCode: "WC2A 2AE")
Site remoteSite = new Site (siteName: "Imperial campus (Janet) ", city:"london", country:"UK", postalCode: "SW7 2BX")

//declare the pre - existing VPN service
Service vfNteMgtVpn = new Service(serviceName:"NTE-MGT", type: ServiceType.Vpn)


/*create the customer service EWL between the two sites
 passing the cfs entity via the json is optional - but the
 passing the cfs name as a string attribute on the order mandatory
*/
CustomerFacingService ethcfs = new CustomerFacingService(cfsName:"2C03636667", productServiceName: "Ethernet Wire-Line", cfsStatus: "new provide",
owningSite: custSite, remoteSite: remoteSite)

CustomerFacingService ipvpncfs = new CustomerFacingService(cfsName:"3CXXX", productServiceName: "IPVPN", cfsStatus: "new provide",
        owningSite: custSite.clone())

/**
 * order group feature is optional.  You can send a single atomic order to cortex
 * or if necessary a 'group' of orders, typically assumed to part of project
 */
OrderGroup ordGroup = new OrderGroup(name:"sample group #1", projectName: "JANET project" )

Order wo1 = new Order(
        organisationName:  "JANET UK",
        owningCfs: ethcfs,          //optional : the ewl to be built
        serviceName: "2C03636667",  //mandatory minimum required - serviceName as string attribute
        bssOrderType: BssOrderType.NewProvide,
        orderStatus: "Issued"    //sent from cramer - need to agree cramer state model for an order
)

Order wo2 = new Order(
        organisationName:  "JANET UK",
        owningCfs: ipvpncfs,
        serviceName: "myipvpn",
        bssOrderType: BssOrderType.NewProvide,
        orderStatus: "Issued"     //sent from cramer
)

OrderLine oline1 = new OrderLine(jobRef: 704851143, orderLineNumber: 1, orderLineStatus: "initial", orderLineAction: OrderLineActionType.Create)
oline1.rfs = new ResourceFacingService(type: ServiceType.Vlan,
        serviceName: "ethernet-vlan-#1",
        serviceDescription: "vf=EWL:cn=JANET:tl=2C03636667",
        rfsAdminStatus: AdminstrativeStateType.Down,
        relatedCfs: ethcfs.cfsName,
        aEndDevice: aend_nte,
        zEndDevice: aend_agn)
oline1.rfs.rfsProperties << new Property(name:"owning_device", value:"194.159.100.86", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(name:"matched_vlans", value:12, valueClassType: Integer.typeName)
oline1.rfs.rfsProperties << new Property(name:"matched_type", value:"dot1q", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(name:"operational_state", value:"down", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(name:"iosxe_efp_type", value:"serviceinstance", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(name:"iosxe_efp_instance_id", value:1213, valueClassType: Integer.typeName)
oline1.rfs.rfsProperties << new Property(name:"l2cp_params", value:"forward", valueClassType: String.typeName)


/* this one is specified as an attribute group
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"direction", value:"egress", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"cir", value:"1000000000", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"cbs", value:"", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"eir", value:"", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egressQoS class:STANDARD-QG", name:"ebs", value:"", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"colour_mode", value:"colour-blind", valueClassType: String.typeName)
oline1.rfs.rfsProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"coupling_flag", value:false, valueClassType: Boolean.typeName)
*/

//in phase 1 cortex wont be configuring the other end of an RFS (typcially a nokia NE or a CPE) - so thi can be treated
//as optional for now
Resource aNteEndResource = new Resource(resourceName: "GigabitEthernet0/3/1", type:ResourceType.PhysicalInterface)
aNteEndResource.deviceId = aend_nte.deviceId

//define general  interface policy map for interface
Resource zEndAgnIfQoSPolicy = new Resource( action:"referenced", resourceName: "G0/2/0-InterfaceQosSPolicy-Template", type:ResourceType.Policy )
zEndAgnIfQoSPolicy.deviceId = aend_agn.deviceId
zEndAgnIfQoSPolicy.resourceProperties << new Property (groupName: "QoS class:class-default", name:"class", value:"class-default", valueClassType: String.typeName)
zEndAgnIfQoSPolicy.resourceProperties << new Property (groupName: "QoS class:class-default", name:"shape", valueList: ["average", "1000000000"], valueClassType: String.typeName)


//for a 903 order the zEnd of an RFS will typically refer to the AGN router - as we are most building services
//the key resource here is the physical ingress interface on the AGN that need to be configured
//pre-existing resources
Resource zEndPhysIf = new Resource(action: "update", resourceName: "GigabitEthernet0/2/0", type:ResourceType.PhysicalInterface, resourceAdminStatus: AdminstrativeStateType.Unlocked )
zEndPhysIf.deviceId = aend_agn.deviceId
zEndPhysIf.referencedResources = new ConcurrentLinkedQueue<>()
zEndPhysIf.referencedResources << zEndAgnIfQoSPolicy.resourceId


Resource customerVlanServiceInstance = new Resource(resourceName: "1213 ethernet", type:ResourceType.LogicalSubInterface, resourceAdminStatus: AdminstrativeStateType.Down, action: "create")
customerVlanServiceInstance.resourceDescription = "vf:EWL;cn:JANET:t1=2C03636667"
customerVlanServiceInstance.parentId =  zEndPhysIf.resourceId
customerVlanServiceInstance.deviceId=  aend_agn.deviceId
customerVlanServiceInstance.referencedResources = new ConcurrentLinkedQueue<>()

customerVlanServiceInstance.resourceProperties << new Property (name:"class", value:"class-default", valueClassType: String.typeName)
customerVlanServiceInstance.resourceProperties << new Property (name:"rewrite", valueList: ["ingress", "tag", "pop 1", "symmetric" ], valueClassType: String.typeName)
customerVlanServiceInstance.resourceProperties << new Property (name:"l2protocol", value: "forward", valueClassType: String.typeName)
customerVlanServiceInstance.resourceProperties << new Property (name:"service-policy", valueList: ["input", "JANET427902-G0/2/0:12-Ethernet-IngressQoS-Template1-Standard" ], valueClassType: String.typeName)
customerVlanServiceInstance.resourceProperties << new Property (name:"service-policy", valueList: ["output", "JANET427902-G0/2/0:12-Ethernet-EgressQoS-Template2" ], valueClassType: String.typeName)


//new resources created on order janet ethernet  ingress policy
Resource custZEndPolicy1 = new Resource(action: "create", resourceName: "JANETUK429894-G0/2/0:12-Ethernet-IngressQoS-Template1-Standard", type:ResourceType.Policy )
custZEndPolicy1.deviceId = aend_agn.deviceId
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"template", value:"Ethernet-IngressQoS-Template1-Standard", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"direction", value:"ingress", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"cir", value:"1000000000", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"bc", value:"12500000", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"pir", value:"1000000000", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"be", value:"12500000", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"colour_mode", value:"colour-blind", valueClassType: String.typeName)
custZEndPolicy1.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"coupling_flag", value:false, valueClassType: Boolean.typeName)

//janet ethernet egress service policy
Resource custZEndPolicy2 = new Resource(action: "create", resourceName: "JANETUK429894-G0/2/0:12-Ethernet-EgressQoS-Template2", type:ResourceType.Policy )
custZEndPolicy2.deviceId = aend_agn.deviceId
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"template", value:"Ethernet-EgressQoS-Template2", valueClassType: String.typeName)
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"direction", value:"egress", valueClassType: String.typeName)
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"bandwidth", value:"1000000", valueClassType: String.typeName)
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:STANDARD-QG", name:"queue-limit", value:"2000000", valueClassType: String.typeName)
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:class-default", name:"cos", value:"1", valueClassType: String.typeName)
custZEndPolicy2.resourceProperties << new Property(groupName: "egress QoS class:class-default", name:"cir", value:"150000", valueClassType: String.typeName)


//parent egress
Resource custEndPolicyParent3 = new Resource(action:"create", resourceName: "JANETUK429894-G0/2/0:12-Ethernet-Parent-EgressQoS-template2", type:ResourceType.Policy )
custEndPolicyParent3.deviceId = aend_agn.deviceId
custEndPolicyParent3.resourceProperties << new Property(groupName: "egress QoS class:class-default", name:"template", value:"Ethernet-Parent-EgressQoS-template2", valueClassType: String.typeName)
custEndPolicyParent3.resourceProperties << new Property(groupName: "egress QoS class:class-default", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
custEndPolicyParent3.resourceProperties << new Property(groupName: "egress QoS class:class-default", name:"shape", valueList:["average","1000000000"], valueClassType: String.typeName)
custEndPolicyParent3.resourceProperties << new Property(groupName: "egress QoS class:class-default", name:"service-policy", value:"Ethernet-EgressQoS-Template2", valueClassType: String.typeName)
custEndPolicyParent3.referencedResources = new ConcurrentLinkedQueue<>()
custEndPolicyParent3.referencedResources << custZEndPolicy2.resourceId


//set referenced ingress and egress  service policy
customerVlanServiceInstance.referencedResources << custZEndPolicy1.resourceId << custEndPolicyParent3.resourceId

//NTE ingress policy map
Resource custManNteIngressPolicy = new Resource(action:"create", resourceName: "JANETUK429894-G0/2/0:10-NTE-INGRESS", type:ResourceType.Policy )
custManNteIngressPolicy.deviceId = aend_agn.deviceId
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"template", value:"NTE-INGRESS", valueClassType: String.typeName)
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"direction", value:"ingress", valueClassType: String.typeName)
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"classification_setting_type", value:"qos", valueClassType: String.typeName)
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"cir", value:"400000", valueClassType: String.typeName)
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"bc", value:"12500", valueClassType: String.typeName)
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"qos-group", value:"6", valueClassType: String.typeName)
custManNteIngressPolicy.resourceProperties << new Property(groupName: "ingress QoS class:class-default", name:"mpls", valueList:["experimental", "imposition", "6"], valueClassType: String.typeName)


//link asr 903 aEndDevice to RFS on the order line
oline1.rfs.zEndDevice = aend_agn


oline1.rfs.aEndDevice.impactedResources = [aNteEndResource]
oline1.rfs.zEndDevice.impactedResources = [zEndAgnIfQoSPolicy, zEndPhysIf, custZEndPolicy1, custZEndPolicy2, custEndPolicyParent3, customerVlanServiceInstance]
//redacted: oline1.rfs.zEndDevice.impactedResources = [zEndPhysIf, zEndIfQoSPolicy, customerVlanServiceInstance, custZEndPolicy1, custZEndPolicy2, custEndPolicyParent3]
//oline1.rfs.aEndDevice.impactedResources = [aNteEndResource]

wo1.orderLines << oline1

OrderLine oline2 = new OrderLine(jobRef: 704851143, orderLineNumber: 2, orderLineStatus: "initial", orderLineAction: OrderLineActionType.Create)
oline2.rfs = new ResourceFacingService(type: ServiceType.ManagementVlan,
        serviceName: "eth-nte-mgt-vlan#1",
        serviceDescription: "vf=EWL:cn=JANET:tl=2C03636667",
        rfsAdminStatus: AdminstrativeStateType.Down,
        aEndDevice:aend_agn,
        zEndDevice:zend_agn)

oline2.rfs.rfsProperties << new Property(name:"owning_device", value:"194.159.100.86", valueClassType: String)

Resource bdiInterfaceResource = new Resource( action:"create", resourceName: "BDI1002", type:ResourceType.BridgeDomain)
bdiInterfaceResource.deviceId = aend_agn.deviceId
bdiInterfaceResource.resourceProperties << new Property (name:"description", value: "JANET_BOYDORR_GLW_A112H", valueClassType: String.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"routing_instance", value: "NTE_MGT", valueClassType: String.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"ip", value: "10.236.118.253", valueClassType: String.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"netmask", value: 30, valueClassType: Integer.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"ip_mtu", value: 1518, valueClassType: Integer.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"access_list_in", value: "NTE_MGMT_ACL_IN", valueClassType: String.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"ip_redirects", value: false, valueClassType: Boolean.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"ip_proxy_arp", value: false, valueClassType: Boolean.typeName )
bdiInterfaceResource.resourceProperties << new Property (name:"ntp", value: "disable", valueClassType: String.typeName )


Resource mgtVlanServiceInstance = new Resource(resourceName: "1002 ethernet", type:ResourceType.LogicalSubInterface, resourceAdminStatus: AdminstrativeStateType.Down, action: "create")
mgtVlanServiceInstance.resourceDescription = "vf=4445;cn:JANET:t1=EAGE0363028:olo=ONEA53884160"
mgtVlanServiceInstance.parentId =  zEndPhysIf.resourceId
mgtVlanServiceInstance.deviceId=  aend_agn.deviceId
mgtVlanServiceInstance.referencedResources = new ConcurrentLinkedQueue<>()
mgtVlanServiceInstance.resourceProperties << new Property(name:"encapsulation", value: "dot1q 10", valueClassType: String.typeName)
mgtVlanServiceInstance.resourceProperties << new Property(name:"rewrite", valueList: ["ingress", "tag", "pop", "1", "symmetric"], valueClassType: String.typeName)
mgtVlanServiceInstance.resourceProperties << new Property (name:"service-policy", valueList: ["input", "JANET427902-G0/2/0:10-NTE-INGRESS" ], valueClassType: String.typeName)
mgtVlanServiceInstance.resourceProperties << new Property (name:"bridge-domain", value: "1002", valueClassType: String.typeName)
mgtVlanServiceInstance.referencedResources = new ConcurrentLinkedQueue<>()
mgtVlanServiceInstance.referencedResources << custManNteIngressPolicy.resourceId
mgtVlanServiceInstance.referencedResources << bdiInterfaceResource.resourceId

//clone to steop encoded once logic firing
oline2.rfs.zEndDevice = zend_agn.clone()

oline2.rfs.zEndDevice.impactedResources = [zEndPhysIf.clone(), bdiInterfaceResource, custManNteIngressPolicy, mgtVlanServiceInstance]

wo1.orderLines << oline2

OrderLine oline3 = new OrderLine(jobRef: 704851143, orderLineNumber: 2, orderLineStatus: "initial", orderLineAction: OrderLineActionType.Create)
oline3.rfs = new ResourceFacingService(type: ServiceType.PseudoWire,
        serviceName: "3007297536",
        serviceDescription: "inter-agn interconnect",
        rfsOpStatus: OperationalStateType.Down,
        rfsAdminStatus: AdminstrativeStateType.Unlocked,
        aEndDevice: aend_agn,
        zEndDevice: zend_agn)

oline3.rfs.rfsProperties << new Property(name:"owning_device", value:"194.159.100.86", valueClassType: String)
oline3.rfs.rfsProperties << new Property(name:"pw_id", value:"3007297536", valueClassType: String)
oline3.rfs.rfsProperties << new Property(name:"priority", value:0, valueClassType: Integer)
oline3.rfs.rfsProperties << new Property(name:"remote_peer", value:"194.159.102.88", valueClassType: String)
oline3.rfs.rfsProperties << new Property(name:"pw_mtu", value:"2000", valueClassType: String)

wo1.orderLines << oline3

oline3.rfs.aEndDevice  = aend_agn.clone()
oline3.rfs.zEndDevice  = zend_agn.clone()

Resource cethVlanSI = customerVlanServiceInstance.clone()
cethVlanSI.resourceProperties = new ConcurrentLinkedQueue<>()
cethVlanSI.resourceProperties << new Property(name:"xconnect", valueList:["194.159.100.88", "3007297536", "encapsulation", "mpls" ], valueClassType: String)
oline3.rfs.aEndDevice.impactedResources = [cethVlanSI]



ordGroup.orders = [wo1, wo2]


//res  = jsonGenerator.toTmfJson([wo1, wo2]).encodePrettily()
res  = jsonGenerator.toTmfJson(ordGroup).encodePrettily()

println "tmfjson work order group  : \n$res"


//res2  = jsonGenerator.toJsonApi([wo1, wo2]).encodePrettily()  - breaks with an array
//res2  = jsonGenerator.toJsonApi(ordGroup).encodePrettily()
//println "\n\njsonApi work order group  : $res2"

