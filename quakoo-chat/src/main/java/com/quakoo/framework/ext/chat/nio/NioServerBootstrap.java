package com.quakoo.framework.ext.chat.nio;


import com.quakoo.framework.ext.chat.model.param.nio.StringDecoder;
import com.quakoo.framework.ext.chat.model.param.nio.StringEncoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NioServerBootstrap {

	public static void main(String[] args) throws InterruptedException {
		EventLoopGroup boss=new NioEventLoopGroup();
        EventLoopGroup worker=new NioEventLoopGroup();
        ServerBootstrap bootstrap=new ServerBootstrap();
        bootstrap.group(boss,worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline p = socketChannel.pipeline();
                p.addLast(new StringEncoder());
                p.addLast(new StringDecoder());
                p.addLast(new ConnectHandler());
            }
        });
        ChannelFuture f = bootstrap.bind(11111).sync();
        if(f.isSuccess()){
            System.out.println("server start---------------");
        }
        f.channel().closeFuture().sync();
	}
	
}
