package com.quakoo.baseFramework.serialize;

import java.util.Arrays;
import java.util.List;


/**
 * Created by 136249 on 2015/3/14.
 */
public class TestList  implements ScloudSerializable{
	
	 @SerializableProperty(type = Type.pojo, index = 1,isList = true)
	  private List<Test.TestPojo1> blocks; 

    public List<Test.TestPojo1> getBlocks() {
		return blocks;
	}


	public void setBlocks(List<Test.TestPojo1> blocks) {
		this.blocks = blocks;
	}



	public static  void main(String[] fwe) throws Exception {
		TestList testList=new TestList();
		Test.TestPojo1 testPojo1=new Test.TestPojo1();
		testList.setBlocks(Arrays.asList(new Test.TestPojo1[]{testPojo1}));
		byte[] bytes=ScloudSerializeUtil.encode(testList);
		ScloudSerializeUtil.decode(bytes, TestList.class);
    }

}
