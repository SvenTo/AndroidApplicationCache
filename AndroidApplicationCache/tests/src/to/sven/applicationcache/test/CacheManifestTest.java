package to.sven.applicationcache.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.apache.commons.io.IOUtils;

import junit.framework.Assert;

import to.sven.applicationcache.CacheManifest;

import android.test.AndroidTestCase;

public class CacheManifestTest extends AndroidTestCase {
	public void testManifest1() throws IOException, URISyntaxException {
		 InputStream in = getContext().getAssets().open("manifest.appcache");
		 CacheManifest cacheManifest = CacheManifest.parse("http://applicationcache.sven.to:8080/ab/manifest.appcache", in, true, true);
		 Assert.assertEquals("http://applicationcache.sven.to:8080/ab/manifest.appcache", cacheManifest.getManifestURL().toString());
		 cacheManifest.setComment("TEST comment");
		 Assert.assertEquals(CacheManifest.ONLINE_WHITELIST_WILDCARD_FLAG_BLOCKING, cacheManifest.getOnlineWhitelistWildcardFlag());
		 Assert.assertEquals(CacheManifest.CACHE_MODE_FAST, cacheManifest.getCacheMode());
		 Assert.assertEquals("http://applicationcache.sven.to:8080/favicon.ico", cacheManifest.getExplicitEntries().get(0).toString());
		 Assert.assertEquals("http://applicationcache.sven.to:8080/ab/index.html", cacheManifest.getExplicitEntries().get(1).toString());
		 Assert.assertEquals("http://www.example.com/ex", cacheManifest.getExplicitEntries().get(5).toString());
		 InputStream expectIn = getContext().getAssets().open("manifest_expect.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}
	
	public void testManifestMasterEntries() throws IOException, URISyntaxException {
		 InputStream in = getContext().getAssets().open("test_master_entries.appcache");
		 CacheManifest cacheManifest = CacheManifest.parse("http://sven.to/manifest.appcache", in, true, true);
		 Iterator<URI> iterator = cacheManifest.getMasterEntries().iterator();
		 Assert.assertEquals("http://sven.to/index.html", iterator.next().toString());
		 Assert.assertEquals("http://sven.to/about.html", iterator.next().toString());
		 Assert.assertEquals("http://sven.to/offline.html", iterator.next().toString());
		 Assert.assertEquals("http://sven.to/offline.css", iterator.next().toString());
		 Assert.assertEquals(false, iterator.hasNext());
	}
	
	public void testManifest2() throws IOException, URISyntaxException {
		 InputStream in = getContext().getAssets().open("manifest2.appcache");
		 CacheManifest cacheManifest = CacheManifest.parse("https://applicationcache.sven.to:8080/ab/manifest2.appcache", in);
		 Assert.assertEquals(CacheManifest.ONLINE_WHITELIST_WILDCARD_FLAG_BLOCKING, cacheManifest.getOnlineWhitelistWildcardFlag());
		 Assert.assertEquals(CacheManifest.CACHE_MODE_PREFER_ONLINE, cacheManifest.getCacheMode());
		 InputStream expectIn = getContext().getAssets().open("manifest2_expect.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}
	
	public void testManifest3() throws IOException, URISyntaxException {
		 InputStream in = getContext().getAssets().open("manifest3.appcache");
		 CacheManifest cacheManifest = CacheManifest.parse("http://applicationcache.sven.to:8080/ab/manifest3.appcache", in);
		 Assert.assertEquals(CacheManifest.ONLINE_WHITELIST_WILDCARD_FLAG_OPEN, cacheManifest.getOnlineWhitelistWildcardFlag());
		 Assert.assertEquals(CacheManifest.CACHE_MODE_FAST, cacheManifest.getCacheMode());
		 InputStream expectIn = getContext().getAssets().open("manifest3_expect.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}

	public void testManifestSameOrigin() throws IOException, URISyntaxException {
		 InputStream in = getContext().getAssets().open("manifest3.appcache");
		 CacheManifest cacheManifest = CacheManifest.parse("http://applicationcache.sven.to:8080/ab/manifest3.appcache", in, false, true);
		 InputStream expectIn = getContext().getAssets().open("manifest3_expect_same_origin.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}

	public void testManifestSameScheme() throws IOException, URISyntaxException {
		 InputStream in = getContext().getAssets().open("manifest3.appcache");
		 CacheManifest cacheManifest = CacheManifest.parse("http://applicationcache.sven.to:8080/ab/manifest3.appcache", in, true, false);
		 InputStream expectIn = getContext().getAssets().open("manifest3_expect_same_scheme.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}
	
	public void testCreatingManifest() throws IOException, URISyntaxException {
		 CacheManifest cacheManifest = new CacheManifest(new URI("http://applicationcache.sven.to/dir/index.appcache"), true, true);
		 addEntries(cacheManifest);
		 InputStream expectIn = getContext().getAssets().open("creating_manifest_expect.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}
	
	public void testCreatingManifestWithoutManifestURI() throws IOException, URISyntaxException {
		 CacheManifest cacheManifest = new CacheManifest();
		 addEntries(cacheManifest);
		 InputStream expectIn = getContext().getAssets().open("creating_manifest_expect_nouri.appcache");
		 String expect = IOUtils.toString(expectIn, "UTF-8");
		 Assert.assertEquals(expect, cacheManifest.toString());
	}
	
	private void addEntries(CacheManifest cacheManifest) throws URISyntaxException {
		 cacheManifest.addExplicitEntry("http://www.google.de");
		 cacheManifest.addExplicitEntry("/index.html#fragment");
		 cacheManifest.addExplicitEntry("mail/home.html");
		 cacheManifest.addExplicitEntry("mail/query.html?query");
		 cacheManifest.addExplicitEntry("/dir/index.html");
	}
}
