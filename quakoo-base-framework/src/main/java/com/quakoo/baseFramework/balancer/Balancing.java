package com.quakoo.baseFramework.balancer;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * 
 * @author liyongbiao
 *
 */
public class Balancing {

	public static abstract class RoundIndex {
		private int _n;

		public void init(int n) {
			_n = n;
		}
				
		public abstract int next(); 
	}
	
	public static class RandomIndex extends RoundIndex {
		
		private Random _rand = new Random();

		public int next() {
			if (super._n <= 0) {
				return -1;
			} else {
				return _rand.nextInt(super._n);
			}
		}
	}
	
	public static class AtomIncrementIndex extends RoundIndex {
		private AtomicInteger _i = new AtomicInteger(0);
				
		public int next() {
			int i = _i.getAndIncrement();
			if (i < 0) {
				_i.set(1);
				i = 0;
			}
			return i % super._n;
		}
	}		
	
	public static class RoundRobinArray {		
		
		private static int getGCD(int[] data) {
			int min = data[0];
			int result = 1;
			
			for (int i = 2; i <= min; ++i) {
				boolean flag = true;
			    for (int j : data) {
			    	if (j % i != 0) {
			    		flag = false;
			    	}
			    }
			    if (flag == true) {
			    	result = i;
			    }
			}
			
			return result;			
		}
		
		public static int[] generate(int[] weight) {
			
			int count = 0;
			for (int i : weight) {
				count += i;
			}
			
			int[] result = new int[count];
			
			int i = -1;
			int n = weight.length;
			int cw = 0;
			
			int[] tmp = new int[n];
			System.arraycopy(weight, 0, tmp, 0, n);
			Arrays.sort(tmp);			
			int gcd = getGCD(tmp);
			int maxw = tmp[n-1];
			
			for (int j = 0; j < count; ++j) {
				while (true) {
					i = (i + 1) % n;
					if (i == 0) {
						cw -= gcd;
						if (cw <= 0) {
							cw = maxw;
							if (cw == 0) {
								break;
							}
						}
					}
					
					if (weight[i] >= cw) {
						result[j] = i;
						break;
					}
				}
			}
			
			return result;
		}		
	}
	
}
