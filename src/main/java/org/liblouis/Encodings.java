package org.liblouis;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Encodings used in Liblouis API
 */
enum Encodings {
	UTF_16LE(Charset.forName("UTF-16LE")),
	UTF_32LE(Charset.forName("UTF-32LE"));

	private final Charset charset;

	private Encodings(Charset charset) {
		this.charset = charset;
	}

	public Charset asCharset() {
		return charset;
	}
}
