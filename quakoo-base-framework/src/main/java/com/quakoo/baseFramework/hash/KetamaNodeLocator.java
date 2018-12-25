package com.quakoo.baseFramework.hash;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


public final class KetamaNodeLocator<T> {
	
	private TreeMap<Long, T> ketamaNodes;
	private HashAlgorithm hashAlg;
	private int numReps = 160;
	
    public KetamaNodeLocator(List<T> nodes, HashAlgorithm alg, int nodeCopies) {
		hashAlg = alg;
		ketamaNodes=new TreeMap<Long, T>();
		
        numReps= nodeCopies;
        
		for (T node : nodes) {
			for (int i = 0; i < numReps / 4; i++) {
				byte[] digest = hashAlg.computeMd5(node.toString() + i);
				for(int h = 0; h < 4; h++) {
					long m = hashAlg.hash(digest, h);
					
					ketamaNodes.put(m, node);
				}
			}
		}
    }

	public T getPrimary(final String k) {
		byte[] digest = hashAlg.computeMd5(k);
		T rv=getNodeForKey(hashAlg.hash(digest, 0));
		return rv;
	}

	T getNodeForKey(long hash) {
		final T rv;
		Long key = hash;
		if(!ketamaNodes.containsKey(key)) {
			SortedMap<Long, T> tailMap=ketamaNodes.tailMap(key);
			if(tailMap.isEmpty()) {
				key=ketamaNodes.firstKey();
			} else {
				key=tailMap.firstKey();
			}
			//For JDK1.6 version
//			key = ketamaNodes.ceilingKey(key);
//			if (key == null) {
//				key = ketamaNodes.firstKey();
//			}
		}
		
		
		rv=ketamaNodes.get(key);
		return rv;
	}
}
