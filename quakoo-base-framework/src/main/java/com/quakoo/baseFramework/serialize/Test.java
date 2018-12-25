package com.quakoo.baseFramework.serialize;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.redis.util.JedisXSerializeUtil;
import com.quakoo.baseFramework.redis.util.JedisXSerializeUtil;
import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.secure.MD5Utils;

/**
 * Created by 136249 on 2015/3/14.
 */
public class Test  {

    public static class TestPojo1 implements ScloudSerializable,Serializable{
        @SerializableProperty(type = Type.string, index = 1)
        private String name;
        @SerializableProperty(type = Type.uint32, index = 2)
        private int age;
        @SerializableProperty(type = Type.int32, index = 3)
        private int source;
        @SerializableProperty(type = Type.long64, index = 4)
        private long qq;
        @SerializableProperty(type = Type.ulong64, index = 5)
        private long id;
        @SerializableProperty(type = Type.booleanType, index = 6,isList = true)
        private List<Boolean> man;
        @SerializableProperty(type = Type.floatType, index = 7,isArray = true)
        private float[] profit;
        @SerializableProperty(type = Type.doubleType, index = 8)
        private double money;
        @SerializableProperty(type = Type.hexString, index = 9)
        private String md5;

        private String unSerializable;
        @SerializableProperty(type = Type.pojo, index = 10,isArray = true)
        private TestPojo1[] testPojo;


        @SerializableProperty(type = Type.byteArray, index = 11)
        private byte[] bytes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getSource() {
            return source;
        }

        public void setSource(int source) {
            this.source = source;
        }

        public long getQq() {
            return qq;
        }

        public void setQq(long qq) {
            this.qq = qq;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }


        public double getMoney() {
            return money;
        }

        public void setMoney(double money) {
            this.money = money;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getUnSerializable() {
            return unSerializable;
        }

        public void setUnSerializable(String unSerializable) {
            this.unSerializable = unSerializable;
        }


        public TestPojo1(){

        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }


        public List<Boolean> getMan() {
            return man;
        }

        public void setMan(List<Boolean> man) {
            this.man = man;
        }

        public float[] getProfit() {
            return profit;
        }

        public void setProfit(float[] profit) {
            this.profit = profit;
        }

        public TestPojo1[] getTestPojo() {
            return testPojo;
        }

        public void setTestPojo(TestPojo1[] testPojo) {
            this.testPojo = testPojo;
        }

        public TestPojo1(String name, int age, int source, long qq, long id, List<Boolean> man, float[] profit, double money, String md5, String unSerializable, TestPojo1[] testPojo, byte[] bytes) {
            this.name = name;
            this.age = age;
            this.source = source;
            this.qq = qq;
            this.id = id;
            this.man = man;
            this.profit = profit;
            this.money = money;
            this.md5 = md5;
            this.unSerializable = unSerializable;
            this.testPojo = testPojo;
            this.bytes = bytes;
        }

        @Override
        public String toString() {
            return "TestPojo1{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", source=" + source +
                    ", qq=" + qq +
                    ", id=" + id +
                    ", man=" + man +
                    ", profit=" + Arrays.toString(profit) +
                    ", money=" + money +
                    ", md5='" + md5 + '\'' +
                    ", unSerializable='" + unSerializable + '\'' +
                    ", testPojo=" + Arrays.toString(testPojo) +
                    ", bytes=" + Arrays.toString(bytes) +
                    '}';
        }
    }


    public static class TestPojo2 implements ScloudSerializable,Serializable{
        @SerializableProperty(type = Type.string, index = 1)
        private String name;
        @SerializableProperty(type = Type.uint32, index = 2)
        private int age;
        @SerializableProperty(type = Type.int32, index = 3)
        private int source;
        @SerializableProperty(type = Type.long64, index = 4)
        private long qq;
        @SerializableProperty(type = Type.ulong64, index = 5)
        private long id;
        @SerializableProperty(type = Type.booleanType, index = 6,isList = true)
        private List<Boolean> man;
        @SerializableProperty(type = Type.floatType, index = 7,isArray = true)
        private float[] profit;
        @SerializableProperty(type = Type.doubleType, index = 8)
        private double money;
        @SerializableProperty(type = Type.hexString, index = 9)
        private String md5;
        private String unSerializable;
        @SerializableProperty(type = Type.pojo, index = 10,isArray = true)
        private TestPojo1[] testPojo;
        @SerializableProperty(type = Type.byteArray, index = 11)
        private byte[] bytes;

        @SerializableProperty(type = Type.floatType, index = 12)
        private float newProfit;
        @SerializableProperty(type = Type.long64, index = 13)
        private long newQQ;

        public TestPojo2(){

        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getSource() {
            return source;
        }

        public void setSource(int source) {
            this.source = source;
        }

        public long getQq() {
            return qq;
        }

        public void setQq(long qq) {
            this.qq = qq;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }


        public double getMoney() {
            return money;
        }

        public void setMoney(double money) {
            this.money = money;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getUnSerializable() {
            return unSerializable;
        }

        public void setUnSerializable(String unSerializable) {
            this.unSerializable = unSerializable;
        }



        public float getNewProfit() {
            return newProfit;
        }

        public void setNewProfit(float newProfit) {
            this.newProfit = newProfit;
        }

        public long getNewQQ() {
            return newQQ;
        }

        public void setNewQQ(long newQQ) {
            this.newQQ = newQQ;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }


        public List<Boolean> getMan() {
            return man;
        }

        public void setMan(List<Boolean> man) {
            this.man = man;
        }

        public float[] getProfit() {
            return profit;
        }

        public void setProfit(float[] profit) {
            this.profit = profit;
        }

        public TestPojo1[] getTestPojo() {
            return testPojo;
        }

        public void setTestPojo(TestPojo1[] testPojo) {
            this.testPojo = testPojo;
        }

        public TestPojo2(String name, int age, int source, long qq, long id, List<Boolean> man, float[] profit, double money, String md5, String unSerializable, TestPojo1[] testPojo, byte[] bytes, float newProfit, long newQQ) {
            this.name = name;
            this.age = age;
            this.source = source;
            this.qq = qq;
            this.id = id;
            this.man = man;
            this.profit = profit;
            this.money = money;
            this.md5 = md5;
            this.unSerializable = unSerializable;
            this.testPojo = testPojo;
            this.bytes = bytes;
            this.newProfit = newProfit;
            this.newQQ = newQQ;
        }

        @Override
        public String toString() {
            return "TestPojo2{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", source=" + source +
                    ", qq=" + qq +
                    ", id=" + id +
                    ", man=" + man +
                    ", profit=" + Arrays.toString(profit) +
                    ", money=" + money +
                    ", md5='" + md5 + '\'' +
                    ", unSerializable='" + unSerializable + '\'' +
                    ", testPojo=" + Arrays.toString(testPojo) +
                    ", bytes=" + Arrays.toString(bytes) +
                    ", newProfit=" + newProfit +
                    ", newQQ=" + newQQ +
                    '}';
        }
    }



    public static  void main(String[] fwe) throws Exception {
        TestPojo1 testPojo1=new TestPojo1();
        TestPojo1 test=new TestPojo1("范围范围发违法违规为各位", 18, -10, 125503048, -4444,
                Arrays.asList(new Boolean[]{true,false,true}), new float[]{0.1f,0.2f,0.3f},
                0.2d, MD5Utils.md5ReStr("fwefwefwe".getBytes()), "337853", new TestPojo1[]{testPojo1,testPojo1},"我是个好孩子".getBytes());

        System.out.println(test);
        byte[] bytes=ScloudSerializeUtil.encode(test);
        System.out.println(bytes.length);
        System.out.println(JedisXSerializeUtil.encode(test).length);
        System.out.println(JsonUtils.format(test).getBytes().length);
        TestPojo2 testPojo2= (TestPojo2)ScloudSerializeUtil.decode(bytes,TestPojo2.class);
        System.out.println(testPojo2);
        System.out.println(new String(testPojo2.getBytes()));
        System.out.println(JedisXSerializeUtil.decode(JedisXSerializeUtil.encode(test)));
    }

}
