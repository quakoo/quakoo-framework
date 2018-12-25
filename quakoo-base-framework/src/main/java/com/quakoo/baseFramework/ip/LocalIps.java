package com.quakoo.baseFramework.ip;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author LiYongbiao1
 *
 */
public class LocalIps {
    /**
     * Logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(LocalIps.class);

    private static String INNER_IP;

    private static String OUTER_IP;

    private static String CLUSTER_ID;
    
    private static List<String> allIps=new ArrayList<String>();

    private static final String UNKNOW_IP = "999.999.999.999";

    // 192.168.*.*
    private static final Pattern INNER_IP_PATTERN = Pattern.compile("192[.]168[.]\\d+[.]\\d+");
    private static final Pattern INNER_IP_PATTERN1 = Pattern.compile("10[.]\\d+[.]\\d+[.]\\d+");
    private static final Pattern INNER_IP_PATTERN2 = Pattern.compile("172[.]1[6-9][.]\\d+[.]\\d+");
    private static final Pattern INNER_IP_PATTERN3 = Pattern.compile("172[.]2[0-9][.]\\d+[.]\\d+");
    private static final Pattern INNER_IP_PATTERN4 = Pattern.compile("172[.]3[0-1][.]\\d+[.]\\d+");

    // 127.0.0.1
    private static final Pattern LOCALHOST_IP_PATTERN = Pattern.compile("127[.]0[.]0[.]1");

    static {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface one = interfaces.nextElement();
                final Enumeration<InetAddress> inetAddresses = one.getInetAddresses();        
                while (inetAddresses.hasMoreElements()) {
                    final InetAddress oneAddr = inetAddresses.nextElement();
                    if (oneAddr instanceof Inet6Address) {
                        // jump over ipv6
                        continue;
                    }
                    final String ip = oneAddr.getHostAddress();
                    allIps.add(ip);
                    // if 192.168
                    if (INNER_IP_PATTERN.matcher(ip).matches()
                    		||INNER_IP_PATTERN1.matcher(ip).matches()
                    		||INNER_IP_PATTERN2.matcher(ip).matches()
                    		||INNER_IP_PATTERN3.matcher(ip).matches()
                    		||INNER_IP_PATTERN4.matcher(ip).matches()) {
                        INNER_IP = ip;
                    }
                    // and not 127.0.0.1
                    else if (!LOCALHOST_IP_PATTERN.matcher(ip).matches()) {
                        OUTER_IP = ip;
                    }

                }
            }
            if (INNER_IP == null) {
                INNER_IP = UNKNOW_IP;
            }
            if (OUTER_IP == null) {
                OUTER_IP = UNKNOW_IP;
            }

            // initial CLUSTER_ID
            final String[] pieces = INNER_IP.split("\\.");
            CLUSTER_ID = pieces[2] + "_" + pieces[3];
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static String getInnerIp() {
        return INNER_IP;
    }

    public static String getOuterIp() {
        return OUTER_IP;
    }

    public static String getClusterId() {
        return CLUSTER_ID;
    }
    
    public static List<String> getAllIpds(){
    	return allIps;
    }
    
    public static String getIp(String start){
    	for(String ip :allIps){
    		if(ip.startsWith(start)){
    			return ip;
    		}
    	}
    	return INNER_IP;
    }

    public static void main(String[] args) {
        System.out.println(LocalIps.INNER_IP);
        System.out.println(LocalIps.OUTER_IP);
        System.out.println(LocalIps.CLUSTER_ID);
    }
}
