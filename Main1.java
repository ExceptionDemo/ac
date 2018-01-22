package neoe.ac_test;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main1 {

	public static class Target {

		public Target(byte[] k, byte[] v) {
			from = k;
			to = v;
		}

		public byte[] from, to;
	}

	public static class Dict {
		// Object[][] data;
		// int len;
		Object[] all;

		public Dict(Map m) {
			all = new Object[0x100];
			for (Object key : m.keySet()) {
				addItem(key, m.get(key));
			}

		}

		private void addItem(Object key, Object value) {
			byte[] k = (byte[]) key;
			byte[] v = (byte[]) value;
			Target t = new Target(k, v);
			addTarget(k, t);
		}

		private void addTarget(byte[] k, Target t) {
			Object[] m = all;
			int len = k.length;
			for (int i = 0; i < len; i++) {
				int c = 0xff & k[i];
				Object[] m2 = (Object[]) m[c];
				if (m2 == null) {
					if (i < len - 1) {
						m2 = new Object[0x100];
						m[c] = m2;
						m = m2;
					} else {
						m[c] = t;
					}
				} else {
					if (i < len - 1) {
						m = m2;
					} else {
						error("bug?");
					}
				}
			}
		}

		private void error(String s) {
			throw new RuntimeException(s);

		}

		public int match(byte[] bs, int sp0, int p1, Buf buf) {
			Object[] m = all;
			int p0 = sp0;
			while (true) {
				if (p0 >= p1)
					return 0;
				int c = 0xff & bs[p0];
				Object o = m[c];
				if (o == null)
					return 0;
				if (o instanceof Target) {
					Target target = (Target) o;
					byte[] from = target.from;
					byte[] to = target.to;
					System.arraycopy(to, 0, buf.res, buf.pr, to.length);
					buf.pr += to.length;
					return from.length;
				} else {
					m = (Object[]) o;
					p0++;
				}
			}
		}

	}

	private static final String UTF8 = "utf-8";

	public static void main(String[] args) throws Exception {
		new Main1().run("G:\\neoe\\oss\\ac\\data\\dict.txt\\dict2.txt", "G:\\neoe\\oss\\ac\\data\\video_title.txt",
				"G:\\neoe\\oss\\ac\\data\\result.txt");
	}

	Timer ti = new Timer();

	private void run(String dict, String inputfn, String outputfn) throws Exception {

		byte[] bs = readBs2(inputfn);
		ti.check("read");
		// System.out.println("bs:"+bs.length);
		String[] dictss = new String(readBs2(dict), UTF8).split("\\n");
		Arrays.sort(dictss);
		ti.check("dict1");
		Map dictmap = new HashMap(dictss.length);
		int k = dictss.length - 1;
		String s0 = "";
		Map m2 = new HashMap();
		for (String s : new String[] { "-*电影*-", "-*音乐*-", "-*音乐&电影*-" }) {
			m2.put(s, s.getBytes(UTF8));
		}

		// data size is small
		Map<Object, Object> dictmap2 = new HashMap();
		for (int i = k; i >= 0; i--) {
			String s = dictss[i];
			int p1 = s.indexOf('\t');
			String s1 = s.substring(0, p1);
			String s2 = s.substring(p1 + 1);
			if (s0.startsWith(s1)) {
				System.out.println("skip " + s1 + " because " + s0);
				dictmap2.put(s1.getBytes(), m2.get(s2));// bug if there are level 3
				continue;
			}
			s0 = s1;
			dictmap.put(s1.getBytes(), m2.get(s2));
		}
		// System.out.println("dictmap:"+dictmap.size());
		ti.check("dict2");
		Dict d = new Dict(dictmap);
		Dict d2 = new Dict(dictmap2);
		ti.check("dict3");
		replace(bs, new FileOutputStream(outputfn), d, d2);
		ti.check("all");
	}

	static class Timer {
		private long t1, t0;

		public Timer() {
			t0 = t1 = System.currentTimeMillis();
		}

		public void check(String info) {
			long t2 = System.currentTimeMillis();
			System.out.println("[t][" + info + "]" + (t2 - t1) + "/" + (t2 - t0));
			t1 = t2;
		}
	}

	private void replace(byte[] bs, FileOutputStream out, Dict d, Dict d2) throws IOException {
		Buf buf = new Buf();
		buf.res = new byte[bs.length * 2];// *may* be enough
		buf.pr = 0;
		submit(bs, 0, bs.length, buf, d, d2);
		ti.check("submit");
		out.write(buf.res, 0, buf.pr);
		out.close();
	}

	static class Buf {
		byte[] res;
		int pr;
	}

	private static void submit(byte[] bs, int p0, int p1, Buf buf, Dict d, Dict d2) {
		int sp0 = p0;
		while (true) {
			int r = d.match(bs, sp0, p1, buf);
			if (r == 0) {
				r = d2.match(bs, sp0, p1, buf);
			}
			if (r == 0) {
				buf.res[buf.pr++] = bs[sp0];
				sp0++;
			} else {
				sp0 += r;
			}

			if (sp0 >= p1)
				break;
		}
	}

	private byte[] readBs2(String fn) throws IOException {
		File f = new File(fn);
		return readBs(new FileInputStream(f), (int) f.length());
	}

	public static byte[] readBs(InputStream in, int len) throws IOException {
		byte[] b = new byte[len];
		int off = 0;
		int n = 0;
		while (n < len) {
			int count = in.read(b, off + n, len - n);
			if (count < 0)
				throw new EOFException();
			n += count;
		}
		return b;
	}

}
