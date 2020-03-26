package test;

import com.quakoo.framework.ext.recommend.bean.SearchRes;
import com.quakoo.framework.ext.recommend.service.ext.RealTimeSearchAbstractService;

import java.util.List;

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

    @Override
    public int getSearchSize() {
        return 2500;
    }

    @Override
    public List<String> getSearchResColumns() {
        return null;
    }

    @Override
    public void handleFilter(List<SearchRes> list, long uid) {

    }

    @Override
    public long getGaussTimeStep() {
        return 0;
    }

    @Override
    public List<String> getDelIds(List<SearchRes> list) {
        return null;
    }
}
