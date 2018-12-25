
package com.quakoo.baseFramework.balancer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author liyongbiao
 *
 */
public class CommonBalancerMap {
		
	private Map<Integer, CommonBalancer> _balancers;
	private final Class<?> _clsIndex;
	
	
	public Map<Integer, CommonBalancer> all(){
		return _balancers;
	}

	public CommonBalancerMap(Class<?> clsIndex) {
		_clsIndex = clsIndex;
	}
	
	public void init(Map<Integer, Map<Object, Integer>> items) throws InstantiationException, IllegalAccessException {
		_balancers = new HashMap<Integer, CommonBalancer>();
		
		Iterator<Map.Entry<Integer, Map<Object, Integer>>> it = items.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Map<Object, Integer>> entry = it.next();
			
			CommonBalancer b = new CommonBalancer((Balancing.RoundIndex)_clsIndex.newInstance());
			b.init(entry.getValue());
			
			_balancers.put(entry.getKey(), b);
		}
	}
	
	public Object next(Integer id) {
		CommonBalancer b = _balancers.get(id); 
		return (b != null) ? b.next() : null;
	}
	
	public Object[] next2(int id) {
		CommonBalancer b = _balancers.get(id);
		if (b == null) {
			return null;
		}
				
		Object[] ret = b.next2();
		long idx = (Integer)ret[0];
		ret[0] = ((idx << 16) & 0xFFFF0000L) | (id & 0xFFFFL);
		return ret;		
	}

	public static void main(String[] gwe) throws IllegalAccessException, InstantiationException {
		CommonBalancerMap commonBalancerMap=new CommonBalancerMap(Balancing.RandomIndex.class);
		Map<Integer, Map<Object, Integer>> items=new HashMap<>();
		Map<Object, Integer> map=new HashMap<>();
		map.put("133",1);
		map.put("133",3);
		items.put(1001,map);
		commonBalancerMap.init(items);

		for(int i=0;i<40;i++) {
			System.out.println(commonBalancerMap.next(1001));
		}
	}
	
}
