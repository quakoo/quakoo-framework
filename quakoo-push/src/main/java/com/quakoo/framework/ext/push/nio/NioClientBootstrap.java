package com.quakoo.framework.ext.push.nio;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.model.constant.Brand;
import com.quakoo.framework.ext.push.model.constant.Platform;
import com.quakoo.framework.ext.push.model.param.NioRequest;
import com.quakoo.framework.ext.push.model.param.RegistRequest;
import com.quakoo.framework.ext.push.model.param.StringDecoder;
import com.quakoo.framework.ext.push.model.param.StringEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NioClientBootstrap {

	public static void main(String[] args) throws Exception {
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.group(eventLoopGroup);
		bootstrap.remoteAddress("127.0.0.1", 11111);
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel socketChannel)
					throws Exception {
				ChannelPipeline p = socketChannel.pipeline();
				p.addLast(new StringEncoder());
				p.addLast(new StringDecoder());
				p.addLast(new ClientHandler());
			}
		});
		ChannelFuture future = bootstrap.connect("127.0.0.1", 11111).sync();
		if (future.isSuccess()) {
			System.out.println("connect server  成功---------");
		}
		SocketChannel socketChannel = (SocketChannel) future.channel();
		RegistRequest registRequest = new RegistRequest();
		registRequest.setUid(2);
		registRequest.setType(NioRequest.type_regist);
		registRequest.setPlatform(Platform.android);
		registRequest.setSessionId(String.valueOf(System.currentTimeMillis()));
		registRequest.setPhoneSessionId(String.valueOf(2));
		registRequest.setBrand(Brand.common);

		String regist = JsonUtils.toJson(registRequest);
		boolean active = socketChannel.isActive();
		if (active) {
			socketChannel.writeAndFlush(regist);
		} else {
		}

		// for(int i = 1; i <= 10; i++) {
		// new Thread(new Client(i)).start();
		// }
	}
}

class Client implements Runnable {

	private long uid;

	public Client(long uid) {
		this.uid = uid;
	}

	@Override
	public void run() {
		try {
			EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.group(eventLoopGroup);
			bootstrap.remoteAddress("127.0.0.1", 11111);
			bootstrap.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel socketChannel)
						throws Exception {
					ChannelPipeline p = socketChannel.pipeline();
					p.addLast(new StringEncoder());
					p.addLast(new StringDecoder());
					p.addLast(new ClientHandler());
				}
			});
			ChannelFuture future = bootstrap.connect("127.0.0.1", 11111).sync();
			if (future.isSuccess()) {
				System.out.println("connect server  成功---------");
			}
			SocketChannel socketChannel = (SocketChannel) future.channel();
			RegistRequest registRequest = new RegistRequest();
			registRequest.setUid(uid);
			registRequest.setType(NioRequest.type_regist);
			registRequest.setPlatform(Platform.android);
			registRequest.setSessionId(String.valueOf(System
					.currentTimeMillis()));
			registRequest.setPhoneSessionId(String.valueOf(uid));
			registRequest.setBrand(Brand.common);

			String regist = JsonUtils.toJson(registRequest);
			boolean active = socketChannel.isActive();
			if (active) {
				socketChannel.writeAndFlush(regist);
			} else {
			}
		} catch (Exception e) {
		}
	}

}
