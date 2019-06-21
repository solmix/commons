package org.solmix.service.versioncontrol.support;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamWrapper extends InputStream {

	private InputStream wrappedStream;
	
	private boolean isZip;

	public InputStreamWrapper(InputStream is, boolean isZip){
		this.isZip = isZip;
		this.wrappedStream = is;
	}
	
	public InputStream getWrappedStream() {
		return wrappedStream;
	}

	public void setWrappedStream(InputStream wrappedStream) {
		this.wrappedStream = wrappedStream;
	}


	@Override
	public int hashCode() {
		return wrappedStream.hashCode();
	}

	@Override
	public int read() throws IOException {
		return wrappedStream.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return wrappedStream.read(b);
	}


	@Override
	public boolean equals(Object obj) {
		return wrappedStream.equals(obj);
	}


	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return wrappedStream.read(b, off, len);
	}


	@Override
	public long skip(long n) throws IOException {
		return wrappedStream.skip(n);
	}


	@Override
	public String toString() {
		return wrappedStream.toString();
	}


	@Override
	public int available() throws IOException {
		return wrappedStream.available();
	}


	@Override
	public void close() throws IOException {
		if (isZip)
			return;
		wrappedStream.close();
	}


	@Override
	public void mark(int readlimit) {
		wrappedStream.mark(readlimit);
	}


	@Override
	public void reset() throws IOException {
		wrappedStream.reset();
	}


	@Override
	public boolean markSupported() {
		return wrappedStream.markSupported();
	}

}

