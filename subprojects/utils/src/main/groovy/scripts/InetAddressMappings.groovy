package scripts

import inet.ipaddr.HostName
import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString

Inet4Address v4 = Inet4Address.getByName("www.google.com")

println "v4 address is " + v4.address + " and as str : " + v4.hostAddress

println "v6 : " + Inet6Address.getByName("www.google.com")

InetAddress[] inet = InetAddress.getByName("www.google.com")

def v6Addresses = []
def v4Addresses = []

for (InetAddress addr : inet) {
    println "\t > " + addr
    if (addr instanceof Inet6Address) {
        v6Addresses << (Inet6Address) addr
    } else if (addr instanceof Inet4Address) {
        v4Addresses << (Inet4Address) addr

    }

}

println "v6 addresses: " + v6Addresses
println "v4 addresses: " + v4Addresses

println "----"

HostName hn = new HostName("www.google.com")//("192.168.1.254") //"[::1]:80");
assert hn
IPAddress ip = hn.getAddress()
assert ip.isIPv4()
assert ip.isIPv6Convertible()
println "all std strings :" + ip.toStandardStrings()
println "to conv v6 @ : " + hn.getAddress().toIPv6()
println "conv string " + ip.toConvertedString()

printPrefixedAddresses(ip.toString() + "/24")

//
// check (hn)


IPAddress v6 = hn.asAddress(IPAddress.IPVersion.IPV6)


new IPAddressString("192.168.1.254")

void check(HostName host) {

    if (host.isAddress()) {

        println("address: " + host.asAddress().toCanonicalString());

    } else if (host.isAddressString()) {

        println("address string with ambiguous address: " +

                host.asAddressString());

    } else {

        println("host name with labels: " +

                Arrays.asList(host.getNormalizedLabels()));

    }

}

void printPrefixedAddresses(String addressStr) {

    IPAddressString ipAddressString = new IPAddressString(addressStr);

    IPAddress address = ipAddressString.getAddress();

    println "--Summary---"

    println("ip @ count: " + address.getCount());

    IPAddress hostAddress = ipAddressString.getHostAddress();

    IPAddress prefixBlock = address.toPrefixBlock();

    Integer prefixLength = ipAddressString.getNetworkPrefixLength();
    println(address);

    println "wildcard format: " + address.toCanonicalWildcardString()

    println "host address: " + hostAddress

    println "prefix length: " + prefixLength

    println "prefix block : " + prefixBlock

    println "-----"

}