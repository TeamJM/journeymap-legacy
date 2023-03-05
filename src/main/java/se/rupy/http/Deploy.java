package se.rupy.http;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Hot-deploys an application containing one or many service filters from disk
 * with simplistic dynamic class loading, eventually after receiving it through
 * a HTTP POST.
 * <pre>
 * &lt;target name="deploy"&gt;
 * &lt;java fork="yes" 
 *     classname="se.rupy.http.Deploy" 
 *     classpath="http.jar"&gt;
 *      &lt;arg line="localhost:8000"/&gt;&lt;!-- any host:port --&gt;
 *      &lt;arg line="service.jar"/&gt;&lt;!-- your application jar --&gt;
 *      &lt;arg line="secret"/&gt;&lt;!-- see run.bat and run.sh --&gt;
 * &lt;/java&gt;
 * &lt;/target&gt;
 * </pre>
 * @author marc
 */
public class Deploy extends Service {
	protected static String path, pass;

	public Deploy(String path, String pass) {
		Deploy.path = path;
		Deploy.pass = pass;

		new File(path).mkdirs();
	}

	public Deploy(String path) {
		Deploy.path = path;

		new File(path).mkdirs();
	}

	public String path() {
		return "/deploy";
	}

	public void filter(Event event) throws Event, Exception {
		String name = event.query().header("file");
		String size = event.query().header("size");
		String pass = event.query().header("pass");

		if (name == null) {
			throw new Failure("File header missing.");
		}

		if (pass == null) {
			throw new Failure("Pass header missing.");
		}

		if (Deploy.pass == null) {
			if(size != null && size.length() > 0 && Integer.parseInt(size) > 2097152) {
				throw new Exception("Maximum deployable size is 2MB.");
			}
			
			Properties properties = new Properties();
			properties.load(new FileInputStream(new File("passport")));
			String port = properties.getProperty(name.substring(0, name.lastIndexOf('.')));
			
			if (port == null || !port.equals(pass)) {
				throw new Exception("Pass verification failed. (" + name + ")");
			}
		}
		else {
			if (!Deploy.pass.equals(pass)) {
				throw new Failure("Pass verification failed. (" + pass + ")");
			} else if(Deploy.pass.equals("secret") && !event.remote().equals("127.0.0.1")) {
				throw new Failure("'secret' pass can only deploy from 127.0.0.1. (" + event.remote() + ")");
			}
		}

		File file = new File(path + name);
		OutputStream out = new FileOutputStream(file);
		InputStream in = event.query().input();

		if (Deploy.pass == null) {
			try {
				pipe(in, out, 1024, 2097152); // 2MB limit
			}
			catch(IOException e) {
				file.delete();
				throw e;
			}
		}
		else {
			pipe(in, out, 1024);
		}

		out.flush();
		out.close();

		event.reply().output().print("Application '" + deploy(event.daemon(), file) + "' deployed.");
	}

	public static String deploy(Daemon daemon, File file) throws Exception {
		Archive archive = new Archive(daemon, file);

		daemon.chain(archive);
		daemon.verify(archive);

		return archive.name();
	}

	/**
	 * This is our dynamic classloader. Very simple, basically just extracts a jar and writes all files to disk except .class files which are loaded into the JVM. All {@link Service} classes are instantiated.
	 * @author Marc
	 */
	public static class Archive extends ClassLoader {
		private AccessControlContext access;
		private HashSet service;
		private HashMap chain;
		private String name;
		private String host;
		private long date;

		Vector classes = new Vector();

		Archive() { // Archive for deployment.
			PermissionCollection permissions = new Permissions();
			permissions.add(new SocketPermission("*", "listen,accept,resolve,connect"));
			permissions.add(new FilePermission("-", "read"));
			permissions.add(new FilePermission("-", "write"));
			permissions.add(new FilePermission("-", "delete"));
			permissions.add(new RuntimePermission("createClassLoader"));
			access = new AccessControlContext(new ProtectionDomain[] {
					new ProtectionDomain(null, permissions)});
		}
		
		Archive(Daemon daemon, File file) throws Exception {
			service = new HashSet();
			chain = new HashMap();
			name = file.getName();
			date = file.lastModified();

			JarInputStream in = new JarInput(new FileInputStream(file));
			/* Moved this to name convention of the deploy jar
			try {
				Attributes attr = in.getManifest().getMainAttributes();
				host = (String) attr.get("host");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			 */
			if(daemon.host) {
				host = name.substring(0, name.lastIndexOf('.'));
				String path = "app" + File.separator + host + File.separator;
				PermissionCollection permissions = new Permissions();
				permissions.add(new SocketPermission("localhost", "resolve,connect"));
				permissions.add(new FilePermission(path + "-", "read"));
				permissions.add(new FilePermission(path + "-", "write"));
				permissions.add(new FilePermission(path + "-", "delete"));
				access = new AccessControlContext(new ProtectionDomain[] {
						new ProtectionDomain(null, permissions)});
				new File(path).mkdirs();
			}
			else {
				host = "content";
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			JarEntry entry = null;

			while ((entry = in.getNextJarEntry()) != null) {
				if (entry.getName().endsWith(".class")) {
					pipe(in, out);
					byte[] data = out.toByteArray();
					out.reset();

					String name = name(entry.getName());
					classes.add(new Small(name, data));
				} else if (!entry.isDirectory()) {
					Big.write(host, "/" + entry.getName(), in);
				}
			}

			int length = classes.size();
			String missing = "";
			Small small = null;

			while (classes.size() > 0) {
				small = (Small) classes.elementAt(0);
				classes.removeElement(small);
				instantiate(small, daemon);
			}
		}

		protected Class findClass(String name) throws ClassNotFoundException {
			Small small = null;
			for(int i = 0; i < classes.size(); i++) {
				small = (Small) classes.get(i);
				if(small.name.equals(name)) {
					small.clazz = defineClass(small.name, small.data, 0,
							small.data.length);
					resolveClass(small.clazz);
					return small.clazz;
				}
			}
			throw new ClassNotFoundException();
		}

		private void instantiate(final Small small, Daemon daemon) throws Exception {
			if (small.clazz == null) {
				small.clazz = defineClass(small.name, small.data, 0,
						small.data.length);
				resolveClass(small.clazz);
			}

			Class clazz = small.clazz.getSuperclass();
			boolean service = false;

			while (clazz != null) {
				if (clazz.getCanonicalName().equals("se.rupy.http.Service")) {
					service = true;
				}
				clazz = clazz.getSuperclass();
			}

			if(service) {
				try {
					if(daemon.host) {
						Service s = (Service) AccessController.doPrivileged(new PrivilegedExceptionAction() {
							public Object run() throws Exception {
								return (Service) small.clazz.newInstance();
							}
						}, access());

						this.service.add(s);
					}
					else {
						this.service.add(small.clazz.newInstance());
					}
				}
				catch(Exception e) {
					if(daemon.verbose) {
						daemon.out.println(small.name + " couldn't be instantiated!");
					}
				}
			}

			if(daemon.debug) {
				daemon.out.println(small.name + (service ? "*" : ""));
			}
		}

		static Deploy.Archive deployer = new Deploy.Archive();
		
		protected AccessControlContext access() {
			return access;
		}

		protected static String name(String name) {
			name = name.substring(0, name.indexOf("."));
			name = name.replace("/", ".");

			if(name.startsWith("WEB-INF.classes")) {
				name = name.substring(16);
			}

			return name;
		}

		public String name() {
			return name;
		}

		public String host() {
			return host;
		}

		public long date() {
			return date;
		}

		protected HashMap chain() {
			return chain;
		}

		protected HashSet service() {
			return service;
		}
	}

	static class Big implements Stream {
		private File file;
		private String name;
		private long date;

		public Big(String host, String name, InputStream in, long date) throws IOException {
			file = write(host, name, in);

			this.name = name;
			this.date = date - date % 1000;
		}

		public Big(File file) {
			long date = file.lastModified();
			this.name = file.getName();
			this.file = file;
			this.date = date - date % 1000;
		}

		static File write(String host, String name, InputStream in) throws IOException {
			String path = name.substring(0, name.lastIndexOf("/"));
			String root = Deploy.path + host;

			new File(root + path).mkdirs();

			File file = new File(root + name);
			file.createNewFile();

			OutputStream out = new FileOutputStream(file);

			pipe(in, out);

			out.flush();
			out.close();

			return file;
		}

		public String name() {
			return name;
		}

		public InputStream input() {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		public long length() {
			return file.length();
		}

		public long date() {
			return date;
		}
	}

	static class Small implements Stream {
		private String name;
		private byte[] data;
		private long date;
		private Class clazz;

		public Small(String name, byte[] data) {
			this(name, data, 0);
		}

		public Small(String name, byte[] data, long date) {
			this.name = name;
			this.data = data;
			this.date = date - date % 1000;
		}

		public String name() {
			return name;
		}

		public InputStream input() {
			return new ByteArrayInputStream(data);
		}

		public long length() {
			return data.length;
		}

		public long date() {
			return date;
		}

		byte[] data() {
			return data;
		}

		public String toString() {
			return name;
		}
	}

	static interface Stream {
		public String name();
		public InputStream input();
		public long length();
		public long date();
	}

	static class Client {
		InputStream send(URL url, File file, String pass) throws IOException {
			return send(url, file, pass, true);
		}

		@SuppressWarnings("resource")
		InputStream send(URL url, File file, String pass, boolean chunk) throws IOException {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");

			OutputStream out = null;
			InputStream in = null;

			if (file != null) {
				conn.addRequestProperty("File", file.getName());
				conn.addRequestProperty("Size", "" + file.length());
				
				if (pass != null) {
					conn.addRequestProperty("Pass", pass);
				}

				if (chunk) {
					conn.setChunkedStreamingMode(0);
				}

				conn.setDoOutput(true);

				out = conn.getOutputStream();
				in = new FileInputStream(file);

				pipe(in, out);

				out.flush();
				in.close();
			}

			int code = conn.getResponseCode();

			if (code == 200) {
				in = conn.getInputStream();
			} else if (code < 0) {
				throw new IOException("HTTP response unreadable.");
			} else {
				in = conn.getErrorStream();
			}

			return in;
		}

		static String toString(InputStream in) throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			pipe(in, out);

			out.close();
			in.close();

			return new String(out.toByteArray());
		}
	}

	public static String name(String name) {
		name = name.substring(0, name.indexOf("."));
		name = name.replace("/", ".");
		return name;
	}

	public static int pipe(InputStream in, OutputStream out) throws IOException {
		return pipe(in, out, 1024, 0);
	}

	public static int pipe(InputStream in, OutputStream out, int length)
	throws IOException {
		return pipe(in, out, length, 0);
	}

	public static int pipe(InputStream in, OutputStream out, int length,
			int limit) throws IOException {
		byte[] data = new byte[length];
		int total = 0, read = in.read(data);
		while (read > -1) {
			if (limit > 0 && total > limit) {
				throw new IOException("Max allowed bytes read. (" + limit + ")");
			}
			total += read;
			out.write(data, 0, read);
			read = in.read(data);
		}
		return total;
	}

	/**
	 * Avoids the jar stream being cutoff.
	 * @author marc.larue
	 */
	static class JarInput extends JarInputStream {
		public JarInput(InputStream in) throws IOException {
			super(in);
		}

		public void close() {
			// geez
		}
	}

	public static void main(String[] args) {
		if (args.length > 2) {
			try {
				URL url = new URL("http://" + args[0] + "/deploy");
				File file = new File(args[1]);
				InputStream in = new Client().send(url, file, args[2]);
				System.out.println(new SimpleDateFormat("H:mm").format(new Date()) + " " + Client.toString(in));
			} catch (ConnectException ce) {
				System.out
				.println("Connection failed, is there a server running on "
						+ args[0] + "?");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Usage: Deploy [host] [file] [pass]");
		}
	}
}
