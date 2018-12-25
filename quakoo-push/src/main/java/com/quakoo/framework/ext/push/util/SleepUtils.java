package com.quakoo.framework.ext.push.util;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;

public class SleepUtils {

	public static void sleep(int time, int min){
		int range = time - min +1;
		Random random = new Random();
	    time = random.nextInt(range) + min;
		Uninterruptibles.sleepUninterruptibly(time, TimeUnit.MILLISECONDS);
	}

}
