package uuid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Snow Flake
 *
 * @Author: Heiku
 * @Date: 2019/12/3
 */
public class SnowFlake {

    /**
     * start timeStamp, it's same in id generate system
     */
    private long twepoch = 1575430610327L;


    /**
     * local machine id (0 ~ 31) & dataCenterId (0 ~ 31) & sequence (0 - 4095)
     */
    private long dataCenterId;
    private long workId;
    private long sequence = 0;

    /**
     * machine id 10 bit
     */
    private long dataCenterIdBits = 5L;
    private long workIdBits = 5L;

    /**
     * sequence 32 bit
     */
    private long sequenceBits = 12L;


    private long workerIdShift = sequenceBits;
    private long dataCenterIdShift = sequenceBits + workIdBits;
    private long timeStampLeftShift = sequenceBits + workIdBits + dataCenterIdBits;

    // 0000000000000000000000000000000000000000000000000000111111111111
    private long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimeStamp = -1L;

    public SnowFlake(long workerId, long dataCenterId){
        this.workId = workerId;
        this.dataCenterId = dataCenterId;
    }


    public synchronized long nextId(){
        long timeStamp = timeGen();
        // time call back
        if (timeStamp < lastTimeStamp){
            System.err.printf("Clock is moving backwards. Rejecting requests until %d", lastTimeStamp);
            throw new RuntimeException(String.format(
                    "Clock moved backwards. Refusing to generate id for %d milliseconds", (lastTimeStamp - timeStamp)
            ));
        }

        // if in same milliseconds, change sequence in same milliseconds
        if (timeStamp == lastTimeStamp){
            // get current sequence
            sequence = (sequence + 1) & sequenceMask;
            // sequence not change, turn to next timeStamp
            if (sequence == 0){
                timeStamp = tilNextMillis(lastTimeStamp);
            }
        }else {
            sequence = 0;
        }
        // update timeStamp record
        lastTimeStamp = timeStamp;

        // generate id
        return ((timeStamp - twepoch) << timeStampLeftShift) |
                (dataCenterId << dataCenterIdShift) |
                (workId << workerIdShift) |
                sequence;
    }



    // get timeStamp
    private long tilNextMillis(long lastTimeStamp){
        long timeStamp = timeGen();
        while (timeStamp <= lastTimeStamp){
            timeStamp = timeGen();
        }
        return timeStamp;
    }

    private long timeGen(){
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SnowFlake snowFlake = new SnowFlake(1, 1);
        for (int i = 0; i < 30; i++) {
            System.out.println(snowFlake.nextId());
        }
    }
}
