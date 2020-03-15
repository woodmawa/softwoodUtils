/*
 * Copyright 2018 author : Will woodman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.softwood.utils

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator

import java.time.LocalDateTime
import java.time.ZoneOffset

class UuidUtil {

    private static final NoArgGenerator timeBasedGenerator = Generators.timeBasedGenerator()
    //private static final long clockSeqAndNode = makeClockSeqAndNode()


    /**
     * From UUID javadocs the resulting timestamp is measured in 100-nanosecond units since midnight, October 15, 1582 UTC
     * timestamp() from UUID is measured in 100-nanosecond units since midnight, October 15, 1582 UTC
     *
     * The Java timestamp in milliseconds since 1970-01-01 as baseline
     *
     * @return
     */
    static long getStartOfUuidRelativeToUnixEpochInMilliseconds () {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"))
        c.set(Calendar.YEAR, 1582)
        c.set(Calendar.MONTH, Calendar.OCTOBER)
        c.set(Calendar.DAY_OF_MONTH, 15)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)

        return c.getTimeInMillis()
    }

    //https://www.wolframalpha.com/input/?i=convert+1582-10-15+UTC+to+unix+time
    final static long START_OF_UUID_RELATIVE_TO_UNIX_EPOCH_SECONDS = -12219292800L
    final static long START_OF_UUID_RELATIVE_TO_UNIX_EPOCH_MILLIS = -12219292800L * 1000L

    /**
     * timestamp() from UUID is measured in 100-nanosecond units since midnight, October 15, 1582 UTC,
     * so we must convert for 100ns units to millisecond procession
     * @param tuid
     * @return
     */
    static long getMillisecondsFromUuid (UUID tuid) {

        assert tuid.version() == 1      //ensure its a time based UUID

        // timestamp returns in 10^-7 (100 nano second chunks),
        // java Date constructor  assumes 10^-3 (millisecond precision)
        // so we have to divide by 10^4 (10,000) to get millisecond precision
        long milliseconds_since_UUID_baseline = tuid.timestamp() /10000L

    }

    static getDateFromUuid (UUID tuid) {
        // Allocates a Date object and initializes it to represent the specified number of milliseconds since the
        // standard java (unix) base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT
        // have to add relative offset from UUID start date of unix epoch to get start date in unix time milliseconds
        new Date (getMillisecondsFromUuid (tuid) + START_OF_UUID_RELATIVE_TO_UNIX_EPOCH_MILLIS )
    }

    static getLocalDateTimeFromUuid (UUID tuid) {
        // Allocates a Date object and initializes it to represent the specified number of milliseconds since the
        // standard java (unix) base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT
        // have to add relative offset from UUID start date of unix epoch to get start date in unix time milliseconds
        long epochSec = (getMillisecondsFromUuid (tuid) + START_OF_UUID_RELATIVE_TO_UNIX_EPOCH_MILLIS)/1000
        LocalDateTime.ofEpochSecond(epochSec, 0, ZoneOffset.UTC)
    }
    static UUID getTimeBasedUuid () {
        UUID tuid = timeBasedGenerator.generate()
    }


    /*****
     *
     * // https://www.programcreek.com/java-api-examples/?code=Netflix/sstable-adaptor/sstable-adaptor-master/sstable-adaptor-cassandra/src/main/java/org/apache/cassandra/utils/UUIDGen.java
    private static long makeClockSeqAndNode()
    {
        long clock = new SecureRandom().nextLong();

        long lsb = 0;
        lsb |= 0x8000000000000000L;                 // variant (2 bits)
        lsb |= (clock & 0x0000000000003FFFL) << 48; // clock sequence (14 bits)
        lsb |= makeNode();                          // 6 bytes
        return lsb;
    }

    private static long makeNode()
    {  */
        /*
         * We don't have access to the MAC address but need to generate a node part
         * that identify this host as uniquely as possible.
         * The spec says that one option is to take as many source that identify
         * this node as possible and hash them together. That's what we do here by
         * gathering all the ip of this host.
         * Note that FBUtilities.getBroadcastAddress() should be enough to uniquely
         * identify the node *in the cluster* but it triggers DatabaseDescriptor
         * instanciation and the UUID generator is used in Stress for instance,
         * where we don't want to require the yaml.
         */
    /*
        Collection<InetAddress> localAddresses = FBUtilities.getAllLocalAddresses();
        if (localAddresses.isEmpty())
            throw new RuntimeException("Cannot generate the node component of the UUID because cannot retrieve any IP addresses.");

        // ideally, we'd use the MAC address, but java doesn't expose that.
        byte[] hash = hash(localAddresses);
        long node = 0;
        for (int i = 0; i < Math.min(6, hash.length); i++)
            node |= (0x00000000000000ff & (long)hash[i]) << (5-i)*8;
        assert (0xff00000000000000L & node) == 0;

        // Since we don't use the mac address, the spec says that multicast
        // bit (least significant bit of the first octet of the node ID) must be 1.
        return node | 0x0000010000000000L;
    }

    private static byte[] hash(Collection<InetAddress> data)
    {
        try
        {
            // Identify the host.
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            for(InetAddress addr : data)
                messageDigest.update(addr.getAddress());

            //TODO: Minh - is it ok?
            // Identify the process on the load: we use both the PID and class loader hash.
            //long pid = SigarLibrary.instance.getPid();
            //if (pid < 0)
            long pid = new Random(System.currentTimeMillis()).nextLong();
            FBUtilities.updateWithLong(messageDigest, pid);

            ClassLoader loader = UUIDGen.class.getClassLoader();
            int loaderId = loader != null ? System.identityHashCode(loader) : 0;
            FBUtilities.updateWithInt(messageDigest, loaderId);

            return messageDigest.digest();
        }
        catch (NoSuchAlgorithmException nsae)
        {
            throw new RuntimeException("MD5 digest algorithm is not available", nsae);
        }
    }
    */
}
