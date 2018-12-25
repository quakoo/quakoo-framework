package com.quakoo.framework.ext.push.nio;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.util.ByteUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class ChannelUtils {
	
	public static void write(ChannelHandlerContext ctx, Object obj, boolean close) {
		try{
			if(ctx==null){
				return;
			}
			String json=JsonUtils.toJson(obj);
			byte[] bytes =json.getBytes();
			byte[] head=ByteUtil.putInt(bytes.length);
			byte[] result=new byte[bytes.length+head.length];
			for(int i=0;i<head.length;i++){
				result[i]=head[i];
			}
			for(int i=0;i<bytes.length;i++){
				result[i+head.length]=bytes[i];
			}
			ByteBuf buf= Unpooled.copiedBuffer(result);
			ctx.writeAndFlush(buf);
			if(close) ctx.close();
		}catch(Exception e){
		    e.printStackTrace();
		    try{
		    	ctx.close();
		    }catch(Exception e1){
		    	
		    }
		}
	}

}
