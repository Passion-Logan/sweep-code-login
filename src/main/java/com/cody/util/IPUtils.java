package com.cody.util;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * ClassName: IPUtils
 *
 * @author WQL
 * @Description:
 * @date: 2020/4/8 21:09
 * @since JDK 1.8
 */
public class IPUtils {

    public static final String DEFAULT_IP = "127.0.0.1";

    /**
     * 根据第一个网卡地址作为其内网ipv4地址，避免返回127.0.0.1
     *
     * @return
     */
    public static String getLocalIpByNetcard() {
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface item = e.nextElement();
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (item.isLoopback() || !item.isUp()) {
                        continue;
                    }
                    if (address.getAddress() instanceof Inet4Address) {
                        Inet4Address inet4Address = (Inet4Address) address.getAddress();
                        return inet4Address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            return DEFAULT_IP;
        }

        return DEFAULT_IP;
    }

    private static volatile String ip;

    public static String getLocalIP() {
        if (ip == null) {
            synchronized (IPUtils.class) {
                ip = getLocalIpByNetcard();
            }
        }
        return ip;
    }

}
