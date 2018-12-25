package com.quakoo.baseFramework.redis.util;
import java.util.Date;

import com.quakoo.baseFramework.redis.transcoders.CachedData;
import com.quakoo.baseFramework.redis.transcoders.SerializingTranscoder;


/**
 * 
 * @author LiYongbiao
 *
 */
public class JedisXSerializeUtil {
    private final static byte userSpy = 0x01;
    private final static byte userHessian2 = 0x02;

    private static boolean isUseSpySerialize(Object o) {
        if (o instanceof String) {
            return true;
        } else if (o instanceof Long) {
            return true;
        } else if (o instanceof Integer) {
            return true;
        } else if (o instanceof Short) {
            return true;
        } else if (o instanceof Boolean) {
            return true;
        } else if (o instanceof Character) {
            return true;
        } else if (o instanceof Date) {
            return true;
        } else if (o instanceof Byte) {
            return true;
        } else if (o instanceof Float) {
            return true;
        } else if (o instanceof Double) {
            return true;
        } else if (o instanceof byte[]) {
            return true;
        }
        return false;
    }

    public static byte[] encode(Object o) {
        if(o==null)return null;
        if (isUseSpySerialize(o)) {
            CachedData cachedData = new SerializingTranscoder().encode(o);
            byte[] data = cachedData.getData();
            int spyFlag = cachedData.getFlag();
            byte[] ret = new byte[data.length + 2];
            System.arraycopy(data, 0, ret, 2, data.length);
            byte flag = (byte) (spyFlag >> 8);
            ret[1] = flag;
            ret[0] = userSpy;//
            return ret;

        } else {
            byte[] data = HessianSerializeUtil.encode(o);
            byte[] ret = new byte[data.length + 1];
            System.arraycopy(data, 0, ret, 1, data.length);
            ret[0] = userHessian2;
            return ret;

        }

    }

    public static Object decode(byte[] bArr) {
        if (bArr[0] == userSpy) {
            int flag = 0;
            flag = (bArr[1] & 0x000000ff) << 8;
            byte[] data = new byte[bArr.length - 2];
            System.arraycopy(bArr, 2, data, 0, data.length);

            CachedData cachedData = new CachedData(flag, data);
            Object o = new SerializingTranscoder().decode(cachedData);
            return o;

        } else if (bArr[0] == userHessian2) {
            byte[] data = new byte[bArr.length - 1];
            System.arraycopy(bArr, 1, data, 0, data.length);
            return HessianSerializeUtil.decode(data);
        }
        return null;
    }
}
