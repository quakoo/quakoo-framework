package com.quakoo.framework.ext.chat.nio;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.model.param.nio.PingRequest;
import com.quakoo.framework.ext.chat.model.param.nio.StringDecoder;
import com.quakoo.framework.ext.chat.model.param.nio.StringEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class NioClientBootstrap {

	private static void test() throws Exception {
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.group(eventLoopGroup);
		bootstrap.remoteAddress("182.92.191.75", 11111); //47.96.9.239 182.92.191.75 47.104.104.227
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel socketChannel)
					throws Exception {
				ChannelPipeline p = socketChannel.pipeline();
				p.addLast(new IdleStateHandler(0, 0, 10));
				p.addLast(new StringEncoder());
				p.addLast(new StringDecoder());
				p.addLast(new ClientHandler());
			}
		});
		ChannelFuture future = bootstrap.connect("182.92.191.75", 11111)
				.sync();
		if (future.isSuccess()) {
			System.out.println("connect server  成功---------");
		}
		SocketChannel socketChannel = (SocketChannel) future.channel();
        for(int i = 0; i < 3; i++) {
        	Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    		PingRequest connectRequest = new PingRequest(67, 0);
    		String connect = JsonUtils.toJson(connectRequest);
    		boolean active = socketChannel.isActive();
    		if (active) {
    			System.out.println("send");
    			socketChannel.writeAndFlush(connect);
    		} else {
    		}
        }
	}
	
	public static void main(String[] args) throws Exception{
//		for(int i = 1; i <= 10; i++) {
//			new Thread(new Client(i)).start();
//		}
		test();
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
//					p.addLast(new IdleStateHandler(0, 0, 10));
					p.addLast(new StringEncoder());
					p.addLast(new StringDecoder());
					p.addLast(new ClientHandler());
				}
			});
			ChannelFuture future = bootstrap.connect("127.0.0.1", 11111)
					.sync();
			if (future.isSuccess()) {
				System.out.println("connect server  成功---------");
			}
			SocketChannel socketChannel = (SocketChannel) future.channel();
			// Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
			PingRequest connectRequest = new PingRequest(uid, 0);
			String connect = JsonUtils.toJson(connectRequest);
			boolean active = socketChannel.isActive();
			if (active) {
				System.out.println("send");
				socketChannel.writeAndFlush(connect);
			} else {
			}
		} catch (Exception e) {
		}
	}

}
