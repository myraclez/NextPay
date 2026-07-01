package me.myraclez.nextPayAPI;

public final class NextPayProvider {

	private static NextPayAPI instance;

	private NextPayProvider() {}

	public static NextPayAPI getAPI() {
		if (instance == null) {
			throw new IllegalStateException("NextPay API is not initialized yet - is NextPay installed and enabled?");
		}
		return instance;
	}

	// package-private setter is not possible across modules, so make it public but clearly internal
	public static void register(NextPayAPI api) {
		instance = api;
	}

	public static void unregister() {
		instance = null;
	}
}
