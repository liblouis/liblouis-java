package org.liblouis;

import com.sun.jna.Memory;
import com.sun.jna.NativeMapped;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class WideString extends PointerType implements NativeMapped {
	
	private final int length;
	
	public WideString() {
		this(0);
	}
	
	public WideString(int length) {
		this.length = length;
	}
	
	public WideString(String value) {
		this(value.length());
		write(value);
	}
	
	public WideString(Pointer p, int offset, int length) {
		this(length);
		setPointer(p.share(offset * WideChar.Constants.CHARSIZE));
	}
	
	public String read(int length) {
		if (length > length())
			throw new IllegalArgumentException("Maximum length is " + length());
		try {
			return new String(getPointer().getByteArray(0, length * WideChar.Constants.CHARSIZE), WideChar.Constants.ENCODING); }
		catch (Exception e) {
			throw new RuntimeException(e); }
	}
	
	public WideString write(String value) {
		if (value.length() > length)
			throw new IllegalArgumentException("Maximum string length is " + length());
		try {
			getPointer().write(0, value.getBytes(WideChar.Constants.ENCODING), 0, value.length() * WideChar.Constants.CHARSIZE); }
		catch (Exception e) {
			throw new RuntimeException(e); }
		return this;
	}
	
	@Override
	public Pointer getPointer() {
		if (super.getPointer() == null) {
			try {
				setPointer(new Memory(length * WideChar.Constants.CHARSIZE)); }
			catch (Exception e) {
				throw new RuntimeException(e); }}
		return super.getPointer();
	}
	
	public int length() {
		return length;
	}
	
	public WideString substring(int beginIndex) {
		return substring(beginIndex, length);
	}
	
	public WideString substring(int beginIndex, int endIndex) {
		if (beginIndex < 0 || endIndex > length || beginIndex > endIndex)
			throw new IndexOutOfBoundsException();
		return new WideString(getPointer(), beginIndex, endIndex - beginIndex);
	}
	
	@Override
	public String toString() {
		return read(length());
	}
}
