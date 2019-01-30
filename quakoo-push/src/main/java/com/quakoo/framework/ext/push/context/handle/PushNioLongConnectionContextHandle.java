package com.quakoo.framework.ext.push.context.handle;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.param.StringDecoder;
import com.quakoo.framework.ext.push.nio.PushConnectHandler;

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
 * 推送长连接启动上下文
 * class_name: PushNioLongConnectionContextHandle
 * package: com.quakoo.framework.ext.push.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:18
 **/
public class PushNioLongConnectionContextHandle extends PushNioHandleContextHandle {
	
	@Resource
	private PushConnectHandler pushConnectHandler;

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
		                p.addLast(pushConnectHandler);
		            }
		        });
				ChannelFuture future = bootstrap.bind(pushInfo.pushNioConnectIp,
						Integer.parseInt(pushInfo.pushNioConnectPort)).sync();
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
