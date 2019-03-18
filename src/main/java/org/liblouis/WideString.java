package org.liblouis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import com.sun.jna.Memory;
import com.sun.jna.NativeMapped;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public final class WideString extends PointerType implements NativeMapped {
	
	private static CharsetEncoder ENCODER;
	private static CharsetDecoder DECODER;
	
	private final int length;
	
	public WideString() {
		this(0);
	}
	
	WideString(int length) {
		this.length = length;
		// can not be put in a static block
		if (ENCODER == null) {
			Charset charset;
			switch (WideChar.SIZE) {
			case 2:
				charset = Encodings.UTF_16LE.asCharset();
				break;
			case 4:
				charset = Encodings.UTF_32LE.asCharset();
				break;
			default:
				throw new RuntimeException();
			}
			ENCODER = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
			                              .onUnmappableCharacter(CodingErrorAction.REPORT);
			DECODER = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
			                              .onUnmappableCharacter(CodingErrorAction.REPORT);
		}
	}
	
	WideString(String value) {
		this(value.length());
		try {
			write(value); }
		catch (IOException e) {
			throw new RuntimeException("should not happen", e); }
	}
	
	WideString(Pointer p, int offset, int length) {
		this(length);
		setPointer(p.share(offset * WideChar.SIZE));
	}
	
	/**
	 * Read as UTF-32 or UTF-16 string
	 *
	 * @param length The number of characters to read
	 * @return The Java string
	 * @throws CharacterCodingException if the string can not be decoded
	 * @throws IOException if length exceeds the maximum number of characters in this string
	 */
	String read(int length) throws CharacterCodingException, IOException {
		synchronized (DECODER) {
			if (length > length())
				throw new IOException("Maximum length is " + length());
			// using CharsetDecoder because behavior of String(byte[]) is undefined when bytes can not be decoded
			char[] ca = new char[length];
			if (length > 0) {
				byte[] ba = getPointer().getByteArray(0, length * WideChar.SIZE);
				ByteBuffer bb = ByteBuffer.wrap(ba);
				CharBuffer cb = CharBuffer.wrap(ca);
				DECODER.reset();
				CoderResult cr = DECODER.decode(bb, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = DECODER.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();
				if (cb.hasRemaining())
					throw new RuntimeException("should not happen");
			}
			return new String(ca);
		}
	}
	
	/**
	 * Write as UTF-32 or UTF-16 string
	 *
	 * @param value The Java string to write
	 * @return This object
	 * @throws CharacterCodingException if the string can not be encoded
	 * @throws IOException if the supplied value is longer than the available space
	 */
	WideString write(String value) throws CharacterCodingException, IOException {
		synchronized (ENCODER) {
			if (value.length() > length)
				throw new IOException("Maximum string length is " + length());
			// using CharsetEncoder because behavior of String.getBytes() is undefined when characters can not be encoded
			byte[] ba = new byte[value.length() * WideChar.SIZE];
			if (ba.length > 0) {
				ByteBuffer bb = ByteBuffer.wrap(ba);
				CharBuffer cb = CharBuffer.wrap(value);
				ENCODER.reset();
				CoderResult cr = ENCODER.encode(cb, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ENCODER.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();
				if (bb.hasRemaining())
					throw new RuntimeException("should not happen");
				getPointer().write(0, ba, 0, ba.length);
			}
			return this;
		}
	}
	
	@Override
	public Pointer getPointer() {
		if (super.getPointer() == null) {
			try {
				setPointer(new Memory(length * WideChar.SIZE)); }
			catch (Exception e) {
				throw new RuntimeException(e); }}
		return super.getPointer();
	}
	
	int length() {
		return length;
	}
	
	WideString substring(int beginIndex) {
		return substring(beginIndex, length);
	}
	
	WideString substring(int beginIndex, int endIndex) {
		if (beginIndex < 0 || endIndex > length || beginIndex > endIndex)
			throw new IndexOutOfBoundsException();
		return new WideString(getPointer(), beginIndex, endIndex - beginIndex);
	}
	
	@Override
	public String toString() {
		try {
			return read(length()); }
		catch (IOException e) {
			throw new RuntimeException("should not happen", e); }
	}
}
