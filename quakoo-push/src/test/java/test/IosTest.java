package test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.common.collect.Maps;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.quakoo.baseFramework.reflect.ClassloadUtil;

public class IosTest {

	public static void main(String[] args) throws IOException {
		InputStream inputStream = ClassloadUtil.getClassLoader()
				.getResourceAsStream("dev_push_jsd.p12");
		ApnsService service =
			    APNS.newService()
			    .withCert(inputStream, "1")
			    .withSandboxDestination().asPool(20).withNoErrorDetection()
			    .build();
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", 1);
		String payload = APNS.newPayload().badge(2).alertBody("Can't be simpler than this!\naaaa").customFields(map)
				.build();
		String token = "63d946d6 b62878c0 6d33b248 95df224d d080b187 395a4977 18418d01 fcba0402";
		service.push(token, payload);
		service.start();
	}
	
}
