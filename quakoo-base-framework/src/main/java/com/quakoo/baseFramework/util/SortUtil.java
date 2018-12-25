package com.quakoo.baseFramework.util;

import java.text.DecimalFormat;

/**
 * Created by liyongbiao on 7/22/15.
 */
public class SortUtil {
	public static double createSort(int num, long id) {
		if (num <= 0) {
			num = 0;
		}
		String nums = Integer.toString(num);

		String ids = Long.toString(id);
		return Double.parseDouble(nums + "." + ids);
	}

	public static double createSort(long num, long id) {
		if (num <= 0) {
			num = 0;
		}
		String nums = Long.toString(num);
		String ids = Long.toString(id);
		if (ids.length() > 3) {
			// 取最后三位,否则double也表示不了
			ids = ids.substring(ids.length() - 3, ids.length());
		}
		return Double.parseDouble(nums + "." + ids);
	}

	public static void main(String[] fwe) {
		DecimalFormat df = new DecimalFormat("##################.###");
		// df.setMaximumFractionDigits(9);
		System.out.println(df.format(createSort(1, 1231234)));
		System.out.println((createSort(1, 924523l)));

	}
}
