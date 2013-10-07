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

import java.io.File;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.util.Pair;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

/**
 * An implementation of the ApplicationCache for Android.
 * Important!: If you use this Class
 * @author sven
 *
 */
@SuppressLint("NewApi")
public class ApplicationCache implements ICacheRequestCreator {

	public ApplicationCacheEventListener mEventListener;
	private short mStatus = STATUS_UNCACHED;
	private RequestQueue mQueue;
	private AppCacheDiskBasedCache mCache;
	private int mDownloadTotal = 0;
	private int mDownloadProgress = 0;
	private String mManifestURL;


    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "applicationCache";
	
	// update status
	public static final short STATUS_UNCACHED = 0;
	public static final short STATUS_IDLE = 1;
	public static final short STATUS_CHECKING = 2;
	public static final short STATUS_DOWNLOADING = 3;
	public static final short STATUS_UPDATEREADY = 4;
	public static final short STATUS_OBSOLETE = 5;
	
	public ApplicationCache() {
		
	}
	
	/**
	 * 
	 * @param manifestURL
	 * @throws URISyntaxException 
	 */
	public void init(String manifestURL, Context context) throws URISyntaxException {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        AppCacheDiskBasedCache cache = new AppCacheDiskBasedCache(cacheDir, manifestURL);
        init(manifestURL, context, cache);
	}
	
	/**
	 * 
	 * @param manifestURL
	 * @throws URISyntaxException 
	 */
	public void init(String manifestURL, Context context, AppCacheDiskBasedCache cache) throws URISyntaxException {
		mManifestURL = manifestURL;
		mCache = cache;
		mQueue = newRequestQueue(context, null);
		updateInternal(false);
	}
	
	private RequestQueue newRequestQueue(Context context, HttpStack stack) {

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();

            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);

        RequestQueue queue = new RequestQueue(mCache, network);
        queue.start();

        return queue;
    }

	public RequestQueue getQueue() {
		return mQueue;
	}

	public AppCacheDiskBasedCache getCache() {
		return mCache;
	}

	/**
	 * Returning the Update Status
	 * Note: Before calling {@link ApplicationCache#init(String)} this method returns {@link #UNCACHED}.
	 * @return Update Status
	 */
	public short getStatus() {
		return mStatus;
	}

	/**
	 * Starts the update.
	 * @return True, if the update was started, false if it's already running.
	 */
	public boolean update(boolean force) {
		switch (mStatus) {
		case STATUS_UNCACHED:
			throw new IllegalStateException("Run init(...) before!");
		case STATUS_IDLE:
		case STATUS_UPDATEREADY:
		case STATUS_OBSOLETE:
			updateInternal(force);
			return true;
		case STATUS_CHECKING:
		case STATUS_DOWNLOADING:
		default:
			return false;
		}
	}
	
	private void updateInternal(boolean force) {
		mStatus = STATUS_CHECKING;
		// TODO: Proirity High?
		mDownloadProgress = 0;
		if(force) {
			mCache.invalidate(mManifestURL, true);
		}
		addRequest(new CacheManifestRequest(mManifestURL, manifestListener, manifestErrorListener, mCache, this));
	}

	public void abort() {
		mQueue.cancelAll(this);
		mStatus = STATUS_IDLE;
	}

	public void swapCache() {
		// TODO: Implement this correct?
		if(mStatus == STATUS_UPDATEREADY) {
			mStatus = STATUS_IDLE;
		} else {
			throw new IllegalStateException("Not in state STATUS_UPDATEREADY.");
		}
	}

	// events
	public void setEventListener(ApplicationCacheEventListener listener) {
		this.mEventListener = listener;
	}

	private final ErrorListener manifestErrorListener = new ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			mStatus = STATUS_OBSOLETE;
			if(mEventListener != null) {
				mEventListener.onError(ApplicationCache.this, false, error);
				mEventListener.onObsolete(ApplicationCache.this);
			}
		}
	};
	
	private final Listener<Integer> manifestListener = new Listener<Integer>() {
		@Override
		public void onResponse(Integer downloadTotal) {
			if(downloadTotal > 0) {
				mStatus = STATUS_DOWNLOADING;
				if(mEventListener != null) {
					mEventListener.onDownloading(ApplicationCache.this, downloadTotal);
				}
			} else {
				// No update and no unloaded master entries from a previous update.
				mStatus = STATUS_IDLE;
				if(mEventListener != null) {
					mEventListener.onNoUpdate(ApplicationCache.this);
				}
			}
		}
	};
	
	private final ErrorListener downloadErrorListener = new ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			if(mEventListener != null) {
				mEventListener.onError(ApplicationCache.this, false, error);
			}
		}
	};
	

	private final Listener<Pair<String,Integer>> downloadProgressListener = new Listener<Pair<String,Integer>>() {

		@Override
		public void onResponse(Pair<String,Integer> downloadedUrl) {
			mDownloadProgress++;
			if(mDownloadProgress == downloadedUrl.second) {
				mStatus = STATUS_UPDATEREADY;
			}
			if(mEventListener != null) {
				mEventListener.onProgress(ApplicationCache.this, downloadedUrl.first, mDownloadProgress, downloadedUrl.second);
				if(mStatus == STATUS_UPDATEREADY) {
					mEventListener.onUpdateReady(ApplicationCache.this);
				}
			}
		}
		
	};
	
	// TODO: GeCachte Responses können früher als DownloadProgess kommen:
	@Override
	public void createCacheRequest(String url, int total) {
		addRequest(new CacheOnlyReqeuest(url, downloadProgressListener, downloadErrorListener , total));
	}
	
	private void addRequest(Request<?> r) {
		r.setTag(this);
		mQueue.add(r);
	}
	
}
