package kr.pe.silent.websequencediagrams.editors;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class WSDUtil {
	private ServiceTracker proxyTracker;

	public WSDUtil() {
		proxyTracker = new ServiceTracker(FrameworkUtil.getBundle(
				this.getClass()).getBundleContext(),
				IProxyService.class.getName(), null);
		proxyTracker.open();
	}

	/**
	 * skin name for request
	 */
	protected static String[] skins = { "default", "rose", "qsd", "napkin",
			"mscgen",

			"omegapple", "modern-blue", "earth", "roundgreen", };

	/**
	 * skin name for UI display
	 */
	protected static String[] skinLabels = { "Plain UML", "Rose", "qsd",
			"napkin", "mscgen",

			"Omegapple", "Modern Blue", "Green Earth", "RoundGreen", };

	public IProxyService getProxyService() {
		return (IProxyService) proxyTracker.getService();
	}

	/**
	 * default skin name is Rose.
	 */
	protected static String DEFAULT_SKIN = skinLabels[1];

	public void dispose() {
		proxyTracker.close();
	}

	public void getSequenceDiagram(String text, String outFile, String style) {

		try {
			// Build parameter string
			String data = "style=" + style + "&message="
					+ URLEncoder.encode(text, "UTF-8");

			// setup a proxy
			URI uri = new URI("http://www.websequencediagrams.com");
			URL url = getProxiedUrl(uri);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(
					conn.getOutputStream());

			// write parameters
			writer.write(data);
			writer.flush();

			// Get the response
			StringBuffer answer = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				answer.append(line);
			}
			writer.close();
			reader.close();

			String json = answer.toString();
			int start = json.indexOf("?img=");
			int end = json.indexOf("\"", start);

			url = getProxiedUrl(new URI("http://www.websequencediagrams.com/"
					+ json.substring(start, end)));

			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					outFile));
			InputStream in = url.openConnection().getInputStream();
			byte[] buffer = new byte[1024];
			int numRead;
			long numWritten = 0;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}

			in.close();
			out.close();

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	private URL getProxiedUrl(URI uri) throws URISyntaxException,
			MalformedURLException {

		IProxyService proxyService = getProxyService();
		IProxyData[] proxyDataForHost = proxyService.select(uri);

		for (IProxyData proxyData : proxyDataForHost) {
			if (proxyData.getHost() != null) {
				System.setProperty("http.proxySet", "true");
				System.setProperty("http.proxyHost", proxyData.getHost());
			}
			if (proxyData.getHost() != null) {
				System.setProperty("http.proxyPort",
						String.valueOf(proxyData.getPort()));
			}
		}
		// Close the service and close the service tracker
		proxyService = null;

		// Send the request
		URL url = uri.toURL();
		return url;
	}
}