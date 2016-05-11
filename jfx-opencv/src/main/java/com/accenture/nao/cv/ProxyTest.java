
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public class ProxyTest {
	public static void main(String[] args) throws Exception {
		try {
			Properties prop = System.getProperties();
			final String proxyHost = "152.160.35.171";
			final int proxyPort = 80;
			prop.setProperty("proxyHost", proxyHost);
			prop.setProperty("proxyPort", String.valueOf(proxyPort));
			prop.setProperty("http.proxyHost", proxyHost);
			prop.setProperty("http.proxyPort", String.valueOf(proxyPort));
			prop.setProperty("https.proxyHost", proxyHost);
			prop.setProperty("https.proxyPort", String.valueOf(proxyPort));

			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			URL url = new URL("https://www.twitter.com/");
			URLConnection urlConnection = url.openConnection(proxy);
			HttpURLConnection connection = null;
			if (urlConnection instanceof HttpURLConnection) {
				connection = (HttpURLConnection) urlConnection;
			} else {
				System.out.println("Please enter an HTTP URL.");
				return;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
			String urlString = "";
			String current;
			while ((current = in.readLine()) != null) {
				urlString += current;
			}
			System.out.println(urlString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
