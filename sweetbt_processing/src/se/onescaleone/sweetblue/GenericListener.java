package se.onescaleone.sweetblue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericListener implements SweetBlueListener {
	private final Object target;
	private final Method targetMethod;

	GenericListener(Object target, Method targetMethod) {
		this.target = target;
		this.targetMethod = targetMethod;
	}

	@Override
	public void SweetBlueConnected(SweetBlueEvent evt) {
		try {
			targetMethod.invoke(target, new Object[] { evt });
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
