package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.Utils.System;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class DoubleTapSleepLS extends XposedModPack {
	public static final String listenPackage = "com.android.systemui";
	public static boolean doubleTapToSleepEnabled = false;
	
	public DoubleTapSleepLS(Context context) { super(context); }
	
	public void updatePrefs(String...Key)
	{
		if(XPrefs.Xprefs == null) return;
		doubleTapToSleepEnabled = XPrefs.Xprefs.getBoolean("DoubleTapSleep", false);
	}
	
	GestureDetector mLockscreenDoubleTapToSleep = null;
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if(!lpparam.packageName.equals(listenPackage) || true) return;
		
		Class<?> NotificationPanelViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
		
		XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader,
				"createTouchHandler", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Object touchHandler = param.getResult();
						Object ThisNotificationPanel = param.thisObject;
						
						XposedHelpers.findAndHookMethod(touchHandler.getClass(),
								"onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
									@Override
									protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
										if(!doubleTapToSleepEnabled) return;
										
										boolean mPulsing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mPulsing");
										boolean mDozing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mDozing");
										int mBarState = (int) XposedHelpers.getObjectField(ThisNotificationPanel, "mBarState");
										
										if (!mPulsing && !mDozing
												&& mBarState < 2 && mLockscreenDoubleTapToSleep != null) {
											mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[1]);
										}
									}
								});
					}
				});
		
		
		XposedBridge.hookAllConstructors(NotificationPanelViewControllerClass,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
							@Override
							public boolean onDoubleTap(MotionEvent e) {
								System.Sleep();
								return true;
							}
						});
					}
				});
		
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
}
