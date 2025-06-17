package net.chamman.moonnight.global.interceptor;

/**
 * 현재 요청 스레드에 대한 클라이언트의 IP 주소를 보관하는 클래스.
 * ThreadLocal을 사용하여 각 스레드가 자신만의 IP 주소 복사본을 갖도록 보장한다.
 */
public class IpAddressContextHolder {

    // 각 스레드마다 독립적인 저장 공간을 제공하는 ThreadLocal
    private static final ThreadLocal<String> ipContextHolder = new ThreadLocal<>();

    /**
     * 현재 스레드의 컨텍스트에 IP 주소를 설정한다.
     * @param ip 클라이언트의 IP 주소
     */
    public static void setIpAddress(String ip) {
        ipContextHolder.set(ip);
    }

    /**
     * 현재 스레드의 컨텍스트에서 IP 주소를 가져온다.
     * @return 저장된 IP 주소, 없으면 null
     */
    public static String getIpAddress() {
        return ipContextHolder.get();
    }

    /**
     * 현재 스레드의 컨텍스트를 비워서 메모리 누수를 방지한다.
     * 요청 처리가 완료된 후 반드시 호출해야 한다.
     */
    public static void clear() {
        ipContextHolder.remove();
    }
}