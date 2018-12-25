import com.quakoo.baseFramework.serialize.ScloudSerializable;
import com.quakoo.baseFramework.serialize.SerializableProperty;
import com.quakoo.baseFramework.serialize.Type;

/**
 * @author liyongbiao
 */
public class Block implements ScloudSerializable {


    /**
     *
     */
    private static final long serialVersionUID = -1761193712953828923L;
    @SerializableProperty(type = Type.ulong64, index = 1)
    private long blockId;   //块ID
    @SerializableProperty(type = Type.uint32, index = 2)
    private int length; //块长度
    @SerializableProperty(type = Type.hexString, index = 3)
    private String fileSha1; //文件的SHA1的16进制字符串
    @SerializableProperty(type = Type.hexString, index = 4)
    private String md5;  //块MD5的16进制字符串
    @SerializableProperty(type = Type.uint32, index = 5)
    private int idcId;
    @SerializableProperty(type = Type.uint32, index = 6)
    private int fsType; //块的存储文件系统类别
    @SerializableProperty(type = Type.string, index = 7)
    private String stoMeta; //存储元信息
    @SerializableProperty(type = Type.string, index = 8)
    private String ext;  //附加信息
    @SerializableProperty(type = Type.ulong64, index = 9)
    private long ctime;
    @SerializableProperty(type = Type.ulong64, index = 10)
    private long utime;
    @SerializableProperty(type = Type.hexString, index = 11)
    private String indexBlockId; //索引块唯一标识符
    
    
    

    public Block() {
		super();
	}

	public Block(long blockId, int length, String fileSha1, String md5,
			int idcId, int fsType, String stoMeta, String ext, long ctime,
			long utime, String indexBlockId) {
		super();
		this.blockId = blockId;
		this.length = length;
		this.fileSha1 = fileSha1;
		this.md5 = md5;
		this.idcId = idcId;
		this.fsType = fsType;
		this.stoMeta = stoMeta;
		this.ext = ext;
		this.ctime = ctime;
		this.utime = utime;
		this.indexBlockId = indexBlockId;
	}

	public long getBlockId() {
        return blockId;
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getFileSha1() {
        return fileSha1;
    }

    public void setFileSha1(String fileSha1) {
        this.fileSha1 = fileSha1;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getIdcId() {
        return idcId;
    }

    public void setIdcId(int idcId) {
        this.idcId = idcId;
    }

    public int getFsType() {
        return fsType;
    }

    public void setFsType(int fsType) {
        this.fsType = fsType;
    }

    public String getStoMeta() {
        return stoMeta;
    }

    public void setStoMeta(String stoMeta) {
        this.stoMeta = stoMeta;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getUtime() {
        return utime;
    }

    public void setUtime(long utime) {
        this.utime = utime;
    }

    public String getIndexBlockId() {
        return indexBlockId;
    }

    public void setIndexBlockId(String indexBlockId) {
        this.indexBlockId = indexBlockId;
    }

    @Override
    public String toString() {
        return "Block{" +
                "blockId=" + blockId +
                ", length=" + length +
                ", fileSha1='" + fileSha1 + '\'' +
                ", md5='" + md5 + '\'' +
                ", idcId=" + idcId +
                ", fsType=" + fsType +
                ", stoMeta='" + stoMeta + '\'' +
                ", ext='" + ext + '\'' +
                ", ctime=" + ctime +
                ", utime=" + utime +
                ", indexBlockId='" + indexBlockId + '\'' +
                '}';
    }
}
