/*
 * Copyright (C) 2013 Sven Nobis ( sven.to )
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package to.sven.applicationcache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a HTML application manifest's file.
 * Supports parsing and serializing it. 
 * 
 * @author sven
 */
public class CacheManifest {
	
	private static final String CHARSET = "UTF-8";
	private static final String MAGIC_NUMBER = "CACHE MANIFEST";
	private static final String SECTION_CACHE = "CACHE:";
	private static final String SECTION_FALLBACK = "FALLBACK:";
	private static final String SECTION_NETWORK = "NETWORK:";
	private static final String SECTION_SETTINGS = "SETTINGS:";
	private static final String MODE_FLAG_PREFER_ONLINE = "prefer-online";
	
	public static final short CACHE_MODE_FAST = 0;
	public static final short CACHE_MODE_PREFER_ONLINE = 1;

	public static final short ONLINE_WHITELIST_WILDCARD_FLAG_BLOCKING = 0;
	public static final short ONLINE_WHITELIST_WILDCARD_FLAG_OPEN = 1;

	private static final short MODE_UNKNOWN = -1;
	private static final short MODE_EXPLICIT = 0;
	private static final short MODE_FALLBACK = 1;
	private static final short MODE_NETWORK = 2;
	private static final short MODE_SETTINGS = 3;
	
	private final URI mManifestURL;
	private short mCacheMode = CACHE_MODE_FAST;
	private final List<URI> mExplicitEntries = new ArrayList<URI>();
	private final Map<URI, URI> mFallbackEntries = new LinkedHashMap<URI, URI>();
	private final List<URI> mOnlineWhitelistNamespaces = new ArrayList<URI>();
	private final LinkedHashSet<URI> mMasterEntries = new LinkedHashSet<URI>(); 
	private short mOnlineWhitelistWildcardFlag = ONLINE_WHITELIST_WILDCARD_FLAG_BLOCKING;
	
	private int mode = MODE_EXPLICIT;
	private String[] validSchemes = { "http", "https" };
	private boolean mValidateSameOrgin;
	private String comment;
	
	/**
	 * Creates an empty manifest
	 */
	public CacheManifest() {
		mManifestURL = null;
		validSchemes = new String[] { "http", "https", null };
	}
	
	/**
	 * Creates an empty manifest for the given manifest's URL.
	 * @param manifestURL The manifest's URL.
	 * @throws URISyntaxException 
	 */
	public CacheManifest(URI manifestURL) throws URISyntaxException {
		this(manifestURL, false, false);
	}
	
	/**
	 * Creates an empty manifest for the given manifest's URL.
	 * @param manifestURL The manifest's URL.
	 * @throws URISyntaxException 
	 */
	public CacheManifest(String manifestURL) throws URISyntaxException {
		this(new URI(manifestURL));
	}
	
	/**
	 * Creates an empty manifest for the given manifest's URL.
	 * @param manifestURL The manifest's URL.
	 * @throws URISyntaxException 
	 */
	public CacheManifest(URI manifestURL, boolean strictSchemes, boolean validateSameOrgin) throws URISyntaxException {
		mManifestURL = manifestURL;
		validateManifestURL();
		if(strictSchemes) {
			validSchemes = new String[] { mManifestURL.getScheme() };
		}
		mValidateSameOrgin = validateSameOrgin;
	}
	
	/**
	 * Creates an empty manifest for the given manifest's URL.
	 * @param manifestURL The manifest's URL.
	 * @throws URISyntaxException 
	 */
	public CacheManifest(String manifestURL, boolean strictSchemes, boolean validateSameOrgin) throws URISyntaxException {
		this(new URI(manifestURL), strictSchemes, validateSameOrgin);
	}
	
	private CacheManifest(String manifestURL, InputStream inStream, boolean strictSchemes, boolean validateSameOrgin) throws IOException, URISyntaxException {
		mManifestURL = new URI(manifestURL);
		validateManifestURL();
		if(strictSchemes) {
			validSchemes = new String[] { mManifestURL.getScheme() };
		}
		mValidateSameOrgin = validateSameOrgin;
		
		parse(inStream);
	}
	
	/**
	 * Parses the given manifest's file and returns it as {@link CacheManifest}.
	 * @param manifestURL The manifest's URL.
	 * @param inStream The content of the manifest's file.
	 * @param strictSchemes Don't allow different scheme component than the manifest's URL;
	 * @param validateSameOrgin Check's if the URLs in the fallback mode following the same origin policy.
	 * @throws IOException 
	 * @throws URISyntaxException  
	 */
	public static CacheManifest parse(String manifestURL, InputStream inStream, boolean strictSchemes, boolean validateSameOrgin) throws IOException, URISyntaxException {
		return new CacheManifest(manifestURL, inStream, strictSchemes, validateSameOrgin);
	}
	
	/**
	 * Parses the given manifest's file without {@code strictSchemes}.
	 * @see {@link CacheManifest#parse(String, InputStream, boolean)}
	 */
	public static CacheManifest parse(String manifestURL, InputStream inStream) throws IOException, URISyntaxException {
		return parse(manifestURL, inStream, false, false);
	}
	
	private void validateManifestURL() throws URISyntaxException {
		if(!mManifestURL.isAbsolute()) {
			throw new URISyntaxException(mManifestURL.toString(), "This is not an absolute uri.");
		} else if(!validSchemes[0].equals(mManifestURL.getScheme()) &&
				  !validSchemes[1].equals(mManifestURL.getScheme())) {
			throw new URISyntaxException(mManifestURL.toString(), "Invalid scheme.");
		}
	}
	
	private void parse(InputStream inStream) throws IOException, URISyntaxException {
		InputStreamReader reader = new InputStreamReader(inStream, Charset.forName(CHARSET));
		BufferedReader buffReader = new BufferedReader(reader);
		String line = buffReader.readLine();
		if(!isMagicNumber(line)) {
			throw new IOException("Not a cache manifest file.");
		}
		
		while((line = buffReader.readLine()) != null) {
			line = line.trim();
			if(line.length() == 0) continue;
			if(isComment(line)) continue;
			if(parseMode(line)) continue;
			
			String[] tokens = line.split("[ \n]");
			processTokens(tokens);
		}
	}
	
	private void processTokens(String[] tokens) throws URISyntaxException {
		switch(mode) {
		case MODE_EXPLICIT:
			addExplicitEntry(tokens[0]);
			return;
		case MODE_FALLBACK:
			if(tokens.length < 2) return;
			addFallbackEntry(tokens[0], tokens[1]);
			return;
		case MODE_NETWORK:
			addNetworkEntry(tokens[0]);
			return;
		case MODE_SETTINGS:
			processSettings(tokens);
			return;
		case MODE_UNKNOWN:
			return;
		}
	}

	/**
	 * Adds URI to Explicit/CACHE section.
	 * @param uri URI
	 * @return true if it was added, false if it's an invalid URI.
	 * @throws URISyntaxException 
	 */
	public boolean addExplicitEntry(String uri) throws URISyntaxException {
		return addExplicitEntry(parseURI(uri));
	}
	
	/**
	 * Adds URI to Explicit/CACHE Section.
	 * @param uri URI
	 * @return true if it was added, false if it's an invalid URI.
	 */
	public boolean addExplicitEntry(URI uri) {
		uri = removeFragment(uri);
		if(uri.equals(mManifestURL)) return false;
		if(!validateSchemes(uri)) return false;
		mExplicitEntries.add(uri);
		mMasterEntries.add(uri);
		return true;
	}

	/**
	 * Adds URI to FALLBACK section.
	 * @param uri URI
	 * @return true if it was added, false if it's an invalid URI.
	 * @throws URISyntaxException 
	 */
	public boolean addFallbackEntry(String fallbackNameSpace, String fallbackEntry) throws URISyntaxException {
		return addFallbackEntry(parseURI(fallbackNameSpace), parseURI(fallbackEntry));
	}

	/**
	 * Adds URI to FALLBACK section.
	 * @param uri URI
	 * @return true if it was added, false if it's an invalid URI.
	 */
	public boolean addFallbackEntry(URI fallbackNameSpace, URI fallbackEntry) {
		fallbackNameSpace = removeFragment(fallbackNameSpace);
		fallbackEntry = removeFragment(fallbackEntry);
		if(!validateSameOriginPolicy(fallbackNameSpace)) return false;
		if(!validateSameOriginPolicy(fallbackEntry)) return false;
		if(fallbackNameSpace.equals(mManifestURL)) return false;
		if(fallbackEntry.equals(mManifestURL)) return false;
		
		if(mFallbackEntries.put(fallbackNameSpace, fallbackEntry) == null) {
			mMasterEntries.add(fallbackEntry);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Adds URI to NETWORK section.
	 * If the URI is "*", then "online whitelist wildcard flag" is set to open.
	 * @param uri URI
	 * @return true if it was added, false if it's an invalid URI.
	 * @throws URISyntaxException 
	 */
	public boolean addNetworkEntry(String uri) throws URISyntaxException {
		if(uri.equals("*")) {
			setOnlineWhitelistWildcardFlag(ONLINE_WHITELIST_WILDCARD_FLAG_OPEN);
			return true;
		} else {
			return addNetworkEntry(parseURI(uri));
		}
	}

	/**
	 * Adds URI to NETWORK section.
	 * 
	 * @param uri URI
	 * @return true if it was added, false if it's an invalid URI.
	 */
	public boolean addNetworkEntry(URI uri) {
		uri = removeFragment(uri);
		if(uri.equals(mManifestURL)) return false;
		if(!validateSchemes(uri)) return false;
		
		return mOnlineWhitelistNamespaces.add(uri);
	}

	private void processSettings(String[] tokens) {
		if(tokens[0].equals(MODE_FLAG_PREFER_ONLINE)) {
			setCacheMode(CACHE_MODE_PREFER_ONLINE);
		}
	}

	private boolean isComment(String line) {
		return line.startsWith("#");
	}

	private boolean parseMode(String line) {
		if(line.equals(SECTION_CACHE))
			mode = MODE_EXPLICIT;
		else if(line.equals(SECTION_FALLBACK))
			mode = MODE_FALLBACK;
		else if(line.equals(SECTION_NETWORK))
			mode = MODE_NETWORK;
		else if(line.equals(SECTION_SETTINGS))
			mode = MODE_SETTINGS;
		else if(line.endsWith(":"))
			mode = MODE_UNKNOWN;
		else
			return false;
		
		return true;
	}
	
	/**
	 * Validates if the given URI follows the same origin policy.
	 * If validation was not enabled during initialization, it returns always true. 
	 * @param uri
	 * @return True, if validation was successful.
	 * @throws IllegalStateException If not manifest URL was given and validation is on. 
	 */
	public boolean validateSameOriginPolicy(URI uri) {
		if(!mValidateSameOrgin) return true;
		if(mManifestURL == null) {
			throw new IllegalStateException("Mainifest URL was not given.");
		}
		
		return (uri.getHost().equalsIgnoreCase(mManifestURL.getHost()) &&
				uri.getPort() == mManifestURL.getPort() &&
				uri.getScheme().equalsIgnoreCase(mManifestURL.getScheme()));
	}
	
	/**
	 * Validates if the given URI has a valid scheme (HTTP/HTTPS).
	 * If strictSchemes is enabled, only the Mainifest Scheme is allowed.
	 * @param uri
	 * @return True, if validation was successful.
	 */
	public boolean validateSchemes(URI uri) {
		for(String scheme : validSchemes) {
			if((scheme == null && uri.getScheme() == null) ||
			   scheme.equalsIgnoreCase(uri.getScheme())) {
				return true;
			}
		}
		return false;
	}

	private boolean isMagicNumber(String line) {
		return line != null && line.startsWith(MAGIC_NUMBER) && line.substring(MAGIC_NUMBER.length()).trim().length() == 0;
	}
	
	private URI parseURI(String token) throws URISyntaxException {
		if(mManifestURL != null) {
			return mManifestURL.resolve(token);
		} else {
			return new URI(token);
		}
	}
	
	private URI removeFragment(URI uri) {
		try {
			return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null);
		} catch (URISyntaxException e) {
			// Could not happen.
			return null;
		}
	}
	
	/**
	 * Creates the Cache Manifest
	 */
	@Override
	public String toString() {
		final String LF = "\n";
		final StringBuilder builder = new StringBuilder();
		builder.append(MAGIC_NUMBER).append(LF);
		
		if(comment != null) {
			builder.append("# " + comment).append(LF);	
		}
		
		for(URI uri : mExplicitEntries) {
			builder.append(toString(uri)).append(LF);
		}
		
		if(!mFallbackEntries.isEmpty()) {
			builder.append(SECTION_FALLBACK).append(LF);
			for(Entry<URI, URI> uriSet : mFallbackEntries.entrySet()) {
				builder
					.append(toString(uriSet.getKey()))
					.append(" ")
					.append(toString(uriSet.getValue()))
					.append(LF);
			}
		}

		if(!mOnlineWhitelistNamespaces.isEmpty() || getOnlineWhitelistWildcardFlag() == ONLINE_WHITELIST_WILDCARD_FLAG_OPEN) {
			builder.append(SECTION_NETWORK).append(LF);
			
			if(getOnlineWhitelistWildcardFlag() == ONLINE_WHITELIST_WILDCARD_FLAG_OPEN) {
				builder.append("*" + LF);
			}
			
			for(URI uri : mOnlineWhitelistNamespaces) {
				builder.append(toString(uri)).append(LF);
			}
		}
		
		if(mCacheMode == CACHE_MODE_PREFER_ONLINE) {
			builder.append(SECTION_SETTINGS).append(LF);
			builder.append(MODE_FLAG_PREFER_ONLINE).append(LF);
		}
		
		return builder.toString(); 
	}
	
	private String toString(URI uri) {
		if(mManifestURL != null) {
			uri = mManifestURL.relativize(uri);
			if(getWithoutPath(uri).equals(getWithoutPath(mManifestURL))) {
				return uri.getPath();
			}
		}
		return uri.toString();
	}
	
	private URI getWithoutPath(URI uri) {
		try {
			return new URI(uri.getScheme(), uri.getAuthority(), null, null, null);
		} catch (URISyntaxException e) {
			// Could not happen.
			return null;
		}
	}
	
	//
	// Getter and Setter:
	//
	
	public short getOnlineWhitelistWildcardFlag() {
		return mOnlineWhitelistWildcardFlag;
	}

	public void setOnlineWhitelistWildcardFlag(short onlineWhitelistWildcardFlag) {
		if(onlineWhitelistWildcardFlag != ONLINE_WHITELIST_WILDCARD_FLAG_BLOCKING &&
		   onlineWhitelistWildcardFlag != ONLINE_WHITELIST_WILDCARD_FLAG_OPEN ) {
			throw new IllegalArgumentException();
		}
		mOnlineWhitelistWildcardFlag = onlineWhitelistWildcardFlag;
	}

	public short getCacheMode() {
		return mCacheMode;
	}

	public void setCacheMode(short cacheMode) {
		if(cacheMode != CACHE_MODE_FAST &&
		   cacheMode != CACHE_MODE_PREFER_ONLINE) {
			throw new IllegalArgumentException();
		}
		mCacheMode = cacheMode;
	}

	public URI getManifestURL() {
		return mManifestURL;
	}
	
	public LinkedHashSet<URI> getMasterEntries() {
		return new LinkedHashSet<URI>(mMasterEntries); 
	}
	
	public boolean isMasterEntry(String url) {
		try {
			URI uri = new URI(url);
			return mMasterEntries.contains(uri);
		} catch (URISyntaxException e) {
			return false;
		}
	}

	public ArrayList<URI> getExplicitEntries() {
		return new ArrayList<URI>(mExplicitEntries);
	}

	public Map<URI, URI> getFallbackEntries() {
		return new LinkedHashMap<URI, URI>(mFallbackEntries);
	}

	public ArrayList<URI> getOnlineWhitelistNamespaces() {
		return new ArrayList<URI>(mOnlineWhitelistNamespaces);
	}
	
	public void clear() {
		mExplicitEntries.clear();
		mFallbackEntries.clear();
		mOnlineWhitelistNamespaces.clear();
		mMasterEntries.clear();
	}

	public String getComment() {
		return comment;
	}

	/**
	 * Allows to add an comment into the manifest file.
	 * @param comment The comment
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
}
