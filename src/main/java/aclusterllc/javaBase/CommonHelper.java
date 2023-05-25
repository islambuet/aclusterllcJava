package aclusterllc.javaBase;

import org.json.JSONObject;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CommonHelper {
    public static long bytesToLong(byte[] bytes)
    {
        return new BigInteger(bytes).longValue();
//        byte[] fillArray = new byte[Long.BYTES - bytes.length];
//        byte[] longArray = joinTwoBytesArray(fillArray, bytes);
//        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
//        buffer.put(longArray);
//        buffer.flip();//need flip
//        return buffer.getLong();
    }

    public static int bytesToInt(byte[] bytes)
    {
        return new BigInteger(bytes).intValue();
    }
    public static byte[] longToBytes(long x, int byteLength) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        byte[] wordArray = buffer.array();
        byte[] requiredByteArray = new byte[byteLength];
        int wordArrayLength = wordArray.length;
        int ignoredLength = wordArrayLength - byteLength;
        for (int i=0; i < wordArrayLength; i++) {
            if(i > (ignoredLength - 1)) {
                requiredByteArray[i-ignoredLength] = wordArray[i];
            }
        }
        return requiredByteArray;
    }
    public static byte[] joinTwoBytesArray(byte[] a, byte[] b) {
        byte[] returnArray = new byte[a.length + b.length];
        ByteBuffer buff = ByteBuffer.wrap(returnArray);
        buff.put(a);
        buff.put(b);
        return buff.array();
    }
}
