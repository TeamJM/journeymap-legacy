package se.rupy.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Asynchronous HTTP request/response, this virtually represents a client
 * socket, but in the case where the server is behind a proxy we cannot depend
 * on that fact since sockets will be reused by multiple different external
 * clients. It's a performance tradeoff that we gladly accept though, since
 * hiding behind an Apache or some other proxy is this servers most probable
 * use.
 *
 * @author marc
 */
@SuppressWarnings("ALL")
public class Event extends Throwable implements Chain.Link
{
    // waste of time it seems. hotspot does this optimisation for me! :)
    public final static boolean LOG = true;

    static int READ = 1 << 0;
    static int WRITE = 1 << 2;
    static int VERBOSE = 1 << 0;
    static int DEBUG = 1 << 1;
    static Mime MIME;

    static
    {
        MIME = new Mime();
        READ = SelectionKey.OP_READ;
        WRITE = SelectionKey.OP_WRITE;
    }

    private static char[] BASE_24 = {'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K',
            'M', 'P', 'Q', 'R', 'T', 'V', 'W', 'X', 'Y', '2', '3', '4', '6',
            '7', '8', '9'};
    private SocketChannel channel;
    private SelectionKey key;

    private Query query;
    private Reply reply;
    private Session session;

    private Daemon daemon;
    private Worker worker;

    private int index, interest;
    private String remote;
    private boolean close;
    private long touch;

    protected Event(Daemon daemon, SelectionKey key, int index) throws IOException
    {
        touch();

        channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);

        this.daemon = daemon;
        this.index = index;
        //this.key = key;

        query = new Query(this);
        reply = new Reply(this);

        key = channel.register(key.selector(), READ, this);
        key.selector().wakeup();

        this.key = key;
    }

    protected static String cookie(String cookie, String key)
    {
        String value = null;

        if (cookie != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(cookie, " ");

            while (tokenizer.hasMoreTokens())
            {
                String part = tokenizer.nextToken();
                int equals = part.indexOf("=");

                if (equals > -1 && part.substring(0, equals).equals(key))
                {
                    String subpart = part.substring(equals + 1);

                    int index = subpart.indexOf(";");

                    if (index > 0)
                    {
                        value = subpart.substring(0, index);
                    }
                    else
                    {
                        value = subpart;
                    }
                }
            }
        }

        return value;
    }

    public static String random(int length)
    {
        Random random = new Random();
        StringBuffer buffer = new StringBuffer();

        while (buffer.length() < length)
        {
            buffer.append(BASE_24[Math.abs(random.nextInt() % 24)]);
        }

        return buffer.toString();
    }

    protected int interest()
    {
        return interest;
    }

    protected void interest(int interest)
    {
        this.interest = interest;
    }

    public Daemon daemon()
    {
        return daemon;
    }

    public Query query()
    {
        return query;
    }

    public Reply reply()
    {
        return reply;
    }

    public Session session()
    {
        return session;
    }

    public String remote()
    {
        return remote;
    }

    public boolean close()
    {
        return close;
    }

    public Worker worker()
    {
        return worker;
    }

    public int index()
    {
        return index;
    }

    protected void close(boolean close)
    {
        this.close = close;
    }

    protected void worker(Worker worker)
    {
        this.worker = worker;

        try
        {
            register(READ);
        }
        catch (CancelledKeyException e)
        {
            disconnect(e);
        }
    }

    protected SocketChannel channel()
    {
        return channel;
    }

    protected void log(Object o)
    {
        log(o, Event.DEBUG);
    }

    protected void log(Object o, int level)
    {
        if (o instanceof Exception && daemon.debug)
        {
            daemon.out.print("[" + (worker == null ? "*" : "" + worker.index())
                    + "-" + index + "] ");
            ((Exception) o).printStackTrace(daemon.out);
        }
        else if (daemon.debug || daemon.verbose && level == Event.VERBOSE)
        {
            daemon.out.println("["
                    + (worker == null ? "*" : "" + worker.index()) + "-"
                    + index + "] " + o);
        }
    }

    /**
     * @return same as {@link Query#big(String)}.
     */
    public long big(String key)
    {
        return query.big(key);
    }

    /**
     * @return same as {@link Query#medium(String)}.
     */
    public int medium(String key)
    {
        return query.medium(key);
    }

    /**
     * @return same as {@link Query#small(String)}.
     */
    public short small(String key)
    {
        return query.small(key);
    }

    /**
     * @return same as {@link Query#tiny(String)}.
     */
    public byte tiny(String key)
    {
        return query.tiny(key);
    }

    /**
     * @return same as {@link Query#bit(String, boolean)}.
     */
    public boolean bit(String key)
    {
        return query.bit(key, true);
    }

    /**
     * @return same as {@link Query#string(String)}.
     */
    public String string(String key)
    {
        return query.string(key);
    }

    /**
     * @return same as {@link Query#input()}.
     */
    public Input input()
    {
        return query.input();
    }

    /**
     * @return same as {@link Reply#output()}.
     * @throws IOException
     */
    public Output output() throws IOException
    {
        return reply.output();
    }

    protected void read() throws IOException
    {
        touch();

        if (!query.headers())
        {
            disconnect(null);
        }

        remote = address();

        if (query.version() == null || !query.version().equalsIgnoreCase("HTTP/1.1"))
        {
            reply.code("505 Not Supported");
        }
        else
        {
            if (!service(daemon.chain(query)))
            {
                if (!content())
                {
                    if (!service(daemon.chain("null")))
                    {
                        reply.code("404 Not Found");
                        reply.output().print(
                                "<pre>'" + query.path() + "' was not found.</pre>");
                    }
                }
            }
        }

        finish();
    }

    protected String address()
    {
        String remote = query.header("x-forwarded-for");

        if (remote == null)
        {
            InetSocketAddress address = (InetSocketAddress) channel.socket()
                    .getRemoteSocketAddress();
            remote = address.getAddress().getHostAddress();
        }

        if (Event.LOG)
        {
            log("remote " + remote, VERBOSE);
        }

        return remote;
    }

    protected boolean content() throws IOException
    {
        Deploy.Stream stream = daemon.content(query);

        if (stream == null)
        {
            return false;
        }

        String type = MIME.content(query.path(), "application/octet-stream");

        reply.type(type);
        reply.modified(stream.date());

        if (query.modified() == 0 || query.modified() < reply.modified())
        {
            Deploy.pipe(stream.input(), reply.output(stream.length()));

            if (Event.LOG)
            {
                log("content " + type, VERBOSE);
            }
        }
        else
        {
            reply.code("304 Not Modified");
        }

        return true;
    }

    protected boolean service(Chain chain) throws IOException
    {
        if (chain == null)
        {
            return false;
        }

        try
        {
            chain.filter(this);
        }
        catch (Failure f)
        {
            throw f;
        }
        catch (Event e)
        {
            // Break the filter chain.
        }
        catch (Exception e)
        {
            if (Event.LOG)
            {
                log(e);
            }

            daemon.error(this, e);

            StringWriter trace = new StringWriter();
            PrintWriter print = new PrintWriter(trace);
            e.printStackTrace(print);

            reply.code("500 Internal Server Error");
            reply.output().print("<pre>" + trace.toString() + "</pre>");
        }

        return true;
    }

    protected void write() throws IOException
    {
        touch();
        service(daemon.chain(query));
        finish();
    }

    private void finish() throws IOException
    {
        String log = daemon.access(this);

        reply.done();
        query.done();

        if (log != null)
        {
            daemon.access(log, reply.push());
        }
    }

    protected void register() throws IOException
    {
        if (interest != key.interestOps())
        {
            if (Event.LOG)
            {
                log((interest == READ ? "read" : "write") + " prereg " + interest
                        + " " + key.interestOps() + " " + key.readyOps(), DEBUG);
            }
            key = channel.register(key.selector(), interest, this);
            if (Event.LOG)
            {
                log((interest == READ ? "read" : "write") + " postreg " + interest
                        + " " + key.interestOps() + " " + key.readyOps(), DEBUG);
            }
        }

        key.selector().wakeup();

        if (Event.LOG)
        {
            log((interest == READ ? "read" : "write") + " wakeup", DEBUG);
        }
    }

    protected void register(int interest)
    {
        interest(interest);

        try
        {
            if (channel.isOpen())
            {
                register();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    protected int block(Block block) throws Exception
    {
        long max = System.currentTimeMillis() + daemon.delay;

        while (System.currentTimeMillis() < max)
        {
            register();
            int available = block.fill(true);

            if (available > 0)
            {
                long delay = daemon.delay - (max - System.currentTimeMillis());

                if (Event.LOG)
                {
                    log("delay " + delay + " " + available, VERBOSE);
                }

                return available;
            }

            Thread.yield();
            worker.snooze(10);
            key.selector().wakeup();
        }

        throw new Exception("IO timeout. (" + daemon.delay + ")");
    }

    protected void disconnect(Exception e)
    {
        try
        {
            if (channel != null)
            { // && channel.isOpen()) {
                channel.close();
            }

            if (key != null)
            {
                key.cancel();
            }

            if (session != null)
            {
                session.remove(this);
            }

            if (daemon.debug)
            {
                if (Event.LOG)
                {
                    log("disconnect " + e);
                }

                if (e != null)
                {
                    e.printStackTrace();
                }
            }

            daemon.error(this, e);
        }
        catch (Exception de)
        {
            de.printStackTrace(daemon.out);
        }
        finally
        {
            daemon.events.remove(new Integer(index));
        }
    }

    protected final void session(final Service service) throws Exception
    {
        String key = cookie(query.header("cookie"), "key");

        if (key == null && query.method() == Query.GET)
        {
            /*
			 * XSS comet cookie: this means first GETs are parsed!
			 * TODO: This should be removed because you can use a P3P header to fix this, go figure!
			 */
            query.parse();
            String cookie = query.string("cookie");
            key = cookie.length() > 0 ? cookie : null;
        }

        if (key != null)
        {
            session = (Session) daemon.session().get(key);

            if (session != null)
            {
                if (Event.LOG)
                {
                    log("old key " + key, VERBOSE);
                }

                session.add(this);
                session.touch();

                return;
            }
        }

        int index = 0;

        if (daemon.host)
        {
            Integer i = (Integer) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    return new Integer(service.index());
                }
            }, daemon.control);

            index = i.intValue();
        }
        else
        {
            index = service.index();
        }

        if (index == 0 && !push())
        {
            session = new Session(daemon);
            session.add(service);
            session.add(this);
            session.key(key);

            if (session.key() == null)
            {
                do
                {
                    key = random(daemon.cookie);
                } while (daemon.session().get(key) != null);
                session.key(key);
            }

            synchronized (daemon.session())
            {
                if (Event.LOG)
                {
                    log("new key " + session.key(), VERBOSE);
                }

                daemon.session().put(session.key(), session);
            }
        }

        try
        {
            service.session(session, Service.CREATE);
        }
        catch (Exception e)
        {
            e.printStackTrace(daemon.out);
        }
    }

    public String toString()
    {
        return String.valueOf(index);
    }

    /**
     * @return true if the Event is being recycled due to a call to
     * {@link Reply#wakeup()}.
     */
    public boolean push()
    {
        return reply.output.push();
    }

    /**
     * Touch the worker if you have a http connection that needs to wait.
     */
    public void touch()
    {
        touch = System.currentTimeMillis();

        if (worker != null)
        {
            worker.touch();
        }
    }

    protected long last()
    {
        return touch;
    }

    /**
     * Keeps the chunked reply open for asynchronous writes. If you are
     * streaming data and you need to send something upon the first request
     * you have to call this in order to avoid that the trailing zero length
     * chunk is sent to complete the response.
     *
     * @throws IOException
     */
    public void hold() throws IOException
    {
        reply.output.push = true;
    }

    interface Block
    {
        public int fill(boolean debug) throws IOException;
    }

    static class Mime extends Properties
    {
        public Mime()
        {
            try
            {
                load(Mime.class.getResourceAsStream("mime.txt")); // techbrew : Moved file to package
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        String content(String path, String fail)
        {
            int index = path.lastIndexOf('.') + 1;

            if (index > 0)
            {
                return getProperty(path.substring(index), fail);
            }

            return fail;
        }
    }
}
