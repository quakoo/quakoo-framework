import com.quakoo.baseFramework.serialize.ScloudSerializable;
import com.quakoo.baseFramework.serialize.SerializableProperty;
import com.quakoo.baseFramework.serialize.Type;

/**
 * @author liyongbiao
 */
public class CopyOfBlock implements ScloudSerializable {


    /**
     *
     */
    private static final long serialVersionUID = -1761193712953828923L;
    @SerializableProperty(type = Type.ulong64, index = 1)
    private Long blockId;   //块ID
    @SerializableProperty(type = Type.uint32, index = 2)
    private Integer length; //块长度
    @SerializableProperty(type = Type.hexString, index = 3)
    private String fileSha1; //文件的SHA1的16进制字符串
    @SerializableProperty(type = Type.hexString, index = 4)
    private String md5;  //块MD5的16进制字符串
    @SerializableProperty(type = Type.uint32, index = 5)
    private Integer idcId;
    @SerializableProperty(type = Type.uint32, index = 6)
    private Integer fsType; //块的存储文件系统类别
    @SerializableProperty(type = Type.string, index = 7)
    private String stoMeta; //存储元信息
    @SerializableProperty(type = Type.string, index = 8)
    private String ext;  //附加信息
    @SerializableProperty(type = Type.ulong64, index = 9)
    private Long ctime;
    @SerializableProperty(type = Type.ulong64, index = 10)
    private Long utime;
    @SerializableProperty(type = Type.hexString, index = 11)
    private String indexBlockId; //索引块唯一标识符
    
    
    

    public CopyOfBlock() {
		super();
	}




	public Long getBlockId() {
		return blockId;
	}




	public void setBlockId(Long blockId) {
		this.blockId = blockId;
	}




	public Integer getLength() {
		return length;
	}




	public void setLength(Integer length) {
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




	public Integer getIdcId() {
		return idcId;
	}




	public void setIdcId(Integer idcId) {
		this.idcId = idcId;
	}




	public Integer getFsType() {
		return fsType;
	}




	public void setFsType(Integer fsType) {
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




	public Long getCtime() {
		return ctime;
	}




	public void setCtime(Long ctime) {
		this.ctime = ctime;
	}




	public Long getUtime() {
		return utime;
	}




	public void setUtime(Long utime) {
		this.utime = utime;
	}




	public String getIndexBlockId() {
		return indexBlockId;
	}




	public void setIndexBlockId(String indexBlockId) {
		this.indexBlockId = indexBlockId;
	}

	
}
