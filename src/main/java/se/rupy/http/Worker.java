package se.rupy.http;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Worker gets the job done. The worker holds the in/out/chunk buffers in order to
 * save resources, since the worker is assigned per event until a query is read and 
 * a reply is written.
 * 
 * @author marc
 */
public class Worker implements Runnable, Chain.Link {
	private Daemon daemon;
	private ByteBuffer in, out;
	private byte[] chunk;
	private Thread thread;
	private Event event;
	private int index, lock;
	private boolean awake, alive;
	private long touch;
	private DateFormat date;

	protected Worker(Daemon daemon, int index) {
		this.daemon = daemon;
		this.index = index;

		in = ByteBuffer.allocateDirect(daemon.size);
		out = ByteBuffer.allocateDirect(daemon.size);

		date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		date.setTimeZone(TimeZone.getTimeZone("GMT"));

		alive = true;

		thread = new Thread(this, "RupyWorker-"+index);
		thread.start();
	}

	protected DateFormat date() {
		return date;
	}

	protected ByteBuffer in() {
		touch();
		return in;
	}

	protected ByteBuffer out() {
		touch();
		return out;
	}

	protected byte[] chunk() {
		if(chunk == null) {
			chunk = new byte[daemon.size + Output.Chunked.OFFSET + 2];
		}

		return chunk;
	}

	protected void wakeup() {
		if (Event.LOG) {
			if(event != null && event.daemon().debug)
				event.log("wakeup", Event.DEBUG);
		}

		touch();

		synchronized (thread) {
			thread.notify();
		}

		awake = true;
	}

	protected void touch() {
		/*
		if(event.daemon().debug) {
			event.log("touch " + (System.currentTimeMillis() - touch), Event.DEBUG);
		}
		 */
		touch = System.currentTimeMillis();
	}

	protected void snooze() {
		snooze(0);
	}

	protected void snooze(long delay) {
		if (Event.LOG) {
			if(event != null && event.daemon().debug)
				event.log("snooze " + delay, Event.DEBUG);
		}

		synchronized (thread) {
			try {
				if (delay > 0) {
					if(awake) {
						awake = false;
						return;
					}

					thread.wait(delay);
				} else {
					thread.wait();
				}
			} catch (InterruptedException e) {
				event.disconnect(e);
			}

			awake = false;
		}
	}

	protected Event event() {
		return event;
	}

	protected void event(Event event) {
		this.event = event;
	}

	protected int lock() {
		return lock;
	}

	protected boolean busy() {
		if(event != null && touch > 0) {
			lock = (int) (System.currentTimeMillis() - touch);

			if(lock > daemon.delay) {
				reset(new Exception("Threadlock " + lock + " (" + event.query().path() + ")"));
				event = null;
				return false;
			}

			return event != null;
		}

		return false;
	}

	public int index() {
		return index;
	}

	protected void stop() {
		synchronized (thread) {
			thread.notify();
		}

		alive = false;
	}

	public String toString() {
		return String.valueOf(index);
	}

	public void run() {
		touch = System.currentTimeMillis();

		while (alive) {
			try {
				if (event != null) {
					if (event.push()) {
						event.write();
						//event.push(false);
					} else {
						event.read();
					}
				}
			} catch (Exception e) {
				reset(e);
			} finally {
				if (event != null) {
					event.worker(null);
					event = daemon.next(this);

					if (event != null) {
						event.worker(this);
					} else {
						snooze();
					}
				} else {
					snooze();
				}
			}
		}
	}

	protected void reset(Exception e) {
		if(event != null) {
			event.disconnect(e);
		}

		out.clear();
		in.clear();
	}
}
