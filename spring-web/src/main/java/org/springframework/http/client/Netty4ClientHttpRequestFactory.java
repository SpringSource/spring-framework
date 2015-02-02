/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="http://netty.io/">Netty 4</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link EventLoopGroup} instance - useful for sharing
 * across multiple clients.
 *
 * @author Arjen Poutsma
 * @since 4.1.2
 */
public class Netty4ClientHttpRequestFactory implements ClientHttpRequestFactory,
		AsyncClientHttpRequestFactory, InitializingBean, DisposableBean {

	/**
	 * The default maximum request size.
	 * @see #setMaxRequestSize(int)
	 */
	public static final int DEFAULT_MAX_REQUEST_SIZE = 1024 * 1024 * 10;

	/**
	 * The default maximum header size
	 * @see #setMaxHeaderSize(int)
	 * @see HttpClientCodec#HttpClientCodec()
	 */
	public static final int DEFAULT_MAX_HEADER_SIZE = 8192;
	
	/**
	 * The default initial line length.
	 * @see #setInitialLineLength(int)
	 * @see HttpClientCodec#HttpClientCodec()
	 */
	public static final int DEFAULT_INITIAL_LINE_LENGTH = 4096;

	/**
	 * The default max chunk size.
	 * @see #setMaxChunkSize(int)
	 * @see HttpClientCodec#HttpClientCodec()
	 */
	public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;
	
	/**
	 * The default byte-buf allocator.
	 * @see UnpooledByteBufAllocator#DEFAULT
	 */
	public static final ByteBufAllocator DEFAULT_BYTE_BUF_ALLOCATOR = UnpooledByteBufAllocator.DEFAULT;

	private final EventLoopGroup eventLoopGroup;

	private final boolean defaultEventLoopGroup;

	private int maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
	
	private int maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;
	
	private int initialLineLength = DEFAULT_INITIAL_LINE_LENGTH;
	
	private int maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;

	private SslContext sslContext;
	
	private ByteBufAllocator byteBufAllocator = DEFAULT_BYTE_BUF_ALLOCATOR;

	private volatile Bootstrap bootstrap;


	/**
	 * Create a new {@code Netty4ClientHttpRequestFactory} with a default
	 * {@link NioEventLoopGroup}.
	 */
	public Netty4ClientHttpRequestFactory() {
		int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
		this.eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
		this.defaultEventLoopGroup = true;
	}

	/**
	 * Create a new {@code Netty4ClientHttpRequestFactory} with the given
	 * {@link EventLoopGroup}.
	 * <p><b>NOTE:</b> the given group will <strong>not</strong> be
	 * {@linkplain EventLoopGroup#shutdownGracefully() shutdown} by this factory;
	 * doing so becomes the responsibility of the caller.
	 */
	public Netty4ClientHttpRequestFactory(EventLoopGroup eventLoopGroup) {
		Assert.notNull(eventLoopGroup, "EventLoopGroup must not be null");
		this.eventLoopGroup = eventLoopGroup;
		this.defaultEventLoopGroup = false;
	}


	/**
	 * Set the maximum request size.
	 * <p>By default this is set to {@link #DEFAULT_MAX_REQUEST_SIZE}.
	 * @see HttpObjectAggregator#HttpObjectAggregator(int)
	 */
	public void setMaxRequestSize(int maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}
	
	/**
	 * Set the maximum header size.
	 * <p>By default this is set to {@link #DEFAULT_MAX_HEADER_SIZE}.
	 * @see HttpClientCodec#HttpClientCodec(int, int, int)
	 */
	public void setMaxHeaderSize(int maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}
	
	/**
	 * Set the initial line length.
	 * <p>By default this is set to {@link #DEFAULT_INITIAL_LINE_LENGTH}.
	 * @see HttpClientCodec#HttpClientCodec(int, int, int)
	 */
	public void setInitialLineLength(int initialLineLength) {
		this.initialLineLength = initialLineLength;
	}
	
	/**
	 * Set the maximum chunk size.
	 * <p>By default this is set to {@link #DEFAULT_MAX_CHUNK_SIZE}.
	 * @see HttpClientCodec#HttpClientCodec(int, int, int)
	 */
	public void setMaxChunkSize(int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}
	
	/**
	 * Set the ByteBuf allocator used by the requests.
	 * @see ByteBufAllocator
	 */
	public void setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
		if(byteBufAllocator==null) {
			this.byteBufAllocator = DEFAULT_BYTE_BUF_ALLOCATOR;
		}
		else {
			this.byteBufAllocator = byteBufAllocator;
		}
	}

	/**
	 * Set the SSL context. When configured it is used to create and insert an
	 * {@link io.netty.handler.ssl.SslHandler} in the channel pipeline.
	 * <p>By default this is not set.
	 */
	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	private Bootstrap getBootstrap() {
		if (this.bootstrap == null) {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.option(ChannelOption.ALLOCATOR, byteBufAllocator);
			bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel channel) throws Exception {
							ChannelPipeline pipeline = channel.pipeline();
							if (sslContext != null) {
								pipeline.addLast(sslContext.newHandler(channel.alloc()));
							}
							pipeline.addLast(new HttpClientCodec(initialLineLength, maxHeaderSize, maxChunkSize));
							pipeline.addLast(new HttpObjectAggregator(maxRequestSize));
						}
					});
			this.bootstrap = bootstrap;
		}
		return this.bootstrap;
	}

	@Override
	public void afterPropertiesSet() {
		getBootstrap();
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	private Netty4ClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new Netty4ClientHttpRequest(getBootstrap(), byteBufAllocator, uri, httpMethod, this.maxRequestSize);
	}


	@Override
	public void destroy() throws InterruptedException {
		if (this.defaultEventLoopGroup) {
			// Clean up the EventLoopGroup if we created it in the constructor
			this.eventLoopGroup.shutdownGracefully().sync();
		}
	}

}
