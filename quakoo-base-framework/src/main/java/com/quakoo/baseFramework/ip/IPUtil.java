package com.quakoo.baseFramework.ip;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 
 * @author liyongbiao
 *
 */
public class IPUtil {
	private static final int IPV4_PART_COUNT = 4;
	
	private static String localIp;

    static {
        Enumeration<NetworkInterface> networkInterface;
        try {
            networkInterface = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
        String ip = null;
        boolean isFound = false;
        while (networkInterface.hasMoreElements()) {
            NetworkInterface ni = networkInterface.nextElement();
            Enumeration<InetAddress> inetAddress = ni.getInetAddresses();
            while (inetAddress.hasMoreElements()) {
                InetAddress ia = inetAddress.nextElement();
                if (ia instanceof Inet6Address) {
                    continue; // ignore ipv6
                }
                if (!ia.isLoopbackAddress() && ia.getHostAddress().indexOf(":") == -1) {
                    ip = ia.getHostAddress();
                    isFound = true;
                    break;
                }
            }
            if (isFound) {
                break;
            }
        }
        localIp = ip;
    }

    public static String getLocalIp() {
        return localIp;
    }
	

	public static boolean isIPv4(String ipStr) {

		for (int i = 0; i < ipStr.length(); i++) {
			char c = ipStr.charAt(i);
			if (c != '.' && Character.digit(c, 16) == -1) {
				return false;
			}
		}

		String[] address = ipStr.split("\\.");
		if (address.length != IPV4_PART_COUNT) {
			return false;
		}

		for (String ipPart : address) {
			int digit;
			try {
				digit = Integer.parseInt(ipPart);
			} catch (NumberFormatException e) {
				return false;
			}
			if (digit < 0 || digit > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
				return false;
			}
		}

		return true;
	}

	public static void main(String[] args) {
		System.out.println(isIPv4("10.10.1.255"));
	}
}
