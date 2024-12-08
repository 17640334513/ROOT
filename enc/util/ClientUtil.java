package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import uf.UF;
import uf.UFMap;

public class ClientUtil {		
	
	static {
		System.setProperty("sun.net.client.defaultConnectTimeout", "3000");
		System.setProperty("sun.net.client.defaultReadTimeout", "3000");
	}
	
	/**
	 * HTTP协议发送报文(UTF-8)
	 * @param url : http请求地址
	 * @param msg : http请求报文(a=1&b=2&c=3)
	 * @param requestMethod : http请求方式(POST或者GET)
	 * */
	public static String sendHttp(String url,String msg,String requestMethod) throws Exception{
		return sendHttp(url, msg, requestMethod, UF.UTF8);
	}
	/**
	 * HTTP协议发送报文
	 * @param url : http请求地址
	 * @param msg : http请求报文(a=1&b=2&c=3)
	 * @param requestMethod : http请求方式(POST或者GET)
	 * @param charset : 报文编码
	 * */
	public static String sendHttp(String url, String msg, String requestMethod, String charset) throws Exception{
		LogUtil.print("HTTP请求路径：" + url, ClientUtil.class);
		LogUtil.print("HTTP请求报文：" + msg, ClientUtil.class);
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod(requestMethod);
		conn.setDoOutput(true);//如果通过post提交数据，必须设置允许对外输出数据    
        try (PrintWriter pw = new PrintWriter(conn.getOutputStream())) {
        	pw.write(msg);//发送报文 
            pw.flush();
        }     
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset))) {
        	StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String ret = sb.toString();
            LogUtil.print("HTTP接收到的返回报文：" + ret, ClientUtil.class);
    		return ret;
        }		
	}	
	
	/**
	 * HTTPS发送报文(UTF-8)
	 * @param url : https请求地址
	 * @param msg : https请求报文(a=1&b=2&c=3)
	 * @param requestMethod : https请求方式(POST或者GET)
	 * */
	public static String sendHttps(String url,String msg,String requestMethod) throws Exception{
		return sendHttps(url, msg, requestMethod, UF.UTF8);
	}
	/**
	 * HTTPS发送报文
	 * @param url : https请求地址
	 * @param msg : https请求报文(a=1&b=2&c=3)
	 * @param requestMethod : https请求方式(POST或者GET)
	 * @param charset : 报文编码
	 * */
	public static String sendHttps(String url, String msg, String requestMethod, String charset) throws Exception{
		LogUtil.print("HTTPS请求路径：" + url, ClientUtil.class);
		LogUtil.print("HTTPS请求报文：" + msg, ClientUtil.class);
		HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/4.0");
		conn.setRequestMethod(requestMethod);
		conn.setDoOutput(true);//如果通过post提交数据，必须设置允许对外输出数据
        try (PrintWriter pw = new PrintWriter(conn.getOutputStream())) {
        	pw.write(msg);//发送报文 
            pw.flush();
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset))) {
        	StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String ret = sb.toString();
            LogUtil.print("HTTPS接收到的返回报文：" + ret, ClientUtil.class);
    		return ret;
        }		
	}
	
	/**
	 * SOCKET发送报文(UTF-8)
	 * @param ip : 目标IP
	 * @param port : 目标端口号
	 * @param msg : 报文
	 * */
	public static String sendSocket(String ip,int port,String msg) throws Exception{
		return sendSocket(ip, port, msg, UF.UTF8);
	}
	/**
	 * SOCKET发送报文
	 * @param ip : 目标IP
	 * @param port : 目标端口号
	 * @param msg : 报文
	 * @param charset : 报文编码
	 * */
	public static String sendSocket(String ip,int port,String msg,String charset) throws Exception{
		LogUtil.print("socket请求路径：" + ip + ":" + port, ClientUtil.class);
		LogUtil.print("socket请求报文：" + msg, ClientUtil.class);
		try (Socket socket = new Socket(ip, port)) {
			socket.setSoTimeout(5000);
			OutputStream os = socket.getOutputStream();
			os.write(msg.getBytes(charset));
			os.flush();
			InputStream is = socket.getInputStream();
			byte[] c = new byte[1024];
			String ret = "";
            while (is.read(c) != -1) {
            	ret += new String(c, charset);
                c = new byte[1024];
            }
            LogUtil.print("socket接收到的返回报文：" + ret, ClientUtil.class);
            return ret.replaceAll("[\\x00-\\x08\\x0b-\\x0c\\x0e-\\x1f]", "");//去掉非ASCII字符
		}	
	}
	
	/**爬取addr网页的代码*/
	public static List<String> crawler(String addr) throws Exception {
		return crawler(addr, null);
	}
	/**爬取addr网页的代码*/
	public static List<String> crawler(String addr, Map<String, String> requestMap) throws Exception {
		return crawler(addr, requestMap, true);
	}
	/**爬取addr网页的代码*/
	public static List<String> crawler(String addr, Map<String, String> requestMap, boolean isPrint) throws Exception {
		Util.delay(1000);//防止过度频繁爬取网站导致被网站封IP
		if(isPrint) LogUtil.print("爬虫开始工作，addr：" + addr + "，requestMap：" + requestMap, ClientUtil.class);
		List<String> retList = new ArrayList<>();
		HttpURLConnection urlConnection = (HttpURLConnection) new URL(addr).openConnection();
		if(requestMap != null){
			//requestMap.put("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0");
			requestMap.put("User-Agent", PropUtil.get("REDEEM_USER_AGENT"));
			requestMap.put("X-Forwarded-For", getRandomIp());
			requestMap.forEach((key, value) -> {
				urlConnection.setRequestProperty(key, value);
			});
		}
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"))){
			String line;
	        while ((line = reader.readLine()) != null) {
	        	retList.add(line);
	        }
		}finally {
			urlConnection.disconnect();
		}
		if(isPrint) LogUtil.print("爬取到的代码：" + retList, ClientUtil.class);
		return retList;
	}
	
	/**生成随机IP地址(0.0.0.0 ~ 255.255.255.255)*/
    public static String getRandomIp() {
    	Random random = new Random();
    	String ret = random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
    	if("49.233.15.170".equals(ret)) {
    		ret = "48.232.14.169";
    	}
        return ret;
    }
    
	public static void main(String[] args) throws Exception {
		String json = sendHttps("https://www.jjmmw.com/fund/ajax/get_fund_his_nav/", "fundcode=320007&beginDate=2009-04-16&endDate=2009-12-04", "POST");
		Map<String, List<Map<String, String>>> jsonMap = JsonUtil.parse(json);
		List<Map<String, String>> navList = jsonMap.get("his_nav_list");
		List<UFMap> dataList = new ArrayList<>();
		BigDecimal lastValue = BigDecimal.ONE;
		for(int i = navList.size() - 1; i >= 0; i--) {
			Map<String, String> navMap = navList.get(i);
			String rate = navMap.get("growth_rate");
			if("--".equals(rate)) rate = "0.00";
			UFMap saveMap = new UFMap().add("DT", navMap.get("tradedate_display2")).add("RATE", rate);
			lastValue = new BigDecimal(rate).divide(UF.DECIMAL100).add(BigDecimal.ONE).multiply(lastValue).setScale(4, RoundingMode.HALF_UP);
			dataList.add(saveMap.add("VALUE", lastValue));
			System.out.println(saveMap);
		}
	}
	
}
