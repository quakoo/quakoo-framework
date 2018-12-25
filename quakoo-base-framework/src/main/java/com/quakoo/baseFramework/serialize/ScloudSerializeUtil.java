package com.quakoo.baseFramework.serialize;

import com.quakoo.baseFramework.redis.util.HessianSerializeUtil;
import com.quakoo.baseFramework.util.ByteUtil;
import com.quakoo.baseFramework.redis.util.HessianSerializeUtil;
import com.quakoo.baseFramework.secure.Hex;
import com.quakoo.baseFramework.util.ByteUtil;
import redis.clients.util.SafeEncoder;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 136249 on 2015/3/14.
 */
public abstract class ScloudSerializeUtil implements Serializable {

    static volatile Map<Class, TreeMap<Integer, SerializableField>> map = new ConcurrentHashMap<>();

    public static TreeMap<Integer, SerializableField> getSerializableField(Class clazz) {
        TreeMap<Integer, SerializableField> result = map.get(clazz);
        if (result == null) {
            synchronized (ScloudSerializeUtil.map) {
                if (map.get(clazz) == null) {
                    try {
                        TreeMap<Integer, SerializableField> serializableProperties = new TreeMap<>();
                        PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
                        for (PropertyDescriptor pd : pds) {
                            if ("class".equals(pd.getName())) {
                                continue;
                            }
                            String name = pd.getName();
                            Field field = clazz.getDeclaredField(name);
                            SerializableProperty serializableProperty = field.getAnnotation(SerializableProperty.class);
                            if (serializableProperty == null) {
                                continue;
                            }
                            
                            Class realFieldType=field.getType();
                            if(!ScloudSerializable.class.equals(serializableProperty.pojoClass())){
                            	realFieldType=serializableProperty.pojoClass();
                            }else if(serializableProperty.isArray() ){
                            	realFieldType=realFieldType.getComponentType();
                            }else if(serializableProperty.isList() ){
                            	java.lang.reflect.Type mapMainType = field.getGenericType();
                            	if (mapMainType instanceof ParameterizedType) {  
                                    ParameterizedType parameterizedType = (ParameterizedType)mapMainType;
                                    java.lang.reflect.Type[] types = parameterizedType.getActualTypeArguments();
                                    if(types==null||types.length!=1){
                                    	throw new SerializeException("find type error!,list genericType num is error ");
                                    }
                                   
                                    realFieldType=(Class) types[0];
                                } else {  
                                   throw new SerializeException("find type error!,list no genericType");
                                }  
                            }
                            
                            SerializableField serializableField = new SerializableField(serializableProperty,
                                    pd.getReadMethod(), pd.getWriteMethod(), realFieldType);
                            if (serializableProperties.containsKey(serializableProperty.index())) {
                                throw new SerializeException("index error");
                            }
                            serializableProperties.put(serializableProperty.index(), serializableField);
                        }
                        ScloudSerializeUtil.map.put(clazz, serializableProperties);
                    } catch (Exception e) {
                        throw new SerializeException("class:"+clazz.getName() + " init error ,check your code!", e);
                    }
                }
                result = map.get(clazz);
            }
        }
        return result;
    }

    public static byte[] encode(ScloudSerializable scloudSerializable) {
        try {
            TreeMap<Integer, SerializableField> serializableFieldTree = getSerializableField(scloudSerializable.getClass());
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bo);
            for (Map.Entry<Integer,SerializableField> entry : serializableFieldTree.entrySet()) {
                SerializableField serializableField = entry.getValue();
                Type type = serializableField.getSerializableProperty().type();
                Object value = serializableField.getReadMethod().invoke(scloudSerializable);
                boolean isArray = serializableField.getSerializableProperty().isArray();
                boolean isList = serializableField.getSerializableProperty().isList();
                encode(type, value, dos, isArray, isList);
            }
            return bo.toByteArray();
        } catch (Exception e) {
            throw new SerializeException(e);
        }

    }


    private static void encode(Type type, Object value, DataOutputStream dos, boolean isArray, boolean isList) throws IOException {
        if (isArray) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                ByteArrayOutputStream subbo = new ByteArrayOutputStream();
                DataOutputStream subDos = new DataOutputStream(subbo);
                int len = Array.getLength(value);
                for (int i = 0; i < len; i++) {
                    Object subValue = Array.get(value, i);
                    encode(type, subValue, subDos, false, false);
                }
                byte[] arrayBytes = subbo.toByteArray();
                int length = arrayBytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(arrayBytes);
            }
        }else if (isList) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                ByteArrayOutputStream subbo = new ByteArrayOutputStream();
                DataOutputStream subDos = new DataOutputStream(subbo);
                for (Object subValue : (List) value) {
                    encode(type, subValue, subDos, false, false);
                }
                byte[] arrayBytes = subbo.toByteArray();
                int length = arrayBytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(arrayBytes);
            }
        }else  if (type == Type.hexString) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                byte[] bytes = Hex.hexStringToBytes((String) value);
                int length = bytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(bytes);
            }
        } else if (type == Type.int32) {
            Varint.writeSignedVarInt((int) value, dos);
        } else if (type == Type.uint32) {
            Varint.writeUnsignedVarInt((int) value, dos);
        } else if (type == Type.long64) {
            Varint.writeSignedVarLong((long) value, dos);
        } else if (type == Type.ulong64) {
            Varint.writeUnsignedVarLong((long) value, dos);
        } else if (type == Type.string) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                byte[] bytes = SafeEncoder.encode((String) value);
                int length = bytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(bytes);
            }
        } else if (type == Type.byteArray) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                byte[] bytes = ((byte[]) value);
                int length = bytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(bytes);
            }
        } else if (type == Type.floatType) {
            byte[] bytes = ByteUtil.putFloat((float) value);
            dos.write(bytes);

        } else if (type == Type.doubleType) {
            byte[] bytes = ByteUtil.putDouble((double) value);
            dos.write(bytes);

        } else if (type == Type.shortType) {
            byte[] bytes = ByteUtil.putShort((short) value);
            dos.write(bytes);

        } else if (type == Type.booleanType) {
            if ((boolean) value) {
                Varint.writeUnsignedVarInt(1, dos);
            } else {
                Varint.writeUnsignedVarInt(0, dos);
            }
        } else if (type == Type.characterType) {
            byte[] bytes = ByteUtil.putChar((char) value);
            dos.write(bytes);
        } else if (type == Type.byteType) {
            dos.write((byte) value);
        } else if (type == Type.pojo) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                byte[] bytes = encode((ScloudSerializable) value);
                int length = bytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(bytes);
            }
        } else if (type == Type.other) {
            if (value == null) {
                Varint.writeUnsignedVarInt(0, dos);
            } else {
                byte[] bytes = HessianSerializeUtil.encode(value);
                int length = bytes.length;
                Varint.writeUnsignedVarInt(length, dos);
                dos.write(bytes);
            }
        } else {
            throw new SerializeException("not notSupported type:" + type);
        }
    }


    public static<T extends ScloudSerializable> T decode(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bi);
        Object value=null ;
        SerializableField serializableField=null;
        try {
            TreeMap<Integer, SerializableField> serializableFieldTree = getSerializableField(clazz);
            ScloudSerializable result = (ScloudSerializable) clazz.newInstance();
            for (Map.Entry<Integer,SerializableField> entry : serializableFieldTree.entrySet()) {
                serializableField = entry.getValue();
                Type type = serializableField.getSerializableProperty().type();
                if (dis.available() == 0) {
                    break;
                }
                boolean isArray = serializableField.getSerializableProperty().isArray();
                boolean isList = serializableField.getSerializableProperty().isList();

                value = decode(type, dis, isArray, isList, serializableField);
                if(value!=null){
                	serializableField.getWriteMethod().invoke(result, value);
                }
            }
            return (T) result;
        } catch (Exception e) {
            throw new SerializeException("serializableField:"+serializableField+",value:"+value,e);
        }
    }

    private static Object decode(Type type,
                                 DataInputStream dis, boolean isArray,
                                 boolean isList, SerializableField serializableField)
            throws IOException, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (isArray || isList) {
            int length = Varint.readUnsignedVarInt(dis);
            if (length > 0) {
                byte[] subBytes = new byte[length];
                dis.readFully(subBytes);
                ByteArrayInputStream subbi = new ByteArrayInputStream(subBytes);
                DataInputStream subdis = new DataInputStream(subbi);
                List list = new ArrayList();
                while (subdis.available() > 0) {
                    Object value = decode(type, subdis, false, false, serializableField);
                    list.add(value);
                }
                if (isList) {
                    return list;
                } else {
                    Object result=Array.newInstance( serializableField.getRealFieldType(), list.size());
                    for(int i=0;i<list.size();i++){
                        Array.set(result, i, list.get(i));
                    }
                    return result;
                }
            }else{
            	return null;
            }
        }


        if (type == Type.hexString) {
            int length = Varint.readUnsignedVarInt(dis);
            if (length > 0) {
                byte[] hexByte = new byte[length];
                dis.readFully(hexByte);
                return Hex.bytesToHexString(hexByte);
            }
        } else if (type == Type.int32) {
            return Varint.readSignedVarInt(dis);
        } else if (type == Type.uint32) {
            return Varint.readUnsignedVarInt(dis);
        } else if (type == Type.long64) {
            return Varint.readSignedVarLong(dis);
        } else if (type == Type.ulong64) {
            return Varint.readUnsignedVarLong(dis);
        } else if (type == Type.string) {

            int length = Varint.readUnsignedVarInt(dis);
            if (length > 0) {
                byte[] strbyte = new byte[length];
                dis.readFully(strbyte);
                return  SafeEncoder.encode(strbyte);
            }
        } else if (type == Type.byteArray) {
            int length = Varint.readUnsignedVarInt(dis);
            if (length > 0) {
                byte[] byteArray = new byte[length];
                dis.readFully(byteArray);
                return byteArray;
            }
        } else if (type == Type.floatType) {
            byte[] floatByte = new byte[ByteUtil.SIZEOF_FLOAT];
            dis.readFully(floatByte);
            return ByteUtil.getFloat(floatByte);
        } else if (type == Type.doubleType) {
            byte[] doubleByte = new byte[ByteUtil.SIZEOF_DOUBLE];
            dis.readFully(doubleByte);
            return ByteUtil.getDouble(doubleByte);
        } else if (type == Type.shortType) {
            byte[] shortByte = new byte[ByteUtil.SIZEOF_SHORT];
            dis.readFully(shortByte);
            return ByteUtil.getShort(shortByte);

        } else if (type == Type.booleanType) {
            int value = Varint.readUnsignedVarInt(dis);
            return (value == 1);
        } else if (type == Type.characterType) {
            byte[] charByte = new byte[ByteUtil.SIZEOF_CHAR];
            dis.readFully(charByte);
            return ByteUtil.getChar(charByte);
        } else if (type == Type.byteType) {
            byte[] singleByte = new byte[ByteUtil.SIZEOF_BYTE];
            dis.readFully(singleByte);
            return singleByte;
        } else if (type == Type.pojo) {
            int length = Varint.readUnsignedVarInt(dis);
            if (length > 0) {
                byte[] obytes = new byte[length];
                dis.readFully(obytes);
                return decode(obytes, serializableField.getRealFieldType());
                
            }
        } else if (type == Type.other) {
            int length = Varint.readUnsignedVarInt(dis);
            if (length > 0) {
                byte[] hessianBytes = new byte[length];
                dis.readFully(hessianBytes);
                return HessianSerializeUtil.decode(hessianBytes);
            }
        } else {
            throw new SerializeException("not notSupported type:" + type);
        }
        return null;
    }


    public static class SerializableField {
        private SerializableProperty serializableProperty;
        private Method readMethod;
        private Method writeMethod;
        private Class realFieldType;

        public SerializableField(SerializableProperty serializableProperty, Method readMethod, Method writeMethod, Class realFieldType) {
            this.serializableProperty = serializableProperty;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.realFieldType = realFieldType;
        }

        public SerializableProperty getSerializableProperty() {
            return serializableProperty;
        }

        public void setSerializableProperty(SerializableProperty serializableProperty) {
            this.serializableProperty = serializableProperty;
        }

        public Method getReadMethod() {
            return readMethod;
        }

        public void setReadMethod(Method readMethod) {
            this.readMethod = readMethod;
        }

        public Method getWriteMethod() {
            return writeMethod;
        }

        public void setWriteMethod(Method writeMethod) {
            this.writeMethod = writeMethod;
        }

        
       
		public Class getRealFieldType() {
			return realFieldType;
		}

		public void setRealFieldType(Class realFieldType) {
			this.realFieldType = realFieldType;
		}

		@Override
		public String toString() {
			return "SerializableField [serializableProperty="
					+ serializableProperty + ", readMethod=" + readMethod
					+ ", writeMethod=" + writeMethod + ", realFieldType="
					+ realFieldType + "]";
		}
        
    }

}
