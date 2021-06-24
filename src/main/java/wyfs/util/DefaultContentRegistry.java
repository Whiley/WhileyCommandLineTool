// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package wyfs.util;

import java.util.HashMap;
import java.util.Map;

import wyfs.lang.Content;
import wyfs.lang.Path;

/**
 * Default implementation of a content registry. This associates a given set
 * of content types and suffixes. The intention is that plugins register new
 * content types and these will end up here.
 *
 * @author David J. Pearce
 *
 */
public class DefaultContentRegistry implements Content.Registry {
	private HashMap<String, Content.Type> contentTypes = new HashMap<>();

	public DefaultContentRegistry register(Content.Type contentType, String suffix) {
		contentTypes.put(suffix, contentType);
		return this;
	}

	public DefaultContentRegistry unregister(Content.Type contentType, String suffix) {
		contentTypes.remove(suffix);
		return this;
	}

	@Override
	public void associate(Path.Entry e) {
		String suffix = e.suffix();
		Content.Type ct = contentTypes.get(suffix);
		if (ct != null) {
			e.associate(ct, null);
		}
	}

	@Override
	public String suffix(Content.Type<?> t) {
		for (Map.Entry<String, Content.Type> p : contentTypes.entrySet()) {
			if (p.getValue() == t) {
				return p.getKey();
			}
		}
		// Couldn't find it!
		return null;
	}

	@Override
	public Content.Type<?> contentType(String suffix) {
		return contentTypes.get(suffix);
	}
}
