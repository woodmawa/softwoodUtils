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

    private final int nodeId

    //volatile gaurantees that reads and writes on multiple threads see the same number
    private volatile long lastTimestamp = -1L
    private volatile long sequence = 0L

    //slightly better encapsulation than volatile
    private AtomicLong aSequence = new AtomicLong (0L)

    // Create SequenceGenerator with a explicit required nodeId
    public SequenceGenerator(int nodeId) {
        if(nodeId < 0 || nodeId > maxNodeId) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, maxNodeId))
        }
        this.nodeId = nodeId
    }

    // Let SequenceGenerator generate a nodeId, using the hash of the mac addresses on network interfaces
    public SequenceGenerator() {
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
        long id = 0
        long tid = currentTimestamp << (TOTAL_BITS - EPOCH_BITS)
        //OR in the nodeId bit shited 6 digits
        long nid = (nodeId << (TOTAL_BITS - EPOCH_BITS - NODE_ID_BITS))
        //id |= (nodeId << (TOTAL_BITS - EPOCH_BITS - NODE_ID_BITS))
        //mask off NODE_ID_BITS from sequence and OR it onto end of id
        long lid = (sequence & maxNodeId)
        id |= (tid | nid | lid)

        //id made of [timestamp|node|sequence] as a long
        return id
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

