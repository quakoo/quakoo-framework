package test;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.model.MessageChat;

public class TheadTest {

	public static void main(String[] args) throws InterruptedException {
//		Thread a = new Thread(new TestThread());
//		
//		a.start();
//		Thread.sleep(10000);
//		a.interrupt();
//		
//		
//		Thread.sleep(Long.MAX_VALUE);
		
		String str = "<img class=\"commonImg\" src=\"http://store.quakoo.com/storage/youhuipai/498*664*13b8a33cd72d20bb7c456dda892c6f2b_200_0.jpeg\" width=\"180px\" height=\"240px\" real=\"http://store.quakoo.com/storage/youhuipai/498*664*13b8a33cd72d20bb7c456dda892c6f2b.jpeg\">";
		MessageChat messageChat = new MessageChat(str, null, null, null, null, null, null);
		String jsonStr = JsonUtils.toJson(messageChat);
		System.out.println(jsonStr);
		MessageChat chat = JsonUtils.fromJson(jsonStr, MessageChat.class);
		System.out.println(chat.toString());
	}
}

class TestThread implements Runnable {

	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("AAAAA");
		}
	}
	
}