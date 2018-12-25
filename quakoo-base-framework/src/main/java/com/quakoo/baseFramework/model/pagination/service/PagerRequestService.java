package com.quakoo.baseFramework.model.pagination.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.baseFramework.model.pagination.PagerSession;
import com.quakoo.baseFramework.model.pagination.PagerUtil;


/**
 * 分页service<br>
 * 此分页只是针对简单的分页逻辑，如果涉及到复杂的业务，如去重等为了防止下一页不出现重复需要使用timeline模式。<br>
 * 
 * @author yongbiaoli
 * 
 */
public abstract class PagerRequestService<T> {

	private static final int defaultRate = 2;

	private static Map<RequestOwner, Integer> requestRate = new HashMap<RequestOwner, Integer>();

	static {
		// 隔一段时间进行重置
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (RequestOwner owner : requestRate.keySet()) {
						requestRate.put(owner, defaultRate);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						Thread.sleep(1000 * 60 * 60 * 24);
					} catch (Exception e) {
					}
				}

			}
		}).start();
	}

	private Pager pager;
	private int index;
	private boolean include = true;

	/**
	 * 
	 * @param pager
	 * @param index
	 *            一个方法一个index 确保唯一
	 */
	public PagerRequestService(Pager pager, int index) {
		this.pager = pager;
		this.index = index;
	}
	
	public PagerRequestService(Pager pager, int index, boolean include) {
		this.pager = pager;
		this.index = index;
		this.include = include;
	}

	/**
	 * 步骤一：获取分页结果
	 * 
	 * @return
	 */
	public abstract List<T> step1GetPageResult(String cursor, int size)
			throws Exception;

	/**
	 * 步骤二:：获取总数
	 * 
	 * @return
	 */
	public abstract int step2GetTotalCount() throws Exception;

	/**
	 * 步骤三：对数据进行过滤,过滤逻辑不支持涉及跨页
	 * 
	 * @return
	 */
	public abstract List<T> step3FilterResult(List<T> unTransformDatas,
			PagerSession session) throws Exception;

	/**
	 * 步骤四：对结果进行转换。
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract List<?> step4TransformData(List<T> unTransformDatas,
			PagerSession session) throws Exception;



	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Pager getPager() throws Exception {
		PagerSession session = new PagerSession();
		RequestOwner requestOwner = new RequestOwner(this.getClass(), index);
		Integer rate = requestRate.get(requestOwner);
		if (rate == null) {
			rate = 2;
			requestRate.put(requestOwner, rate);
		}
		List unTransformDatas = null;
		int size = pager.getSize();
		if(include){
			if(size == Integer.MAX_VALUE)
				size--;
			size++;
		}
		boolean noMoreData = false;
		for (;;) {
			unTransformDatas = step1GetPageResult(pager.getCursor(),
					size * rate);
			if (unTransformDatas.size() < size * rate) {
				noMoreData = true;
			}
			unTransformDatas = step3FilterResult(unTransformDatas, session);
			if (noMoreData || unTransformDatas.size() > size ) {
				break;
			} else {
				rate = rate + 1;
				requestRate.put(requestOwner, rate);
			}
		}
        boolean has = false;
        String preCursor = "0";
        String nextCursor = "0";
        size = pager.getSize();
        if (include) {
            if (size == Integer.MAX_VALUE)
                size--;
            size++;
        }
        int sign = 0;
        if (include && unTransformDatas.size() >= size) {
            has = true;
            sign = size;
        } else if (!include && unTransformDatas.size() > size) {
            has = true;
            sign = size;
        } else {
            sign = unTransformDatas.size();
        }
        List data = PagerUtil.sub(unTransformDatas, 0, sign);
        if (data != null && data.size() > 0) {
            Object first = data.get(0);
            preCursor = PagerUtil.getCursor(first);
            Object last = data.get(data.size() - 1);
            nextCursor = PagerUtil.getCursor(last);
        }
        if (include && has)
            data = PagerUtil.sub(data, 0, sign - 1);
        pager.setData(data);
        if (has)
            pager.setNextCursor(nextCursor);
        pager.setPreCursor(preCursor);
        pager.setCount(data.size());
        if (has) {
            pager.setHasnext(has);
        }
        pager.setTotalCount(step2GetTotalCount());
        pager.setData(step4TransformData(pager.getData(), session));
        return pager;
	}


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Pager getPager(String sortName) throws Exception {
        PagerSession session = new PagerSession();
        RequestOwner requestOwner = new RequestOwner(this.getClass(), index);
        Integer rate = requestRate.get(requestOwner);
        if (rate == null) {
            rate = 2;
            requestRate.put(requestOwner, rate);
        }
        List unTransformDatas = null;
        int size = pager.getSize();
        if(include){
            if(size == Integer.MAX_VALUE)
                size--;
            size++;
        }
        boolean noMoreData = false;
        for (;;) {
            unTransformDatas = step1GetPageResult(pager.getCursor(),
                    size * rate);
            if (unTransformDatas.size() < size * rate) {
                noMoreData = true;
            }
            unTransformDatas = step3FilterResult(unTransformDatas, session);
            if (noMoreData || unTransformDatas.size() > size ) {
                break;
            } else {
                rate = rate + 1;
                requestRate.put(requestOwner, rate);
            }
        }
        boolean has = false;
        String preCursor = "0";
        String nextCursor = "0";
        size = pager.getSize();
        if (include) {
            if (size == Integer.MAX_VALUE)
                size--;
            size++;
        }
        int sign = 0;
        if (include && unTransformDatas.size() >= size) {
            has = true;
            sign = size;
        } else if (!include && unTransformDatas.size() > size) {
            has = true;
            sign = size;
        } else {
            sign = unTransformDatas.size();
        }
        List data = PagerUtil.sub(unTransformDatas, 0, sign);
        if (data != null && data.size() > 0) {
            Object first = data.get(0);
            preCursor = PagerUtil.getCursor(first, sortName);
            Object last = data.get(data.size() - 1);
            nextCursor = PagerUtil.getCursor(last, sortName);
        }
        if (include && has)
            data = PagerUtil.sub(data, 0, sign - 1);
        pager.setData(data);
        if (has)
            pager.setNextCursor(nextCursor);
        pager.setPreCursor(preCursor);
        pager.setCount(data.size());
        if (has) {
            pager.setHasnext(has);
        }
        pager.setTotalCount(step2GetTotalCount());
        pager.setData(step4TransformData(pager.getData(), session));
        return pager;
    }

}
