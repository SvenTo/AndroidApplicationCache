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

import com.android.volley.VolleyError;

public interface ApplicationCacheEventListener {

	  public void onChecking(ApplicationCache source);
	  
	  public void onError(ApplicationCache source, boolean wasManifest, VolleyError error);
	  
	  public void onNoUpdate(ApplicationCache source);
	  
	  public void onDownloading(ApplicationCache source, int total);
	  
	  public void onProgress(ApplicationCache source, String loadedUrl, int loaded, int total);
	  
	  public void onUpdateReady(ApplicationCache source);
	  
	  public void onObsolete(ApplicationCache source);
}
