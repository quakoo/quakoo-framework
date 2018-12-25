package com.quakoo.baseFramework.redis.util;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * 
 * @author LiYongbiao
 *
 */
public class ProcessInfoUtil {
    public static void main(String[] args) throws Exception {
        int pid = getPid();
        String ip = getLocalIp();
        System.out.println("pid: " + pid+",ip="+ip);
        System.in.read(); // block the program so that we can do some probing on it
    }

    public static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName(); // format: "pid@hostname"
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getLocalIp() {
        Enumeration<NetworkInterface> networkInterface;
        try {
            networkInterface = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
        String ip = null;
        while (networkInterface.hasMoreElements()) {
            NetworkInterface ni = networkInterface.nextElement();
            Enumeration<InetAddress> inetAddress = ni.getInetAddresses();
            while (inetAddress.hasMoreElements()) {
                InetAddress ia = inetAddress.nextElement();
                if (ia instanceof Inet6Address)
                    continue; // ignore ipv6
                if (!ia.isLoopbackAddress()
                        && ia.getHostAddress().indexOf(":") == -1 && "127.0.0.1".equals(ia.getHostAddress())==false ) {
                    ip = ia.getHostAddress();
                }
            }
        }
        return ip;
    }
}
