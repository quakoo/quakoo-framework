package com.quakoo.framework.ext.push.service;



import com.quakoo.framework.ext.push.model.PushMsg;

import java.util.List;

public interface PushMsgService {

    public long createId();

    public void accept(PushMsg pushMsg);

    public void finish(PushMsg pushMsg);

    public void finish(List<PushMsg> pushMsgs);

}
