package se.rupy.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Handles the incoming request data.
 * @author marc.larue
 */
public abstract class Input extends InputStream implements Event.Block {
	private boolean chunk, init;
	private byte[] one = new byte[1];
	private int available, length;
	private Event event;

	protected Input(Event event) throws IOException {
		this.event = event;
	}

	protected void init() {
		chunk = event.query().length() > -1 ? false : true;

		if (Event.LOG) {
			if(event.daemon().verbose) {
				event.log("header " + length, Event.VERBOSE);
			}
		}

		length = 0;
		init = true;
	}

	protected void end() {
		if (Event.LOG) {
			if (event.daemon().verbose && length > 0)
				event.log("query " + length, Event.VERBOSE);
		}

		available = 0;
		init = false;
	}

	protected Event event() {
		return event;
	}

	protected boolean chunk() {
		return chunk;
	}

	protected int real() throws IOException {
		if (real(one, 0, 1) > 0) {
			return one[0] & 0xFF;
		}
		return -1;
	}

	protected int real(byte[] b) throws IOException {
		return real(b, 0, b.length);
	}

	protected int real(byte[] b, int off, int len) throws IOException {
		try {
			available = fill(false);

			if (available == 0) {
				if (init && !chunk && length >= event.query().length()) {
					return -1; // fixed length EOF
				}

				available = event.block(this);
			}

			int read = available > len ? len : available;
			event.worker().in().get(b, off, read);
			available -= read;
			length += read;
			return read;
		} catch (Failure.Close c) {
			throw c;
		} catch (IOException e) {
			Failure.chain(e);
		} catch (Exception e) {
			throw (IOException) new IOException().initCause(e);
		}

		return 0; // will never happen, you silly compiler ...
	}

	public int available() {
		return available;
	}

	public boolean markSupported() {
		return false;
	}

	public int fill(boolean debug) throws IOException {
		if (available > 0)
			return available;

		ByteBuffer buffer = event.worker().in();
		buffer.clear();

		try {
			available = event.channel().read(buffer);
		}
		catch(IOException e) {
			throw (Failure.Close) new Failure.Close().initCause(e); // Connection reset by peer
		}

		if (available > 0) {
			buffer.flip();
		} else if (available < 0) {
			throw new Failure.Close("Available: " + available); // Connection dropped by peer
		}

		return available;
	}

	/**
	 * Reads a \r\n terminated line of text from the input.
	 * @return
	 * @throws IOException
	 */
	public String line() throws IOException {
		StringBuffer buffer = new StringBuffer("");

		while (true) {
			if (buffer.length() > 2048) { // Facebook... :P
				throw new IOException("Line too long.");
			}

			int a = real();

			if (a == '\r') {
				int b = real();

				if (b == '\n') {
					return buffer.toString();
				} else if (b > -1) {
					buffer.append((char) a);
					buffer.append((char) b);
				}
			} else if (a > -1) {
				buffer.append((char) a);
			}
		}
	}

	static class Chunked extends Input {
		private byte[] one = new byte[1];
		private int length;

		protected Chunked(Event event) throws IOException {
			super(event);
		}

		public int read() throws IOException {
			if (read(one, 0, 1) > 0) {
				return one[0] & 0xFF;
			}
			return -1;
		}

		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		public int read(byte[] b, int off, int len) throws IOException {
			if (!chunk()) {
				return real(b, off, len);
			}

			if (length == 0) {
				boolean done = false;
				int c = real();

				while (c != '\n') {
					int val = 0;

					if (c == ';' || c == '\r') {
						done = true;
					} else if (!done) {
						if (c >= '0' && c <= '9') {
							val = c - '0';
						} else if (c >= 'a' && c <= 'f') {
							val = c - 'a' + 10;
						} else if (c >= 'A' && c <= 'F') {
							val = c - 'A' + 10;
						} else {
							throw new IOException("Chunked input.");
						}

						length = length * 16 + val;
					}

					c = real();
				}

				if (length == 0) {
					return -1; // chunked EOF
				}
			}

			if (len > length) {
				len = length;
			}

			int read = real(b, off, len);

			if (read == length) {
				real();
				real();
			}

			if (read > 0) {
				length -= read;
			}

			return read;
		}
	}
}
