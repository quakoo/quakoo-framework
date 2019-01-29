package com.quakoo.framework.ext.chat.context.handle.nio;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.model.param.nio.StringDecoder;
import com.quakoo.framework.ext.chat.nio.ConnectHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * socket容器启动上下文
 *
 * class_name: NioLongConnectionContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle.nio
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:37
 **/
public class NioLongConnectionContextHandle extends NioHandleContextHandle {
	
	@Resource
	private ConnectHandler connectHandler;

	@Override
	protected void startConnectBootstrap() {
		Thread connectBootstrap = new Thread(new ConnectBootstrap());
		connectBootstrap.start();
	}

	class ConnectBootstrap implements Runnable {
		@Override
		public void run() {
			EventLoopGroup boss = new NioEventLoopGroup(1);
			EventLoopGroup worker = new NioEventLoopGroup();
			try {
				ServerBootstrap bootstrap = new ServerBootstrap();
				bootstrap.group(boss, worker);
				bootstrap.channel(NioServerSocketChannel.class);
				bootstrap.option(ChannelOption.SO_BACKLOG, 128).
				handler(new LoggingHandler(LogLevel.INFO));
				bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
				bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
		            @Override
		            protected void initChannel(SocketChannel socketChannel) throws Exception {
		                ChannelPipeline p = socketChannel.pipeline();
		                p.addLast(new StringDecoder());
		                p.addLast(connectHandler);
		            }
		        });
				ChannelFuture future = bootstrap.bind(chatInfo.nioConnectBootstrapIp, 
						Integer.parseInt(chatInfo.nioConnectBootstrapPort)).sync();
				if(future.isSuccess()){
		            System.out.println("server start---------------");
		        }
				future.channel().closeFuture().sync();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				boss.shutdownGracefully();
				worker.shutdownGracefully();
			}
		}
	}
}
