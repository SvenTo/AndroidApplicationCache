package to.sven.applicationcache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.android.volley.Cache.Entry;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

public class CacheManifestRequest extends Request<Integer> {
	private final Listener<Integer> mListener;
	private final String mUrl;
	private final AppCacheDiskBasedCache mCache;
	private final ICacheRequestCreator mCacheRequestCreator;
			
    public CacheManifestRequest(String url, Listener<Integer> listener, ErrorListener errorListener,
    		AppCacheDiskBasedCache cache, ICacheRequestCreator cacheRequestCreator) {
    	super(Method.GET, url, errorListener);
        mListener = listener;
        mCache = cache;
        mCacheRequestCreator = cacheRequestCreator;
        mUrl = url;
    }

	@Override
	protected Response<Integer> parseNetworkResponse(NetworkResponse response) {
		try {
			CacheManifest manifest = CacheManifest.parse(mUrl, new ByteArrayInputStream(response.data));
			if(!response.notModified) {
				mCache.setCacheManifest(manifest);
				mCache.markPendingMasterEntries();
			}
			
			List<String> pendingMasterEntries = mCache.getPendingMasterEntries();
			for(String url : pendingMasterEntries) {
				mCacheRequestCreator.createCacheRequest(url, pendingMasterEntries.size());
			}
			
			// We save a network response with the notModified flag
			// so we see next time we get the cached entry, 
			// that it wasn't changed.
			Entry entry = HttpHeaderParser.parseCacheHeaders(new NetworkResponse(response.statusCode, response.data, response.headers, true));
			return Response.success(pendingMasterEntries.size(), entry);
		} catch(IOException ex) {
			return Response.error(new ParseError(ex));
		} catch(URISyntaxException ex) {
			// Anyway, this should not possible.
			return Response.error(new ParseError(ex));
		}
	}
	
	@Override
	protected void deliverResponse(Integer downloadCount) {
        mListener.onResponse(downloadCount);
	}
}
