package org.erlide.core.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.Preferences;

public class PreferencesHelper {

	private IScopeContext[] loadContexts;
	private IScopeContext storeContext;
	private IPreferencesService service;
	private String qualifier;

	PreferencesHelper(String qualifier, IScopeContext[] loadContexts,
			IScopeContext storeContext) {
		this.loadContexts = loadContexts;
		this.storeContext = storeContext;
		service = Platform.getPreferencesService();
		this.qualifier = qualifier;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		return service.getBoolean(qualifier, key, defaultValue, loadContexts);
	}

	public byte[] getByteArray(String key, byte[] defaultValue) {
		return service.getByteArray(qualifier, key, defaultValue, loadContexts);
	}

	public double getDouble(String key, double defaultValue) {
		return service.getDouble(qualifier, key, defaultValue, loadContexts);
	}

	public float getFloat(String key, float defaultValue) {
		return service.getFloat(qualifier, key, defaultValue, loadContexts);
	}

	public int getInt(String key, int defaultValue) {
		return service.getInt(qualifier, key, defaultValue, loadContexts);
	}

	public long getLong(String key, long defaultValue) {
		return service.getLong(qualifier, key, defaultValue, loadContexts);
	}

	public String getString(String key, String defaultValue) {
		return service.getString(qualifier, key, defaultValue, loadContexts);
	}

	public String[] childrenNames(String key) {
		return childrenNames(getNodes(qualifier, key, loadContexts));
	}

	public void putBoolean(String key, boolean value) {
		storeContext.getNode(qualifier).putBoolean(key, value);
	}

	public void putByteArray(String key, byte[] defaultValue) {
		storeContext.getNode(qualifier).putByteArray(key, defaultValue);
	}

	public void putDouble(String key, double defaultValue) {
		storeContext.getNode(qualifier).putDouble(key, defaultValue);
	}

	public void putFloat(String key, float defaultValue) {
		storeContext.getNode(qualifier).putFloat(key, defaultValue);
	}

	public void putInt(String key, int defaultValue) {
		storeContext.getNode(qualifier).putInt(key, defaultValue);
	}

	public void putLong(String key, long defaultValue) {
		storeContext.getNode(qualifier).putLong(key, defaultValue);
	}

	public void putString(String key, String defaultValue) {
		storeContext.getNode(qualifier).put(key, defaultValue);
	}

	//

	private Preferences[] getNodes(String qualifier, String key,
			IScopeContext[] contexts) {
		String[] order = service.getLookupOrder(qualifier, key);
		String childPath = makeRelative(decodePath(key)[0]);
		ArrayList<Preferences> result = new ArrayList<Preferences>();
		for (int i = 0; i < order.length; i++) {
			String scopeString = order[i];
			boolean found = false;
			for (int j = 0; contexts != null && j < contexts.length; j++) {
				IScopeContext context = contexts[j];
				if (context != null && context.getName().equals(scopeString)) {
					Preferences node = context.getNode(qualifier);
					if (node != null) {
						found = true;
						if (childPath != null) {
							node = node.node(childPath);
						}
						result.add(node);
					}
				}
			}
			if (!found) {
				Preferences node = service.getRootNode().node(scopeString)
						.node(qualifier);
				if (childPath != null) {
					node = node.node(childPath);
				}
				result.add(node);
			}
			found = false;
		}
		return result.toArray(new Preferences[result.size()]);
	}

	// copied these from EclipsePreferences

	protected static final String DOUBLE_SLASH = "//"; //$NON-NLS-1$
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/*
	 * Return a relative path
	 */
	public static String makeRelative(String path) {
		String result = path;
		if (path == null) {
			return EMPTY_STRING;
		}
		if (path.length() > 0 && path.charAt(0) == IPath.SEPARATOR) {
			result = path.length() == 0 ? EMPTY_STRING : path.substring(1);
		}
		return result;
	}

	/*
	 * Return a 2 element String array. element 0 - the path element 1 - the key
	 * The path may be null. The key is never null.
	 */
	public static String[] decodePath(String fullPath) {
		String key = null;
		String path = null;

		// check to see if we have an indicator which tells us where the path
		// ends
		int index = fullPath.indexOf(DOUBLE_SLASH);
		if (index == -1) {
			// we don't have a double-slash telling us where the path ends
			// so the path is up to the last slash character
			int lastIndex = fullPath.lastIndexOf(IPath.SEPARATOR);
			if (lastIndex == -1) {
				key = fullPath;
			} else {
				path = fullPath.substring(0, lastIndex);
				key = fullPath.substring(lastIndex + 1);
			}
		} else {
			// the child path is up to the double-slash and the key
			// is the string after it
			path = fullPath.substring(0, index);
			key = fullPath.substring(index + 2);
		}

		// adjust if we have an absolute path
		if (path != null) {
			if (path.length() == 0) {
				path = null;
			} else if (path.charAt(0) == IPath.SEPARATOR) {
				path = path.substring(1);
			}
		}

		return new String[] { path, key };
	}

	// end EclipsePreferences

	public String[] childrenNames(Preferences[] nodes) {
		List<String> names = new ArrayList<String>();
		if (nodes == null) {
			return names.toArray(new String[0]);
		}
		for (int i = 0; i < nodes.length; i++) {
			Preferences node = nodes[i];
			if (node != null) {
				try {
					String[] myNames = node.childrenNames();
					for (String aName : myNames) {
						if (!names.contains(aName)) {
							names.add(aName);
						}
					}
				} catch (Exception e) {
				}
			}
		}
		return names.toArray(new String[names.size()]);
	}

}