package com.softwood.utils

import java.net.NetworkInterface
import java.security.SecureRandom
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Enumeration
import java.util.concurrent.atomic.AtomicLong

/**
 * Distributed Sequence Generator.
 * Inspired by Twitter snowflake: https://github.com/twitter/snowflake/tree/snowflake-2010
 *
 * This class should be used as a Singleton.
 * Make sure that you create and reuse a Single instance of SequenceGenerator per node in your distributed system cluster.
 */
public class SequenceGenerator {
    private static final int TOTAL_BITS = 64
    private static final int EPOCH_BITS = 42
    private static final int NODE_ID_BITS = 10
    private static final int SEQUENCE_BITS = 12

    //1023 - 10 bits long
    private static final int maxNodeId = (int)(Math.pow(2, NODE_ID_BITS) - 1)
    //4095 - 12 bits long
    private static final int maxSequence = (int)(Math.pow(2, SEQUENCE_BITS) - 1)

    // Custom Epoch (January 1, 2015 Midnight UTC = 2015-01-01T00:00:00Z)
    private static final long CUSTOM_EPOCH = 1420070400000L

    public static int node

    private final int nodeId

    //volatile guarantees that reads and writes on multiple threads see the same number
    private volatile long lastTimestamp = -1L
    private volatile long sequence = 0L

    //factory instance
    private volatile static SequenceGenerator instance

    //builder option - returns the class, so that build can be called at the end of the method chain
    public static setNode (int id) {
        if(id < 0 || id > maxNodeId) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, maxNodeId))
        }
        node = id
        SequenceGenerator
    }

    /**
     * build()
     * returns a one time factory instance.  Will return the same instance on all calls from any thread
     * @return
     */
    public static synchronized SequenceGenerator build () {

        if (!instance) {
            if (!node)
                instance = new SequenceGenerator()
            else
                instance = SequenceGenerator(node)
        } else
            instance
    }

    // private factory constructor, Create SequenceGenerator with a explicit required nodeId
    private SequenceGenerator(int nodeId) {
        if(nodeId < 0 || nodeId > maxNodeId) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, maxNodeId))
        }
        this.nodeId = nodeId
    }

    // Let factory SequenceGenerator generate a nodeId, bu default using the hash of the mac addresses on network interfaces
    private SequenceGenerator() {
        this.nodeId = createNodeId()
    }


    public String getDateStringForSequence (long seq) {
        getLocalDateTimeForSequence (seq).toString()
    }

    public LocalDateTime getLocalDateTimeForSequence (long seq) {
        long mask = (long) (2**64 - 1) << (TOTAL_BITS - EPOCH_BITS)
        long timeSegment = seq & mask
        long shiftedBackSegment = timeSegment >>  (TOTAL_BITS - EPOCH_BITS)

        //get back to time since standard epoc
        long epochMillis = shiftedBackSegment + CUSTOM_EPOCH


        //LocalDateTime.ofEpochSecond(shiftedBackSegment, 0, ZoneOffset.UTC).toString()
        LocalDateTime dt = new Timestamp (epochMillis).toLocalDateTime()
    }

    /**
     * nextId()
     * a synchronised method that calculates a sequence that is time sortable, and is comprised of
     *
     * time in milliseconds top 64 to 22 bits, followed by 10 bits for the node, and 12 bits for the sequence number
     * within a given millisecond of accuracy from the system clock
     *
     * @return
     */
    public synchronized long nextId() {
        long currentTimestamp = timestamp()

        if(currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!")
        }

        if (currentTimestamp == lastTimestamp) {
            //get masked off next atomic sequence number
            //sequence = aSequence.incrementAndGet() & maxSequence
            sequence = (sequence + 1) & maxSequence;
            //if masked value has cycled
            if(sequence == 0) {
                // Sequence Exhausted, wait till next millisecond.
                currentTimestamp = waitNextMillis(currentTimestamp)
            }
        } else {
            // reset sequence to start with zero for the next millisecond
            //aSequence.set(0L)
            sequence = 0
        }

        lastTimestamp = currentTimestamp;

        //take the timestamp and bit shift it 64-42 = 22 bits
        AtomicLong  seq = new AtomicLong (0)
        long tid = currentTimestamp << (TOTAL_BITS - EPOCH_BITS)

        //OR in the nodeId bit shifted 10 digits
        long nid = (nodeId << (TOTAL_BITS - EPOCH_BITS - NODE_ID_BITS))

        //mask off SEQUENCE_ID_BITS from sequence, 12 digits and OR it onto end of id
        long lid = (sequence & maxSequence)
        //ensure atomic update
        seq.set (tid | nid | lid)

        //seq made of [timestamp|node|sequence] as a long
        return seq.get()
    }


    // Get current timestamp in milliseconds, adjust for the custom epoch.
    private static long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH
    }

    // Block and wait till next millisecond
    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp()
        }
        return currentTimestamp
    }

    /**
     * createNodeId
     * generates a ref number for node by creating a hash from all network interface mac addresses on this node
     * this is masked to ensure its no longer than 10 bits long
     *
     * @return int nodeId
     */
    private int createNodeId() {
        int nodeId
        try {
            StringBuilder sb = new StringBuilder()
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement()
                byte[] mac = networkInterface.getHardwareAddress()
                if (mac != null) {
                    for(int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]))
                    }
                }
            }
            nodeId = sb.toString().hashCode();
        } catch (Exception ex) {
            nodeId = (new SecureRandom().nextInt())
        }

        //mask down to 10 bits long
        nodeId = nodeId & maxNodeId
        return nodeId
    }
}

