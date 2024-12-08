package action.out;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import action.BaseAction;
import action.Common;
import action.Dispatcher;
import dao.Dao;
import uf.Chart;
import uf.UF;
import uf.UFMap;
import util.DateUtil;
import util.LogUtil;
import util.Util;

public class ChartAction extends BaseAction {
	
	static Chart asset(UFMap map) throws Exception {
		String type = map.getT("type");
		String today = map.getT("today");
		String interval = map.getT("interval");
		String start = null;
		if("hm".equals(interval)) {
			start = map.getT("start");
		}else if(!Util.isEmpty(interval)) {
			LocalDate localDate = LocalDate.parse(today, DateUtil.DTF10);
			start = localDate.minusMonths(Integer.parseInt(interval)).format(DateUtil.DTF10);
		}
		List<UFMap> list = Dao.query("chart", map.add("start", start));
		Chart chart = new Chart();
		if("revenueRate".equals(type)) {
			chart.addXAxisData(list.get(0).getString("DT"));
			chart.addSeriesData("0.00", type, "line");
			BigDecimal datum = UF.DECIMAL100;
			for(int i = 1; i < list.size(); i ++) {
				UFMap dataMap = list.get(i);
				UFMap dataMapLast = list.get(i - 1);
				BigDecimal revenue = dataMap.getT("REVENUE");
				BigDecimal revenueLast = dataMapLast.getT("REVENUE");
				BigDecimal rmbLast = dataMapLast.getT("RMB");
				datum = datum.multiply(revenue.subtract(revenueLast).add(rmbLast)).divide(rmbLast, 2, RoundingMode.HALF_UP);
				chart.addXAxisData(dataMap.getString("DT"));
				chart.addSeriesData(datum.subtract(UF.DECIMAL100).toString(), type, "line");
			}
		}else if("revenue".equals(type)) {
			for(UFMap dataMap : list) {
				chart.addXAxisData(dataMap.getString("DT"));
				chart.addSeriesData(dataMap.getString("REVENUE"), type, "line");
			}
		}else if("assetRate".equals(type)) {
			BigDecimal datum = list.get(0).getT("RMB");
			BigDecimal divisor = datum.divide(new BigDecimal(100));
			for(UFMap dataMap : list) {
				chart.addXAxisData(dataMap.getString("DT"));
				BigDecimal rmb = dataMap.getT("RMB");
				rmb = rmb.subtract(datum).divide(divisor, 2, RoundingMode.HALF_UP);
				chart.addSeriesData(rmb + "", type, "line");
			}
		}else if("asset".equals(type)) {
			for(UFMap dataMap : list) {
				chart.addXAxisData(dataMap.getString("DT"));
				chart.addSeriesData(dataMap.getString("RMB"), type, "line");
			}
		}
		return chart;
	}
	
	static String addAsset(UFMap map) throws Exception {
		BigDecimal rmb = map.getBigDecimal("rmb").setScale(2, RoundingMode.HALF_UP);
		BigDecimal revenue = map.getBigDecimal("revenue").setScale(2, RoundingMode.HALF_UP);
		Dao.delete("ASSET", map.add("DT", DateUtil.now10()));
		if(Dao.save("ASSET", map.add("RMB", rmb).add("REVENUE", revenue))) {
			return "总资产: " + rmb + "; 累计收益: " + revenue;
		}
		return "保存失败";
	}
	
	/**
	 * 查看ASSET
	 * http://127.0.0.1:8080/Chart/getAsset.o
	 * */
	static Dispatcher getAsset(UFMap map) throws Exception {
		List<UFMap> assetList = Dao.queryAll("ASSET");
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"DT", "RMB", "REVENUE", "RATE", "PROFIT", "OTHER"});
		UFMap assetMap;
		String[] columns;
		int upCount = 0, downCount = 0;
		for(int i = assetList.size() - 1; i > 0; i --) {
			assetMap = assetList.get(i);
			UFMap assetMapLast = assetList.get(i - 1);
			columns = new String[6];
			columns[0] = assetMap.getString("DT");
			columns[1] = assetMap.getString("RMB");
			columns[2] = assetMap.getString("REVENUE");
			BigDecimal revenue = assetMap.getT("REVENUE");
			BigDecimal revenueLast = assetMapLast.getT("REVENUE");
			BigDecimal rmb = assetMap.getT("RMB");
			BigDecimal rmbLast = assetMapLast.getT("RMB");
			BigDecimal profit = revenue.subtract(revenueLast);
			if(profit.compareTo(UF.DECIMAL0) == 1) {
				upCount ++;
			}else {
				downCount ++;
			}
			columns[3] = profit.multiply(UF.DECIMAL100).divide(rmbLast, 2, RoundingMode.HALF_UP) + "%";
			columns[4] = profit.toString();
			columns[5] = rmb.subtract(rmbLast).subtract(profit).toString();
			tableList.add(columns);
		}
		assetMap = assetList.get(0);
		columns = new String[6];
		columns[0] = assetMap.getString("DT");
		columns[1] = assetMap.getString("RMB");
		columns[2] = assetMap.getString("REVENUE");
		columns[3] = "0.00%";
		columns[4] = "0.00";
		columns[5] = "0.00";
		tableList.add(columns);
		String body = "总交易日：" + (upCount + downCount) + "，盈利天数：" + upCount + "，亏损天数：" + downCount+ "<br>";
		getSession().setAttribute("body", body + Common.htmlTable(tableList, 3, UF.DECIMAL0));
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 查看RECORD
	 * http://127.0.0.1:8080/Chart/getRecords.o
	 * */
	static Dispatcher getRecords(UFMap map) throws Exception {
		List<UFMap> recordsList = Dao.query("getRecords", null);
		List<String[]> tableList = new ArrayList<>();
		int revenue = 0, upCount = 0, downCount = 0;
		for(UFMap recordsMap : recordsList) {
			String[] columns = new String[5];
			columns[0] = recordsMap.getT("NAME");
			columns[1] = recordsMap.getT("BUY");
			columns[2] = recordsMap.getT("SELL");
			int profit = recordsMap.getT("PROFIT");
			if(profit > 0) {
				upCount ++;
			}else {
				downCount ++;
			}
			columns[3] = String.valueOf(profit);
			revenue += profit;
			columns[4] = String.valueOf(revenue);
			tableList.add(columns);
		}
		Collections.reverse(tableList);
		tableList.add(0, new String[] {"NAME", "BUY", "SELL", "PROFIT", "REVENUE"});
		String body = "交易次数：" + (upCount + downCount) + "，止盈次数：" + upCount + "，止损次数：" + downCount+ "<br>";
		getSession().setAttribute("body", body + Common.htmlTable(tableList, 3, UF.DECIMAL0));
		return dispatcher("/jsp/result.jsp");
	}
	
	static String addRecords(UFMap map) throws Exception {
		String name = map.getT("NAME");
		Object profit = map.get("PROFIT");
		String today = DateUtil.now8();
		if("".equals(profit)) {
			Dao.save("RECORDS", map.add("BUY", today).add("PROFIT", null));
			return today + "买入" + name;
		}else {
			Dao.executeSql("updateRecords", map.add("SELL", today));
			return today + "卖出" + name + "，盈利：" + profit;
		}
	}
	
	/**
	 * 查看当前持仓
	 * http://127.0.0.1:8080/Chart/getHold.o
	 * */
	static Dispatcher getHold(UFMap map) throws Exception {
		List<UFMap> holdList = Dao.query("getHold", null);
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"NAME", "BUY", "SELL", "STATUS", "HOLD", "TMR", "DAYS", "BASE", "LAST", "HISTORY", "EXPECT"});
		int upCount = 0, downCount = 0;
		for(UFMap holdMap : holdList) {
			String[] columns = new String[11];
			columns[0] = holdMap.getString("NAME");
			columns[1] = holdMap.getString("BUY");
			columns[2] = holdMap.getString("SELL");
			columns[3] = holdMap.getString("STATUS");
			columns[4] = holdMap.getString("HOLD");
			if(columns[4].startsWith("-")) {
				downCount ++;
			}else {
				upCount ++;
			}
			columns[5] = holdMap.getString("TMR");
			columns[6] = holdMap.getString("DAYS");
			columns[7] = holdMap.getString("BASE");
			columns[8] = holdMap.getString("LAST");
			columns[9] = holdMap.getString("HISTORY");
			columns[10] = holdMap.getString("EXPECT");
			tableList.add(columns);
		}
		String body = DateUtil.now19() + "<br>持仓股数：" + (upCount + downCount) + "，浮盈股数：" + upCount + "，浮亏股数：" + downCount+ "<br>";
		getSession().setAttribute("body", body + Common.htmlTable(tableList, 4, UF.DECIMAL0));
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 把趋势轮动法的历史数据存到DB里
	 * http://127.0.0.1:8080/Chart/trendSave.o
	 * */
	static void trendSave(UFMap map) throws Exception {
		List<UFMap> poolList = Dao.queryAll("POOL_STOCK");
		//List<UFMap> poolList = Dao.queryAll("POOL_FUND");
		for(UFMap poolMap : poolList) {
			String code = poolMap.getT("CODE");
			String name = poolMap.getT("NAME");
			BigDecimal buy = poolMap.getT("BUY");
			BigDecimal sell = poolMap.getT("SELL");
			int days = poolMap.getT("DAYS");
			List<UFMap> dataList = Common.getDataList(Common.addStockBourse(code), Common.PERIOD_DAY);
			//List<UFMap> dataList = Common.getDataList(Common.addIndexOrFundOrBondBourse(code), Common.PERIOD_DAY);
			Boolean isHold = null;
			String status = "N";
			List<UFMap> historyList = new ArrayList<>();
			BigDecimal volumn5 = null;
			boolean changeVolumn = true;
			for(int i = Common.IGNORE_DAYS; i < dataList.size() - 1; i ++) {
				UFMap dataMap = dataList.get(i);
				Date dt = dataMap.getT("dt");
				BigDecimal recentRise = Common.calRise(dataList, i, days);
				if(isHold == null) {
					boolean buyToday = false;
					if(recentRise.compareTo(buy) >= 0) {
						if(changeVolumn) {
							volumn5 = Common.getVolumnRate(dataList, i, 5);
						}
						if(Common.cannotBuy(dataList, i + 1)) {
							changeVolumn = false;
						}else {
							buyToday = true;
							changeVolumn = true;
						}
					}
					if(!buyToday) {
						isHold = false;
					}
				}else if(!isHold) {
					status = "N";
					boolean buyToday = false;
					if(recentRise.compareTo(buy) >= 0) {
						if(changeVolumn) {
							volumn5 = Common.getVolumnRate(dataList, i, 5);
						}
						if(Common.cannotBuy(dataList, i + 1)) {
							changeVolumn = false;
						}else {
							buyToday = true;
							changeVolumn = true;
						}
					}
					if(buyToday) {
						isHold = true;
						status = "G";
					}
				}else {
					status = null;
					boolean sellToday = recentRise.compareTo(sell) <= 0 && !Common.cannotSell(dataList, i + 1);
					if(sellToday) {
						isHold = false;
						status = "A";
					}
				}
				if("N".equals(status)) {
					continue;
				}
				if(dt.after(DT_START)) {
					BigDecimal nextOpen = dataList.get(i + 1).getT("open");
					UFMap historyMap = new UFMap().add("DT", dt).add("CODE", code).add("STATUS", status);
					if("G".equals(status)) {
						historyMap.add("NEXT_OPEN_RATE", nextOpen.divide(dataMap.getT("close"), 4, RoundingMode.HALF_UP));
						historyMap.add("VOLUMN5", volumn5);
					}else {
						historyMap.add("RATE", nextOpen.divide(dataMap.getT("open"), 4, RoundingMode.HALF_UP));
					}
					historyList.add(historyMap);
				}
			}
			//Dao.executeSqlBatch("insertHistoryFund", historyList);
			Dao.executeSqlBatch("insertHistoryStock", historyList);
			LogUtil.print(name + " size: " + historyList.size(), "");
		}
	}
	
	private static Date DT_START = Date.valueOf("2012-01-01");
	private static int HOLD_NUM = 0;
	private static BigDecimal VOLUMN_STOCK = UF.DECIMAL0;
	private static BigDecimal NEXT_OPEN_STOCK = UF.DECIMAL0;
	private static BigDecimal VOLUMN_FUND = UF.DECIMAL0;
	private static BigDecimal NEXT_OPEN_FUND = UF.DECIMAL0;
	/**
	 * 回测趋势轮动法的历史走势
	 * http://127.0.0.1:8080/Chart/trendHistory.o?HOLD_NUM=10&VOLUMN_STOCK=0.805&NEXT_OPEN_STOCK=1.005&VOLUMN_FUND=0.805&NEXT_OPEN_FUND=1.005
	 * */
	static void trendHistory(UFMap map) throws Exception {
		HOLD_NUM = map.getInt("HOLD_NUM");
		VOLUMN_STOCK = map.getBigDecimal("VOLUMN_STOCK");
		NEXT_OPEN_STOCK = map.getBigDecimal("NEXT_OPEN_STOCK");
		VOLUMN_FUND = map.getBigDecimal("VOLUMN_FUND");
		NEXT_OPEN_FUND = map.getBigDecimal("NEXT_OPEN_FUND");
		List<Date> dtList = Dao.queryList("getDts", null);
		List<UFMap> trendList = new ArrayList<>();
		BigDecimal value = UF.DECIMAL10000, balance = UF.DECIMAL10000;
		Map<String, BigDecimal> holdMap = new HashMap<>();
		Dao.isPrint(false);
		for(Date dt : dtList) {
			List<UFMap> historyList = Dao.query("getHistory", dt);
			for(UFMap historyMap : historyList) {
				String code = historyMap.getT("CODE");
				if(holdMap.containsKey(code)) {
					BigDecimal rate = historyMap.getT("RATE");
					BigDecimal part = holdMap.get(code);
					value = value.subtract(part);
					part = part.multiply(rate).setScale(2, RoundingMode.HALF_UP);
					if("A".equals(historyMap.getT("STATUS"))) {
						BigDecimal premium = part.divide(UF.DECIMAL10000, 2, RoundingMode.HALF_UP);
						if(!code.startsWith("F")) {
							premium = premium.multiply(UF.DECIMAL6);
						}
						part = part.subtract(premium).setScale(2, RoundingMode.HALF_UP);
						holdMap.remove(code);
						balance = balance.add(part);
					}else {
						holdMap.put(code, part);
					}
					value = value.add(part);
				}else if("G".equals(historyMap.getT("STATUS")) && holdMap.size() < HOLD_NUM) {
					BigDecimal nextOpenRate = historyMap.getT("NEXT_OPEN_RATE");
					boolean isFund = code.startsWith("F");
					if(nextOpenRate.compareTo(isFund ? NEXT_OPEN_FUND : NEXT_OPEN_STOCK) >= 0) {
						BigDecimal volumnRate = historyMap.getT("VOLUMN5");
						if(volumnRate.compareTo(isFund ? VOLUMN_FUND : VOLUMN_STOCK) == -1) {
							BigDecimal part = balance.divide(new BigDecimal(HOLD_NUM - holdMap.size()), 2, RoundingMode.HALF_UP);
							balance = balance.subtract(part);
							BigDecimal premium = part.divide(UF.DECIMAL10000, 2, RoundingMode.HALF_UP);
							part = part.subtract(premium).setScale(2, RoundingMode.HALF_UP);
							value = value.subtract(premium);
							holdMap.put(code, part);
						}
					}
				}
			}
			UFMap trendMap = new UFMap().add("DT", dt);
			trendList.add(trendMap.add("VALUE", value));
			LogUtil.print(trendMap + "，balance：" + balance + "，持仓：" + holdMap, "");
		}
		Dao.isPrint(true);
		Dao.delete("TREND", null);
		Dao.saveBatch("TREND", trendList);
	}
	
	/**
	 * 回测趋势轮动法的历史走势
	 * http://127.0.0.1:8080/Chart/trend.o
	 * */
	static Chart trend(UFMap map) throws Exception {
		List<UFMap> list = Dao.queryAll("TREND");
		Chart chart = new Chart();
		BigDecimal top = new BigDecimal("10000");
		BigDecimal maxBack = UF.DECIMAL1, back = UF.DECIMAL1;
		int maxDays = 0, days = 0;
		Date maxBackDate = null, maxDaysDate = null;
		UFMap yearMap = new UFMap();
		BigDecimal yearValue = top;
		String year = "2012";
		BigDecimal value = null;
		for(UFMap dataMap : list) {
			value = dataMap.getT("VALUE");
			Date dt = dataMap.getT("DT");
			String dtYear = dt.toString().substring(0, 4);
			if(!year.equals(dtYear)) {
				yearMap.add(year, value.divide(yearValue, 4, RoundingMode.HALF_UP));
				year = dtYear;
				yearValue = value;
			}
			if(value.compareTo(top) == 1) {
				top = value;
				if(days > maxDays) {
					maxDays = days;
					maxDaysDate = dt;
				}
				days = 0;
			}else {
				back = value.divide(top, 4, RoundingMode.HALF_UP);
				if(back.compareTo(maxBack) == -1) {
					maxBack = back;
					maxBackDate = dt;
				}
				days ++;
			}
			chart.addXAxisData(dataMap.getString("DT"));
			chart.addSeriesData(value.toString(), "Trend", "line");
		}
		LogUtil.print(maxBackDate + " 最大回撤：" + maxBack + "; " + maxDaysDate + " 最大套牢天数：" + maxDays, "");
		LogUtil.print(yearMap, "");
		Dao.save("BACK_TEST", new UFMap().add("HOLD", HOLD_NUM).add("VOLUMN_STOCK", VOLUMN_STOCK).add("VOLUMN_FUND", VOLUMN_FUND).add("EXPECT", value)
				.add("NEXT_OPEN_STOCK", NEXT_OPEN_STOCK).add("NEXT_OPEN_FUND", NEXT_OPEN_FUND).add("MIN_RISE", maxBack).add("MAX_DAYS", maxDays));
		return chart;
	}
	
	static String updateCookie(UFMap map) throws Exception {
		String cookie = map.getT("cookie");
		if(cookie == null || "".equals(cookie.trim())) {
			return "new cookie is null";
		}
		Common.updateCookie(cookie);
		return "update cookie success";
	} 
	
	/**
	 * 每年重算一次最优解
	 * http://127.0.0.1:8080/Chart/bestUpdate.o?table=POOL_STOCK
	 * */
	static void bestUpdate(UFMap map) throws Exception {
		String table = map.getT("table");
		List<UFMap> updateList = Dao.query("getBestUpdate", table);
		boolean isStock = "POOL_STOCK".equals(table);
		for(UFMap updateMap : updateList) {
			String code = updateMap.getT("CODE");
			if(isStock) {
				code = Common.addStockBourse(code);
			}
			String name = updateMap.getT("NAME");
			List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
			BigDecimal history = Common.calHistory(dataList.subList(Common.IGNORE_DAYS, dataList.size()));
			BigDecimal hundredth = new BigDecimal("0.01");
			BigDecimal onePointFive = new BigDecimal("1.5");
			BigDecimal zeroPointNine = new BigDecimal("0.9");
			List<UFMap> poolList = new ArrayList<>();
			for(BigDecimal sell = Common.ZERO_POINT_SEVEN; sell.compareTo(onePointFive) == -1; sell = sell.add(hundredth)) {
				LogUtil.print(name + " : " + sell);
				BigDecimal buy = sell.add(hundredth);
				for(buy = buy.compareTo(zeroPointNine) == 1 ? buy : zeroPointNine; buy.compareTo(onePointFive) <= 0; buy = buy.add(hundredth)) {
					for(int days = 1; days <= 60; days ++) {
						UFMap chaseMap = Common.chaseUpBest(dataList, buy, sell, days);
						if(chaseMap != null && history.compareTo(chaseMap.getT("EXPECT")) <= 0) {
							poolList.add(chaseMap);
						}
					}
				}
			}
			int size = poolList.size();
			LogUtil.print(name + " poolList size: " + size);
			poolList.sort((poolMap1, poolMap2) -> {
				BigDecimal pool1 = poolMap1.getT("EXPECT");
				BigDecimal pool2 = poolMap2.getT("EXPECT");
				return pool2.compareTo(pool1);
			});
			if(code.length() == 6) {
				Dao.save("POOL_FUND_BAK", poolList.get(0).add("CODE", code).add("NAME", name).add("HISTORY", history));
			}else if(code.length() == 8) {
				Dao.save("POOL_STOCK_BAK", poolList.get(0).add("CODE", code.substring(2)).add("NAME", name).add("HISTORY", history));
			}
		}
	}
	
}
