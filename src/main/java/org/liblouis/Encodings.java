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
	UTF_32LE(Charset.forName("UTF-32LE")),
	// Liblouis' custom encoding for dot patterns:
	// - bit 16 always set
	// - bit 15 = (virtual) dot F ... bit 9 = (virtual) dot 9
	// - bit 8 = dot 8 ... bit 1 = dot 1
	DOTSIO_16(new DotsIOCharset(16)),
	// - bits 32 to 17 are ignored
	DOTSIO_32(new DotsIOCharset(32));

	private final Charset charset;

	private Encodings(Charset charset) {
		this.charset = charset;
	}

	public Charset asCharset() {
		return charset;
	}

	private static class DotsIOCharset extends Charset {

		final int charsize;

		protected DotsIOCharset(int charsize) {
			super("LOU-DOTSIO-" + charsize, null);
			this.charsize = charsize / 8;
		}

		@Override
		public boolean contains(Charset cs) {
			return equals(cs);
		}

		@Override
		public CharsetDecoder newDecoder() {
			return new DotsIODecoder();
		}

		@Override
		public CharsetEncoder newEncoder() {
			return new DotsIOEncoder();
		}

		class DotsIOEncoder extends CharsetEncoder {
			DotsIOEncoder() {
				super(DotsIOCharset.this, charsize, charsize,
				      charsize == 2
				          ? new byte[]{(byte)0x00,(byte)0x80}
				          : new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x80});
			}
			@Override
			protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
				int k = 0;
				while (in.hasRemaining()) {
					char c = in.get(k++);
					if (c < '\u2800' || c > '\u28ff')
						return k == 1 ? CoderResult.unmappableForLength(1) : CoderResult.UNDERFLOW;
					if (out.remaining() < charsize)
						return CoderResult.OVERFLOW;
					for (int i = 2; i < charsize; i++)
						out.put((byte)0);
					out.put((byte)(c & '\u00ff'));
					out.put((byte)0x80);
					in.get();
				}
				return CoderResult.UNDERFLOW;
			}
		}

		class DotsIODecoder extends CharsetDecoder {
			DotsIODecoder() {
				super(DotsIOCharset.this, 1.f / charsize, 1);
			}
			@Override
			protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
				int k = 0;
				while (in.remaining() >= charsize) {
					k++;
					byte b = in.get(k * charsize - 1);
					if ((b & 0x80) == 0)
						return k == 1 ? CoderResult.malformedForLength(charsize) : CoderResult.UNDERFLOW;
					if ((b & 0xff) != 0x80)
						// virtual dots present
						return k == 1 ? CoderResult.malformedForLength(charsize) : CoderResult.UNDERFLOW;
					if (!out.hasRemaining())
						return CoderResult.OVERFLOW;
					b = in.get(k * charsize - 2);
					out.put((char)(b | '\u2800'));
					// ignoring byte 3 and 4 if present
					for (int i = 0; i < charsize; i++)
						in.get();
				}
				return CoderResult.UNDERFLOW;
			};
		}
	}
}
