
package com.quakoo.baseFramework.balancer;

import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author liyongbiao
 *
 */
public class CommonBalancer {
	
	private Object[] _items;
	private final Balancing.RoundIndex _index;
	
	public CommonBalancer(Balancing.RoundIndex index) {
		_index = index;
	}
	


	public void init(Map<Object, Integer> items) {
		
		Object[] keys = new Object[items.size()];
		int[] weights = new int[items.size()];
		
		Iterator<Map.Entry<Object, Integer>> it = items.entrySet().iterator();		
		int i = 0;
		while (it.hasNext()) {
			Map.Entry<Object, Integer> e = it.next();
			keys[i] = e.getKey();
			weights[i] = e.getValue();
			++i;
		}
		
		int[] rra = Balancing.RoundRobinArray.generate(weights);
		_items = new Object[rra.length];
		
		i = 0;
		for (int idx : rra) {
			_items[i++] = keys[idx]; 
		}
		
		_index.init(_items.length);		
	}
	
	public Object next() {
		int index = _index.next();
		if (index < 0) {
			return null;
		} else {
			return _items[_index.next()];
		}
	}
	
	public Object[] next2() {
		int idx = _index.next();
		return new Object[] {
			idx,
			_items[idx]
		};
	}
	
}
