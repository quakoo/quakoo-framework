package test;

import com.quakoo.framework.ext.recommend.service.ext.RealTimeSearchAbstractService;

public class RealTimeSearchServiceImpl extends RealTimeSearchAbstractService {

    @Override
    public String getSearchIndex() {
        return "article";
    }

    @Override
    public String getSearchColumn() {
        return "title";
    }

    @Override
    public String getSearchTime() {
        return "lastUpdateTime";
    }

}
