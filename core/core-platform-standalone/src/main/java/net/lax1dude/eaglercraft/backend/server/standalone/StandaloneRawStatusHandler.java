/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 */

package net.lax1dude.eaglercraft.backend.server.standalone;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

final class StandaloneRawStatusHandler extends ByteToMessageDecoder {

	private final String fallbackAddress;

	StandaloneRawStatusHandler(String fallbackAddress) {
		this.fallbackAddress = fallbackAddress;
	}

	@Override
	protected void decode(ChannelHandlerContext context, ByteBuf input, java.util.List<Object> output) {
		int end = input.indexOf(input.readerIndex(), input.writerIndex(), (byte) '\n');
		if (end < 0 || input.readableBytes() > 16384) {
			return;
		}
		String headers = input.toString(input.readerIndex(), input.readableBytes(), StandardCharsets.US_ASCII);
		if (!headers.contains("\r\n\r\n") && !headers.contains("\n\n")) {
			return;
		}
		if (!headers.startsWith("GET / HTTP/") || headers.toLowerCase(java.util.Locale.ROOT).contains("upgrade: websocket")) {
			context.pipeline().remove(this);
			output.add(input.readRetainedSlice(input.readableBytes()));
			return;
		}
		String address = fallbackAddress;
		for (String line : headers.split("\\r?\\n")) {
			if (line.regionMatches(true, 0, "Host:", 0, 5)) {
				address = line.substring(5).trim();
				break;
			}
		}
		String page = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Eagler Connected!</title>"
				+ "</head><body><h1>Eagler Connected!</h1><p>Join with " + escapeHtml(address)
				+ "</p></body></html>";
		byte[] body = page.getBytes(StandardCharsets.UTF_8);
		String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
				+ body.length + "\r\nConnection: close\r\n\r\n";
		ByteBuf outputBuffer = context.alloc().buffer(response.length() + body.length);
		outputBuffer.writeCharSequence(response, StandardCharsets.US_ASCII);
		outputBuffer.writeBytes(body);
		context.writeAndFlush(outputBuffer).addListener(ChannelFutureListener.CLOSE);
		input.skipBytes(input.readableBytes());
	}

	private static String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}

}