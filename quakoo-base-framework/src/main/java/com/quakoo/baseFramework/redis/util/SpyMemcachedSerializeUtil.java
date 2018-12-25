package com.quakoo.baseFramework.redis.util;
import com.quakoo.baseFramework.redis.transcoders.CachedData;
import com.quakoo.baseFramework.redis.transcoders.SerializingTranscoder;
import com.quakoo.baseFramework.redis.transcoders.CachedData;
import com.quakoo.baseFramework.redis.transcoders.SerializingTranscoder;


/**
 * 
 * @author LiYongbiao
 *
 */
public class SpyMemcachedSerializeUtil {
    public static byte[] encode(Object o) {
        CachedData cachedData = new SerializingTranscoder().encode(o);
        byte[] data = cachedData.getData();
        int spyFlag = cachedData.getFlag();
        byte[] ret = new byte[data.length + 1];
        System.arraycopy(data, 0, ret, 1, data.length);
        byte flag = (byte) (spyFlag >> 8);
//        if((spyFlag & SerializingTranscoder.COMPRESSED) != 0){//it compressed
//            flag |=0x80;
//        }
        ret[0] = flag;
        return ret;
    }

    public static Object decode(byte[] bArr) {
        int flag = 0;
        flag = (bArr[0] & 0x000000ff) << 8;
//        flag |=(((int)(bArr[0]&0x7f))<<8);
//        if((bArr[0]&0x80)!=0){//it compressed
//            flag |= SerializingTranscoder.COMPRESSED;
//        }
        byte[] data = new byte[bArr.length - 1];
        System.arraycopy(bArr, 1, data, 0, data.length);

        CachedData cachedData = new CachedData(flag, data);
        Object o = new SerializingTranscoder().decode(cachedData);
        return o;
    }
}
