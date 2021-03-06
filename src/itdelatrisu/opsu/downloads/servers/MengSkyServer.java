/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.downloads.servers;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import static yugecin.opsudance.core.errorhandling.ErrorHandler.*;

/**
 * Download server: http://osu.mengsky.net/
 */
public class MengSkyServer extends DownloadServer {

	/** Server name. */
	private static final String SERVER_NAME = "MengSky";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "http://osu.mengsky.net/api/download/%d";

	/** Formatted search URL: {@code query,page,unranked,approved,qualified} */
	private static final String SEARCH_URL = "http://osu.mengsky.net/api/beatmapinfo?query=%s&page=%d&ranked=1&unrank=%d&approved=%d&qualified=%d";

	/** Maximum beatmaps displayed per page. */
	private static final int PAGE_LIMIT = 20;

	/** Total result count from the last query. */
	private int totalResults = -1;

	@Override
	public String getName() { return SERVER_NAME; }

	@Override
	public String getDownloadURL(int beatmapSetID) {
		return String.format(DOWNLOAD_URL, beatmapSetID);
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		DownloadNode[] nodes = null;
		try {
			// read JSON
			int rankedOnlyFlag = rankedOnly ? 0 : 1;
			String search = String.format(
				SEARCH_URL, URLEncoder.encode(query, "UTF-8"), page,
				rankedOnlyFlag, rankedOnlyFlag, rankedOnlyFlag
			);
			JSONObject json = Utils.readJsonObjectFromUrl(new URL(search));

			// parse result list
			JSONArray arr = json.getJSONArray("data");
			nodes = new DownloadNode[arr.length()];
			for (int i = 0; i < nodes.length; i++) {
				JSONObject item = arr.getJSONObject(i);
				String
					title = item.getString("title"), titleU = item.getString("titleU"),
					artist = item.getString("artist"), artistU = item.getString("artistU"),
					creator = item.getString("creator");
				// bug with v1.x API (as of 10-13-16):
				// sometimes titleU is artistU instead of the proper title
				if (titleU.equals(artistU) && !titleU.equals(title))
					titleU = title;
				nodes[i] = new DownloadNode(
					item.getInt("id"), item.getString("syncedDateTime"),
					title, titleU, artist, artistU, creator
				);
			}

			// store total result count
			int pageTotal = json.getInt("pageTotal");
			int resultCount = 1 + (pageTotal - 1) * PAGE_LIMIT;
			if (page == pageTotal) {
				resultCount += nodes.length - 1;
			}
			this.totalResults = resultCount;
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			explode(String.format("Problem loading result list for query '%s'.", query), e, DEFAULT_OPTIONS);
		}
		return nodes;
	}

	@Override
	public int minQueryLength() { return 1; }

	@Override
	public int totalResults() { return totalResults; }
}
