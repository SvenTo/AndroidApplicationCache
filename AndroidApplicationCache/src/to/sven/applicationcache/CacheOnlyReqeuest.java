package to.sven.applicationcache;

import android.util.Pair;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;


public class CacheOnlyReqeuest extends Request<Pair<String,Integer>> {

	private final Listener<Pair<String,Integer>> mListener;
	private final String mUrl;
	private final int mTotal;
	
	public CacheOnlyReqeuest(String url, Listener<Pair<String,Integer>> listener, ErrorListener errorListener, int total) {
		super(Method.GET, url, errorListener);
        mListener = listener;
        mUrl = url;
        mTotal = total;
	}

	@Override
	protected Response<Pair<String,Integer>> parseNetworkResponse(NetworkResponse response) {
		try {
			return Response.success(new Pair<String,Integer>(mUrl, mTotal),
									HttpHeaderParser.parseCacheHeaders(response));
		} catch (Exception e) {
			return Response.error(new ParseError(e));
		}
	}
	
	@Override
	protected void deliverResponse(Pair<String,Integer> cachedUrl) {
        mListener.onResponse(cachedUrl);
	}
}
