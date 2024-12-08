package action;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uf.UF;
import uf.UFMap;
import util.ClientUtil;
import util.DateUtil;
import util.JsonUtil;
import util.LogUtil;
import util.PropUtil;
import util.StringUtil;
import util.Util;

@SuppressWarnings("rawtypes")
public class Common {
	
	public static final String PERIOD_MONTH = "month";
	public static final String PERIOD_WEEK = "week";
	public static final String PERIOD_DAY = "day";
	public static final String PERIOD_120M = "120m";
	public static final int IGNORE_DAYS = 120;
	public static final BigDecimal ZERO_POINT_SEVEN = new BigDecimal("0.7");
	
	private static HttpURLConnection xueqiuConn;
	private static String cookie = "cookiesu=811720961262687; device_id=8559a78fe9de3d1e70fce8dfe3f07d67; smidV2=202407142047430e7b7bc8bb64afaf3816fde489e59517004409109ad750f50; s=bo1oifdtgj; .thumbcache_f24b8bbe5a5934237bbc0eda20c1b6e7=mEFB+ys79M3pqFrk1hIEysosnIYY5lTGWvEBZ5wRgXTSFqzvWj1Sb13OjIZa1/e20eljKGUG7OSDpbYburIimA%3D%3D; acw_tc=2760827217334869383891481e713ccface98a38777749a1e76c713c2e1c3e; xq_a_token=220b0abef0fac476d076c9f7a3938b7edac35f48; xqat=220b0abef0fac476d076c9f7a3938b7edac35f48; xq_r_token=a57f65f14670a8897031b7c4f10ea42a50894850; xq_id_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1aWQiOi0xLCJpc3MiOiJ1YyIsImV4cCI6MTczNTY5Mjg4OSwiY3RtIjoxNzMzNDg2ODg5NjA5LCJjaWQiOiJkOWQwbjRBWnVwIn0.c872_0RjW5oVWA74HceUXkAyf6bVu_Ky8vBmRts9C3SZ8IAEGgvn9hTvXKLcmYVMOXUx6QjvWMzTk1tBJhns8_DBpWdmmXOS7CSgBGJduMUd5joowIBneT_ZggpiuaSHMaG42S-Y2JJK-Sbut4TM445ONHA4H5s3hNyEalFQbil2rFqgH0jhJ9iChm1B2mNWNqavtLcoONr4XIWq26MQgHA9N590UquDKyUTBAWsJo2PJGrl7v5bEjw_CDlAOXcM5hls5gHBJkMFTEo4n6SX10GoKmwwgj0zOSktmPKdNtUHU2YBghj49ppgaXkiwB3TCGv29w_g9ZaAeoDt1qUHjg; u=811720961262687; Hm_lvt_1db88642e346389874251b5a1eded6e3=1733021986,1733139951,1733318435,1733486939; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1733486939; HMACCOUNT=10ECCE78CF78523E; ssxmod_itna=eqIx9GDtoYuDyDl+xCxDqpPhe4GIeqGCYbYKb+3yDBkDAIYDZDiqAPGhDC3RKn8+PKXjlSPd+e9l0RqHFQBC0YrjegODdd8yex0aDbqGkenWt4GGnxBYDQxAYDGDDPDooKD1D3qDkD7oUM9wXgDi3DbotDf4DmDGABFqDgDYQDGwK0D7QDIwtdDDNdKc4nz7he1eQocTTlY02xKYtDjwTD/Spk6Ytd2aHdDnr7pZiFFqGyiKGuBTXDPTT3AT0/tC4N7hxN+DYKbgA4ig3dtGkKQ0+iYBhRDSPn583hS4AxSKDAtAMoeD; ssxmod_itna2=eqIx9GDtoYuDyDl+xCxDqpPhe4GIeqGCYbYKb+3x8kpoODGNweNGa3Qjsp5DfxyDkNlPTm6k5+xOA=4F+2Gh6etseuurKeC41GpEzD/CIUqFwa8viRX0a6ZWKFSKPYT4HOCyQW4T5BA9B4XeF+tB34GFSi5P5Gx1ireN+g2xCeev70bqz0KPwCTdoBAigSNdy4WQ5Z2fzeatseAd1ZKP5kFC=kFUzCfWanrrPKy4KpU5EuqOWKi4FQ7HXqUOPfHdP+tqfWA5TfbCZGUvFxG2G4GcDiQXj05deD==";
	private static Map<String, String> requestMap;
	
	/**
	 * resultIndex: 决定这一行是红色还是绿色的columns下标
	 * resultCompare: 决定这一行是红色还是绿色的column比resultCompare小则绿色
	 * */
	public static StringBuilder htmlTable(List<String[]> list, int resultIndex, Object resultCompare) {
		StringBuilder ret = new StringBuilder();
		ret.append("<table border='1'>");
		String[] ths = list.get(0);
		ret.append("<tr>");
		for(String th : ths) {
			ret.append("<td>").append(th).append("</td>");
		}
		ret.append("</tr>");
		for(int i = 1; i < list.size(); i ++) {
			String[] columns = list.get(i);
			String color = "black";
			if(resultCompare != null) {
				String result = columns[resultIndex];
				if(resultCompare instanceof BigDecimal) {
					if(result.endsWith("%")) {
						result = result.substring(0, result.length() - 1);
					}
					BigDecimal value = new BigDecimal(result);
					if(value.compareTo((BigDecimal) resultCompare) == 1) {
						color = "red";
					}else if(value.compareTo((BigDecimal) resultCompare) == -1) {
						color = "green";
					}
				}else if(resultCompare instanceof Integer){
					if(Integer.parseInt(result) < (Integer) resultCompare) {
						color = "red";
					}else {
						color = "green";
					}
				}else {
					if(result.startsWith((String) resultCompare)) {
						color = "red";
					}
				}
			}
			ret.append("<tr style=\"color:" + color + "\">");
			for(String column : columns) {
				ret.append("<td>").append(column).append("</td>");
			}
			ret.append("</tr>");
		}
		ret.append("</table>");
		return ret;
	}
	
	/**获取雪球网爬虫的conn*/
	public static void getXueqiuConn() throws Exception {
		//String cookie = getCookie();
		LogUtil.print("Set-Cookie: " + cookie);
		String acw_tc = "acw_tc=" + StringUtil.substring(cookie, " acw_tc=", ";");
		String xq_a_token = "; xq_a_token=" + StringUtil.substring(cookie, " xq_a_token=", ";");
		String xqat = "; xqat=" + StringUtil.substring(cookie, " xqat=", ";");
		String xq_r_token = "; xq_r_token=" + StringUtil.substring(cookie, " xq_r_token=", ";");
		String xq_id_token = "; xq_id_token=" + StringUtil.substring(cookie, " xq_id_token=", ";");
		String u = "; u=" + StringUtil.substring(cookie, " u=", ";");
		requestMap = new HashMap<String, String>();
		requestMap.put("cookie", acw_tc + xq_a_token + xqat + xq_r_token + xq_id_token + u);
		requestMap.put("Host", "stock.xueqiu.com");
		requestMap.put("Upgrade-Insecure-Requests", "1");
		requestMap.put("Connection", "keep-alive");
		requestMap.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
		requestMap.put("Referer", PropUtil.get("XUEQIU_ADDR"));
	}
	
	private static String getCookie() throws Exception {
		for(int i = 0; i < 10; i ++) {
			try {
				URL url = new URL(PropUtil.get("XUEQIU_ADDR"));
				xueqiuConn = (HttpURLConnection) url.openConnection();
				return xueqiuConn.getHeaderFields().get("Set-Cookie").toString();
			} catch (Exception e) {
				LogUtil.print(e);
				Util.delay(1000);
			}
		}
		throw new Exception("10次获取cookie都失败了");
	}
	
	/**爬虫获取雪球网的数据列表*/
	private static List getItemList(String kAddr) throws Exception {
		List<String> lineList = ClientUtil.crawler(kAddr, requestMap, false);
		try {
			String line = lineList.get(0);
			Map lineMap = JsonUtil.parse(line);
			Map dataMap = (Map) lineMap.get("data");
			return (List) dataMap.get("item");
		}catch (Exception e) {
			LogUtil.print(lineList);
			throw e;
		}
	}
	
	/**
	 * 获取code标的历史数据(后复权)
	 * @param code: 个股需要带前缀，基债不需要; period: 参见本类静态常量
	 * */
	public static List<UFMap> getDataList(String code, String period) throws Exception {
		if(requestMap == null) {
			getXueqiuConn();
		}
		List<UFMap> dateList = new ArrayList<>();
		String mills = System.currentTimeMillis() + "";
		while(true) {
			String kAddr = PropUtil.get("K_ADDR").replace("PERIOD", period).replace("NOW", mills).replace("COUNT", "5000");
			List itemList = getItemList(kAddr.replace("ID", addIndexOrFundOrBondBourse(code)));
			if(itemList.get(0) instanceof String) {
				break;
			}
			for(Object obj : itemList) {
				List list = (List) obj;
				Date dt = new Date(Long.parseLong(list.get(0) + ""));
				long volumn = Long.parseLong(list.get(1) + "");//交易量
				BigDecimal open = new BigDecimal(list.get(2) + "");
				BigDecimal high = new BigDecimal(list.get(3) + "");
				BigDecimal low = new BigDecimal(list.get(4) + "");
				BigDecimal close = new BigDecimal(list.get(5) + "");
				if(close.compareTo(UF.DECIMAL0) == 0) {
					continue;
				}
				BigDecimal rate = new BigDecimal(list.get(7) + "");
				dateList.add(new UFMap().add("dt", dt).add("volumn", volumn).add("open", open).add("high", high).add("low", low).add("close", close).add("rate", rate));
			}
			if(itemList.size() < 5000) {
				break;
			}
			mills = Long.parseLong(((List) itemList.get(0)).get(0).toString()) - 60000 + "";
		}
		dateList.sort((map1, map2) -> {
			Date dt1 = map1.getT("dt");
			Date dt2 = map2.getT("dt");
			return dt1.compareTo(dt2);
		});
		return dateList;
	}
	
	/**例如：把002455变成SZ002455*/
	public static String addStockBourse(String code) {
		if(code.startsWith("6")) return "SH" + code;
		return "SZ" + code;
	}
	
	/**例如：把159980变成SZ159980*/
	public static String addIndexOrFundOrBondBourse(String code) {
		if(Util.isEmpty(code)) return null;
		if(code.length() != 6) return code;
		if(code.startsWith("0") || code.startsWith("5") || code.startsWith("11")) return "SH" + code;
		if(code.startsWith("H") || code.startsWith("93") || code.startsWith("95") || code.startsWith("C") || code.startsWith("7")) return "CSI" + code;
		return "SZ" + code;
	}
	
	/**获取场外基金的历史净值*/
	public static List<UFMap> getDataList(String code) throws Exception{
		String json = ClientUtil.sendHttps(PropUtil.get("FUND_HIS_ADDR"), PropUtil.get("FUND_HIS_PARAM").replace("ID", code).replace("SDATE", "2000-01-01").replace("EDATE", DateUtil.now10()), "POST");
		Map<String, List<Map<String, String>>> jsonMap = JsonUtil.parse(json);
		List<Map<String, String>> navList = jsonMap.get("his_nav_list");
		List<UFMap> dataList = new ArrayList<>();
		BigDecimal lastValue = BigDecimal.ONE;
		for(int i = navList.size() - 1; i >= 0; i--) {
			Map<String, String> navMap = navList.get(i);
			String rate = navMap.get("growth_rate");
			if("--".equals(rate)) rate = "0.00";
			UFMap saveMap = new UFMap().add("DT", LocalDate.parse(navMap.get("tradedate_display2"))).add("RATE", new BigDecimal(rate));
			lastValue = new BigDecimal(rate).divide(UF.DECIMAL100).add(BigDecimal.ONE).multiply(lastValue).setScale(4, RoundingMode.HALF_UP);
			dataList.add(saveMap.add("VALUE", lastValue));
		}
		return dataList;
	}
	
	/**
	 * 计算dataList中下标为index那天收盘后前days个交易日的累计涨幅
	 * @return 例: 1.08表示涨幅为8个点
	 * */
	public static BigDecimal calRise(List<UFMap> dataList, int index, int days) {
		BigDecimal now = dataList.get(index).getT("close");
		BigDecimal ago = dataList.get(index - days).getT("close");
		return now.divide(ago, 4, RoundingMode.HALF_UP);
	}
	
	/**计算dataList一直持有的累计收益*/
	public static BigDecimal calHistory(List<UFMap> dataList) {
		int index = dataList.size() - 1;
		return calRise(dataList, index, index);
	}
	
	/**计算全球轮动法最优解的回测结果*/
	public static UFMap chaseUpBest(List<UFMap> dataList, BigDecimal buy, BigDecimal sell, int days) {
		String status = "NONE";
		Boolean isHold = null;
		BigDecimal expect = BigDecimal.ONE;
		Date lastDealDate = null;
		BigDecimal lastValue = null, rise = null, minRise = UF.DECIMAL1;//最大回撤
		int buyCount = 0, upCount = 0, size = dataList.size(), buyIndex = IGNORE_DAYS, maxDays = 0;//最多坐过山车天数
		for(int i = IGNORE_DAYS; i < size; i ++) {
			UFMap dataMap = dataList.get(i);
			BigDecimal recentRise = Common.calRise(dataList, i, days);
			boolean isLast = i == size - 1;
			if(isHold == null) {
				boolean buyToday = recentRise.compareTo(buy) >= 0 && (isLast || !cannotBuy(dataList, i + 1));
				if(!buyToday) {
					isHold = false;
				}
			}else if(!isHold) {
				if("AWAY".equals(status)) {
					lastDealDate = dataList.get(i - 1).getT("dt");
				}
				status = "NONE";
				boolean buyToday = recentRise.compareTo(buy) >= 0 && (isLast || !cannotBuy(dataList, i + 1));
				if(buyToday) {
					isHold = true;
					status = "GOGO";
					buyCount ++;
					buyIndex = i;
					expect = expect.subtract(expect.divide(UF.DECIMAL10000));
				}
			}else {
				if("GOGO".equals(status)) {
					lastDealDate = dataList.get(i - 1).getT("dt");
					lastValue = dataMap.getT("open");
				}
				status = "HOLD";
				BigDecimal close = dataMap.getT("close");
				rise = close.divide(lastValue, 4, RoundingMode.HALF_UP);
				if(rise.compareTo(UF.DECIMAL1) <= 0) {
					int tieUpDays = i - buyIndex;
					if(maxDays < tieUpDays) {
						maxDays = tieUpDays;
					}
					if(rise.compareTo(minRise) == -1) {
						minRise = rise;
					}
				}
				boolean sellToday = recentRise.compareTo(sell) <= 0 && (isLast || !cannotSell(dataList, i + 1));
				if(sellToday || isLast) {
					if(!isLast) {
						BigDecimal open = dataList.get(i + 1).getT("open");
						rise = open.divide(lastValue, 4, RoundingMode.HALF_UP);
						lastValue = open;
					}
					if(sellToday) {
						isHold = false;
						status = "AWAY";
						expect = expect.subtract(expect.divide(UF.DECIMAL10000).multiply(UF.DECIMAL6));
					}
					if(rise.compareTo(UF.DECIMAL1) == 1) {
						upCount ++;
					}
					expect = expect.multiply(rise);
				}
			}
		}
		if (buyCount <= 3/* || maxDays > 250 || minRise.compareTo(new BigDecimal("0.7")) == -1 */) {
			return null;
		}
		UFMap poolMap = new UFMap().add("BUY", buy).add("SELL", sell).add("STATUS", status).add("BASE", dataList.get(size - 1 - days).getT("dt"));
		if("AWAY".equals(status) || "NONE".equals(status)) {
			poolMap.add("TMR", getTmr(dataList, buy, days));
		}else {
			poolMap.add("TMR", getTmr(dataList, sell, days));
			if("GOGO".equals(status)) {
				int i = size - 1;
				while (Common.calRise(dataList, i - 1, days).compareTo(buy) >= 0) {
					i --;
				}
				poolMap.add("VOLUMN5", Common.getVolumnRate(dataList, i, 5));
			}
		}
		poolMap.add("HOLD", rise.multiply(UF.DECIMAL100).subtract(UF.DECIMAL100).setScale(2, RoundingMode.HALF_UP)).add("LAST", lastDealDate).add("sum", buyCount).add("earn", upCount);
		return poolMap.add("EXPECT", expect.setScale(4, RoundingMode.HALF_UP)).add("DAYS", days).add("minRise", minRise).add("maxDays", maxDays);
	}
	
	public static BigDecimal getTmr(List<UFMap> dataList, BigDecimal metric, int days) {
		int size = dataList.size();
		return getTmr(dataList.get(size - days).getT("close"), dataList.get(size - 1).getT("close"), metric);
	}
	
	private static BigDecimal getTmr(BigDecimal ago, BigDecimal now, BigDecimal metric) {
		return Util.riseRate(now, ago.multiply(metric));
	}
	
	public static BigDecimal getVolumnRate(List<UFMap> dataList, int i, int days) {
		BigDecimal volumn = dataList.get(i).getBigDecimal("volumn");
		BigDecimal volumnSum = UF.DECIMAL0;
		for(int j = 1; j <= days; j ++) {
			volumnSum = volumnSum.add(dataList.get(i - j).getBigDecimal("volumn"));
		}
		return volumn.multiply(new BigDecimal(days)).divide(volumnSum, 5, RoundingMode.HALF_UP);
	}
	
	public static int getBreakDays(List<UFMap> dataList, int i) {
		BigDecimal close = dataList.get(i).getT("close");
		int j = 1;
		for(; j <= 100; j ++) {
			if(close.compareTo(dataList.get(i - j).getT("close")) != 1) {
				break;
			}
		}
		return j - 1;
	}
	
	/**判断是否一字涨停无法买进*/
	public static boolean cannotBuy(List<UFMap> dataList, int i) {
		BigDecimal high = dataList.get(i).getT("high");
		BigDecimal low = dataList.get(i).getT("low");
		BigDecimal rate = dataList.get(i).getT("rate");
		return high.compareTo(low) == 0 && rate.compareTo(UF.DECIMAL0) == 1;
	}
	
	/**判断是否一字跌停无法卖出*/
	public static boolean cannotSell(List<UFMap> dataList, int i) {
		BigDecimal high = dataList.get(i).getT("high");
		BigDecimal low = dataList.get(i).getT("low");
		BigDecimal rate = dataList.get(i).getT("rate");
		return high.compareTo(low) == 0 && rate.compareTo(UF.DECIMAL0) == -1;
	}
	
	public static void updateCookie(String newCookie) throws Exception {
		cookie = newCookie;
		requestMap = null;
		getXueqiuConn();
	}
	
}
