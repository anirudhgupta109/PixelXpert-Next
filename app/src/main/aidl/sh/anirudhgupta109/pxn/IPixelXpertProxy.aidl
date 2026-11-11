// IPixelXpertProxy.aidl
package sh.anirudhgupta109.pxn;

// Root proxy service interface. The hook side runs inside the apps it hooks,
// so it reaches us over a binder rather than by calling in-process.
interface IPixelXpertProxy {
	String[] runRootCommand(String command);
}
