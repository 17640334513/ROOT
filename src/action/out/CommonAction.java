package action.out;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import action.BaseAction;
import action.Common;
import action.Dispatcher;
import dao.Dao;
import uf.UF;
import uf.UFMap;
import util.DateUtil;
import util.LogUtil;
import util.Util;

@SuppressWarnings("deprecation")
public class CommonAction extends BaseAction{
	
	/**
	 * 回测code当天涨幅在start%~end%之间，下一个交易日的涨跌概率
	 * http://127.0.0.1:8080/Common/scopeNext.o?code=SH513100&name=纳指ETF&start=4&end=5
	 * */
	static Dispatcher scopeNext(UFMap map) throws Exception {
		String code = map.getT("code");//需要带前缀
		String name = map.getT("name");
		BigDecimal start = map.getBigDecimal("start");
		BigDecimal end = map.getBigDecimal("end");
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
		int upCount = 0;
		int downCount = 0;
		int sum = 0;
		BigDecimal nextRateSum = BigDecimal.ZERO;
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"日期", "当天涨幅", "下个交易日涨幅"});
		for(int i = 0; i < dataList.size() - 1; i ++) {
			UFMap dataMap = dataList.get(i);
			if(start.compareTo(dataMap.getT("rate")) == -1 && end.compareTo(dataMap.getT("rate")) == 1) {
				sum ++;
				BigDecimal nextRate = dataList.get(i + 1).getT("rate");
				nextRateSum = nextRateSum.add(nextRate);
				tableList.add(new String[] {dataMap.getString("dt"), dataMap.get("rate") + "%", nextRate + "%"});
				if(nextRate.compareTo(BigDecimal.ZERO) == 1) {
					upCount ++;
				}else if(nextRate.compareTo(BigDecimal.ZERO) == -1) {
					downCount ++;
				}
			}
		}
		String h1 = "回测" + name + "(" + code + ")" + "当天涨幅在" + start + "%到" + end + "%之间，下一个交易日的涨跌概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p>满足条件总天数：").append(sum).append("</p>");
		body.append("<p style=\"color:red\">其中下个交易日涨的天数：").append(upCount).append("</p>");
		body.append("<p style=\"color:green\">其中下个交易日跌的天数：").append(downCount).append("</p>");
		body.append("<p>其中下个交易日平的天数：").append(sum - upCount - downCount).append("</p>");
		body.append("<p style=\"color:red\">下个交易日涨概率：" + Util.proportion(new BigDecimal(upCount), new BigDecimal(sum)) + "%</p>");
		body.append("<p style=\"color:green\">下个交易日跌概率：" + Util.proportion(new BigDecimal(downCount), new BigDecimal(sum)) + "%</p>");
		body.append("<p>下个交易日平概率：" + Util.proportion(new BigDecimal(sum - upCount - downCount), new BigDecimal(sum)) + "%</p>");
		body.append("<p style=\"color:red\">下个交易日平均涨幅：" + nextRateSum.divide(new BigDecimal(sum), 2, RoundingMode.HALF_UP) + "%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 2, BigDecimal.ZERO));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 回测code在每年的哪个月涨幅最大
	 * http://127.0.0.1:8080/Common/whichMonth.o?code=SZ000661&name=长春高新&includeCurrent=false（✔）
	 * */
	static Dispatcher whichMonth(UFMap map) throws Exception {
		String code = map.getT("code");//需要带前缀
		String name = map.getT("name");
		boolean includeCurrent = map.getBoolean("includeCurrent");//是否包括当前今年今月
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_MONTH);
		if(!includeCurrent) dataList.remove(dataList.size() - 1);
		int[][] months = new int[12][];
		for(UFMap dataMap : dataList) {
			Date dt = dataMap.getT("dt");
			BigDecimal rate = dataMap.getT("rate");
			int month = dt.getMonth();
			int[] datas = months[month];
			if(datas == null) {
				datas = new int[]{0, 0};
				months[month] = datas;
			}
			datas[0] ++;
			if(rate.compareTo(BigDecimal.ZERO) == 1) {
				datas[1] ++;
			}
		}
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"月份", "涨概率"});
		Object[][] rates = new Object[12][];
		for(int i = 0; i < 12; i ++) {
			int[] datas = months[i];
			rates[i] = new Object[2];
			rates[i][0] = i + 1;
			rates[i][1] = new BigDecimal(datas[1] * 100).divide(new BigDecimal(datas[0]), 2, RoundingMode.HALF_UP);
			tableList.add(new String[]{rates[i][0] + "月", rates[i][1] + "%"});
		}
		Arrays.sort(rates, (month1, month2) -> {//从小到大排序
			BigDecimal rate1 = (BigDecimal) month1[1];
			BigDecimal rate2 = (BigDecimal) month2[1];
			return rate1.compareTo(rate2);
		});
		String h1 = "回测" + name + "(" + code + ")" + "历史各个月份涨概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p style=\"color:red\">哪个月涨概率最大：").append(rates[11][0]).append("月，涨概率：").append(rates[11][1]).append("%</p>");
		body.append("<p style=\"color:green\">哪个月涨概率最小：").append(rates[0][0]).append("月，涨概率：").append(rates[0][1]).append("%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 1, new BigDecimal("50")));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 回测code在当天成交量是上个交易日的n倍以上，且涨幅大于percent%的情况下，下个交易日的涨跌概率
	 * http://127.0.0.1:8080/Common/volRiseNext.o?code=SZ399006&name=创业板指&n=1.2&percent=1（✔）
	 * */
	static Dispatcher volRiseNext(UFMap map) throws Exception {
		String code = map.getT("code");
		String name = map.getT("name");
		BigDecimal n = map.getBigDecimal("n");
		BigDecimal percent = map.getBigDecimal("percent");
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
		int totalDays = 0, upDays = 0, downDays = 0;
		BigDecimal nextRateSum = BigDecimal.ZERO;
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"日期", "成交量是上个交易日的多少倍", "当天涨幅", "下个交易日涨幅"});
		for(int i = 1; i < dataList.size() - 1; i ++) {
			UFMap dataMap = dataList.get(i);
			BigDecimal volumn = dataMap.getBigDecimal("volumn");
			BigDecimal lastVolumn = dataList.get(i - 1).getBigDecimal("volumn");
			BigDecimal volN = volumn.divide(lastVolumn, 4, RoundingMode.HALF_UP);
			BigDecimal rate = dataMap.getBigDecimal("rate");
			if(volN.compareTo(n) != -1 && rate.compareTo(percent) != -1) {
				totalDays ++;
				BigDecimal nextRate = dataList.get(i + 1).getBigDecimal("rate");
				if(nextRate.compareTo(BigDecimal.ZERO) == 1) {
					upDays ++;
				}else if(nextRate.compareTo(BigDecimal.ZERO) == -1) {
					downDays ++;
				}
				nextRateSum = nextRateSum.add(nextRate);
				tableList.add(new String[] {dataMap.getString("dt"), volN + "", rate + "", nextRate + ""});
			}
		}
		String h1 = "回测" + name + "(" + code + ")在当天成交量是上个交易日的" + n + "倍以上，且涨幅大于" + percent + "%的情况下，下个交易日的涨跌概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p>满足条件总天数：").append(totalDays).append("</p>");
		body.append("<p style=\"color:red\">其中下个交易日涨的天数：").append(upDays).append("</p>");
		body.append("<p style=\"color:green\">其中下个交易日跌的天数：").append(downDays).append("</p>");
		body.append("<p>其中下个交易日平的天数：").append(totalDays - upDays - downDays).append("</p>");
		body.append("<p style=\"color:red\">下个交易日涨概率：" + Util.proportion(new BigDecimal(upDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:green\">下个交易日跌概率：" + Util.proportion(new BigDecimal(downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p>下个交易日平概率：" + Util.proportion(new BigDecimal(totalDays - upDays - downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:red\">下个交易日平均涨幅：" + nextRateSum.divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP) + "%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 3, BigDecimal.ZERO));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 回测code连涨两天买入，连跌两天卖出的涨跌概率(持有期间不重复买入)
	 * http://127.0.0.1:8080/Common/chase2day.o?code=SZ161127&name=标普生物（✔）
	 * */
	static Dispatcher chase2day(UFMap map) throws Exception {
		String code = map.getT("code");
		String name = map.getT("name");
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
		int totalDays = 0, upDays = 0, downDays = 0;
		BigDecimal nextRateSum = BigDecimal.ZERO;
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"买入日期", "卖出日期", "涨幅"});
		boolean state = false;
		BigDecimal yield = BigDecimal.ONE;
		String buyDate = null;
		for(int i = 2; i < dataList.size(); i ++) {
			BigDecimal rate = dataList.get(i).getBigDecimal("rate");
			boolean up2 = dataList.get(i - 2).getBigDecimal("rate").compareTo(BigDecimal.ZERO) == 1;
			UFMap dataMap = dataList.get(i - 1);
			boolean up1 = dataMap.getBigDecimal("rate").compareTo(BigDecimal.ZERO) == 1;
			if(state) {
				if(!up1 && !up2) {
					state = false;
					BigDecimal nextRate = yield.subtract(BigDecimal.ONE).multiply(UF.DECIMAL100);
					yield = BigDecimal.ONE;
					nextRateSum = nextRateSum.add(nextRate);
					totalDays ++;
					if(nextRate.compareTo(BigDecimal.ZERO) == 1) {
						upDays ++;
					}else if(nextRate.compareTo(BigDecimal.ZERO) == -1) {
						downDays ++;
					}
					tableList.add(new String[] {buyDate, dataMap.getString("dt"), nextRate.setScale(2, RoundingMode.HALF_UP) + "%"});
				}else {
					yield = yield.multiply(rate.divide(UF.DECIMAL100).add(BigDecimal.ONE));
				}
			}else {
				if(up1 && up2) {
					state = true;
					yield = rate.add(UF.DECIMAL100).multiply(yield).divide(UF.DECIMAL100, 4, RoundingMode.HALF_UP);
					buyDate = dataMap.getString("dt");
				}
			}
		}
		String h1 = "回测" + name + "(" + code + ")连涨两天买入，连跌两天卖出的涨跌概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p>满足条件交易总次数：").append(totalDays).append("</p>");
		body.append("<p style=\"color:red\">其中取得正收益的次数：").append(upDays).append("</p>");
		body.append("<p style=\"color:green\">其中取得负收益的次数：").append(downDays).append("</p>");
		body.append("<p>其中取得平收益的次数：").append(totalDays - upDays - downDays).append("</p>");
		body.append("<p style=\"color:red\">取得正收益的概率：" + Util.proportion(new BigDecimal(upDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:green\">取得负收益的概率：" + Util.proportion(new BigDecimal(downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p>取得平收益的概率：" + Util.proportion(new BigDecimal(totalDays - upDays - downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:red\">平均收益：" + nextRateSum.divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP) + "%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 2, BigDecimal.ZERO));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 回测code在每周的星期几涨幅最大
	 * http://127.0.0.1:8080/Common/whichDayOfWeek.o?code=SH512690&name=酒ETF（✔）
	 * */
	static Dispatcher whichDayOfWeek(UFMap map) throws Exception {
		String code = map.getT("code");//需要带前缀
		String name = map.getT("name");
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
		int[][] weeks = new int[5][];
		for(UFMap dataMap : dataList) {
			Date dt = dataMap.getT("dt");
			BigDecimal rate = dataMap.getT("rate");
			int dayOfWeek = dt.getDay() - 1;
			int[] datas = weeks[dayOfWeek];
			if(datas == null) {
				datas = new int[]{0, 0};
				weeks[dayOfWeek] = datas;
			}
			datas[0] ++;//总天数
			if(rate.compareTo(BigDecimal.ZERO) == 1) {
				datas[1] ++;//涨的天数
			}
		}
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"星期几", "涨概率"});
		Object[][] rates = new Object[5][];
		for(int i = 0; i < 5; i ++) {
			int[] datas = weeks[i];
			rates[i] = new Object[2];
			rates[i][0] = i + 1;
			rates[i][1] = new BigDecimal(datas[1] * 100).divide(new BigDecimal(datas[0]), 2, RoundingMode.HALF_UP);
			tableList.add(new String[]{"星期" + rates[i][0], rates[i][1] + "%"});
		}
		Arrays.sort(rates, (week1, week2) -> {//从小到大排序
			BigDecimal rate1 = (BigDecimal) week1[1];
			BigDecimal rate2 = (BigDecimal) week2[1];
			return rate1.compareTo(rate2);
		});
		String h1 = "回测" + name + "(" + code + ")" + "历史各个星期几涨概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p style=\"color:red\">星期几涨概率最大：星期").append(rates[4][0]).append("，涨概率：").append(rates[4][1]).append("%</p>");
		body.append("<p style=\"color:green\">星期几涨概率最小：星期").append(rates[0][0]).append("，涨概率：").append(rates[0][1]).append("%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 1, new BigDecimal("50")));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 回测code在每月的几号涨概率最大
	 * http://127.0.0.1:8080/Common/whichDayOfMonth.o?code=SZ159920&name=恒生ETF
	 * */
	static Dispatcher whichDayOfMonth(UFMap map) throws Exception {
		String code = map.getT("code");//需要带前缀
		String name = map.getT("name");
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
		int[][] days = new int[31][];
		for(UFMap dataMap : dataList) {
			Date dt = dataMap.getT("dt");
			BigDecimal rate = dataMap.getT("rate");
			int dayOfMonth = dt.getDate() - 1;
			int[] datas = days[dayOfMonth];
			if(datas == null) {
				datas = new int[]{0, 0};
				days[dayOfMonth] = datas;
			}
			datas[0] ++;//总天数
			if(rate.compareTo(BigDecimal.ZERO) == 1) {
				datas[1] ++;//涨的天数
			}
		}
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"星期几", "涨概率"});
		Object[][] rates = new Object[31][];
		for(int i = 0; i < 31; i ++) {
			int[] datas = days[i];
			rates[i] = new Object[2];
			rates[i][0] = i + 1;
			rates[i][1] = new BigDecimal(datas[1] * 100).divide(new BigDecimal(datas[0]), 2, RoundingMode.HALF_UP);
			tableList.add(new String[]{rates[i][0] + "号", rates[i][1] + "%"});
		}
		Arrays.sort(rates, (day1, day2) -> {//从小到大排序
			BigDecimal rate1 = (BigDecimal) day1[1];
			BigDecimal rate2 = (BigDecimal) day2[1];
			return rate1.compareTo(rate2);
		});
		String h1 = "回测" + name + "(" + code + ")" + "每月的几号涨概率最大";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p style=\"color:red\">几号涨概率最大：").append(rates[30][0]).append("号，涨概率：").append(rates[30][1]).append("%</p>");
		body.append("<p style=\"color:green\">几号涨概率最小：").append(rates[0][0]).append("号，涨概率：").append(rates[0][1]).append("%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 1, new BigDecimal("50")));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 回测code近days日涨幅高于buy%时买入，近days日涨幅低于sell%时卖出的累计收益 (趋势轮动法)
	 * http://127.0.0.1:8080/Common/chaseUp.o?code=SZ399006&name=创业板指&buy=1.07&sell=1&days=20
	 * 其中code如果是个股则必须带前缀
	 * */
	static Dispatcher chaseUp(UFMap map) throws Exception {
		String code = map.getT("code");//需要带前缀
		String name = map.getT("name");
		BigDecimal buy = map.getBigDecimal("buy");
		BigDecimal sell = map.getBigDecimal("sell");
		int days = map.getInt("days");
		List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
		Boolean isHold = null;
		String status = "NONE";
		BigDecimal revenue = BigDecimal.ONE;
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"躲跌", "买入日期", "前" + days + "天涨幅", "卖出日期", "前" + days + "天涨幅", "收益"});
		String[] rows = null;
		int upCount = 0, downCount = 0;
		BigDecimal lastValue = dataList.get(Common.IGNORE_DAYS).getT("open");
		for(int i = Common.IGNORE_DAYS; i < dataList.size(); i ++) {
			UFMap dataMap = dataList.get(i);
			BigDecimal recentRise = Common.calRise(dataList, i, days);
			boolean isLast = i == dataList.size() - 1;
			if(isHold == null) {
				boolean buyToday = recentRise.compareTo(buy) >= 0 && (isLast || !Common.cannotBuy(dataList, i + 1));
				if(!buyToday) {
					isHold = false;
				}
			}else if(!isHold) {
				status = "NONE";
				boolean buyToday = recentRise.compareTo(buy) >= 0 && (isLast || !Common.cannotBuy(dataList, i + 1));
				if(buyToday) {
					isHold = true;
					status = "GOGO";
					rows = new String[]{"", dataMap.getString("dt"), recentRise.toString(), "", "", ""};
				}
			}else {
				if("GOGO".equals(status)) {
					BigDecimal open = dataMap.getT("open");
					rows[0] = open.divide(lastValue, 4, RoundingMode.HALF_UP).toString();
					lastValue = open;
				}
				status = "HOLD";
				boolean sellToday = recentRise.compareTo(sell) <= 0 && (isLast || !Common.cannotSell(dataList, i + 1));
				if(sellToday || isLast) {
					BigDecimal open = isLast ? dataMap.getT("close") : dataList.get(i + 1).getT("open");
					BigDecimal rise = open.divide(lastValue, 4, RoundingMode.HALF_UP);
					revenue = revenue.multiply(rise);
					lastValue = open;
					if(rise.compareTo(UF.DECIMAL1) == -1) {
						downCount ++;
					}else {
						upCount ++;
					}
					isHold = false;
					status = "AWAY";
					rows[3] = dataMap.getString("dt");
					rows[4] = recentRise.toString();
					rows[5] = rise.toString();
					tableList.add(rows);
				}
			}
		}
		String h1 = "回测" + name + "(" + code + ")近" + days + "日涨幅高于" + buy + "时买入，近" + days + "日涨幅低于" + sell + "时卖出的累计收益";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p>买卖总次数：").append(tableList.size() - 1).append("</p>");
		body.append("<p style=\"color:red\">赚的次数：").append(upCount).append("</p>");
		body.append("<p style=\"color:green\">亏的次数：").append(downCount).append("</p>");
		body.append("<p>累计收益：").append(revenue.setScale(4, RoundingMode.HALF_UP)).append("</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 5, BigDecimal.ONE));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 计算code在趋势轮动法逻辑中的最优解
	 * 个股code要带前缀: https://81.70.199.189/Common/chaseUpBest.o?code=SZ300003&name=乐普医疗
	 * 其他code不带前缀: http://127.0.0.1:8080/Common/chaseUpBest.o?code=399005&name=中小100
	 * */
	static Dispatcher chaseUpBest(UFMap map) throws Exception {
		String code = map.getT("code");
		String name = map.getT("name");
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
		String body = name + "(" + code + ")趋势轮动法最优解(HISTORY=" + history + "): <br>";
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"BUY", "SELL", "EXPECT", "DAYS", "LAST", "STATUS", "HOLD", "TMR", "BASE", "maxDays", "minRise", "sum", "earn"});
		int show = size < 1000 ? size : 1000;
		for(int i = 0; i < show; i ++) {
			UFMap poolMap = poolList.get(i);
			String[] columns = new String[13];
			columns[0] = poolMap.getString("BUY");
			columns[1] = poolMap.getString("SELL");
			columns[2] = poolMap.getString("EXPECT");
			columns[3] = poolMap.getString("DAYS");
			columns[4] = poolMap.getString("LAST");
			columns[5] = poolMap.getString("STATUS");
			columns[6] = poolMap.getString("HOLD");
			columns[7] = poolMap.getString("TMR");
			columns[8] = poolMap.getString("BASE");
			columns[9] = poolMap.getString("maxDays");
			columns[10] = poolMap.getString("minRise");
			columns[11] = poolMap.getString("sum");
			columns[12] = poolMap.getString("earn");
			tableList.add(columns);
		}
		try {
			if(code.length() == 6) {
				Dao.save("POOL_FUND", poolList.get(0).add("CODE", code).add("NAME", name).add("HISTORY", history));
			}else if(code.length() == 8) {
				Dao.save("POOL_STOCK", poolList.get(0).add("CODE", code.substring(2)).add("NAME", name).add("HISTORY", history));
			}
		} catch (Exception e) {
			LogUtil.print(e);
		}
		getSession().setAttribute("body", body + Common.htmlTable(tableList, 9, 250));
		return dispatcher("/jsp/result.jsp");
	}
	
	/**
	 * 趋势轮动法(FUND)，每天执行一次决定明天买什么
	 * http://127.0.0.1:8080/Common/trendRefreshFund.o
	 * */
	static String trendRefreshFund(UFMap map) throws Exception {
		List<UFMap> poolList = Dao.queryAll("POOL_FUND");
		List<UFMap> batchParamsList = new ArrayList<>();
		List<String> errorList = new ArrayList<>();
		for(UFMap poolMap : poolList) {
			String code = poolMap.getT("CODE");
			String name = poolMap.getT("NAME");
			BigDecimal buy = poolMap.getT("BUY");
			BigDecimal sell = poolMap.getT("SELL");
			int days = poolMap.getT("DAYS");
			try {
				List<UFMap> dataList = Common.getDataList(code, Common.PERIOD_DAY);
				UFMap dataMap = Common.chaseUpBest(dataList, buy, sell, days).add("CODE", code);
				batchParamsList.add(dataMap.add("HISTORY", Common.calHistory(dataList.subList(Common.IGNORE_DAYS, dataList.size()))));
				LogUtil.print(name + " status: " + dataMap.get("STATUS"));
			}catch(IOException e) {
				errorList.add(name);
				LogUtil.print(name + "出错");
				LogUtil.print(e);
			}
		}
		Dao.executeSqlBatch("trendRefreshFund", batchParamsList);
		return errorList.toString();
	}
	
	/**
	 * 趋势轮动法(STOCK)，每天执行一次决定明天买什么
	 * http://127.0.0.1:8080/Common/trendRefreshStock.o
	 * */
	static String trendRefreshStock(UFMap map) throws Exception {
		List<UFMap> poolList = Dao.queryAll("POOL_STOCK");
		List<UFMap> batchParamsList = new ArrayList<>();
		List<String> errorList = new ArrayList<>();
		for(UFMap poolMap : poolList) {
			String code = poolMap.getT("CODE");
			String name = poolMap.getT("NAME");
			BigDecimal buy = poolMap.getT("BUY");
			BigDecimal sell = poolMap.getT("SELL");
			int days = poolMap.getT("DAYS");
			try {
				List<UFMap> dataList = Common.getDataList(Common.addStockBourse(code), Common.PERIOD_DAY);
				UFMap dataMap = Common.chaseUpBest(dataList, buy, sell, days).add("CODE", code);
				batchParamsList.add(dataMap.add("HISTORY", Common.calHistory(dataList.subList(Common.IGNORE_DAYS, dataList.size()))));
				LogUtil.print(name + " status: " + dataMap.get("STATUS"));
			}catch(IOException e) {
				errorList.add(name);
				LogUtil.print(name + "出错");
				LogUtil.print(e);
			}
		}
		Dao.executeSqlBatch("trendRefreshStock", batchParamsList);
		return errorList.toString();
	}
	
	/**
	 * 查看POOL_FUND/POOL_STOCK
	 * http://127.0.0.1:8080/Common/getPool.o?table=POOL_FUND
	 * */
	static Dispatcher getPool(UFMap map) throws Exception {
		String tableName = map.getT("table");
		List<UFMap> poolList = Dao.query("getPool", tableName);
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"CODE", "NAME", "BUY", "SELL", "STATUS", "HOLD", "TMR", "DAYS", "BASE", "LAST", "HISTORY", "EXPECT"});
		int away = 0, gogo = 0, hold = 0, none = 0;
		for(int i = 0; i < poolList.size(); i ++) {
			UFMap poolMap = poolList.get(i);
			String[] columns = new String[12];
			columns[0] = poolMap.getString("CODE");
			columns[1] = poolMap.getString("NAME");
			columns[2] = poolMap.getString("BUY");
			columns[3] = poolMap.getString("SELL");
			columns[4] = poolMap.getT("STATUS");
			if("AWAY".equals(columns[4])) {
				away ++;
			}else if("GOGO".equals(columns[4])) {
				gogo ++;
				columns[4] += "<br>" + poolMap.get("VOLUMN5");
			}else if("HOLD".equals(columns[4])) {
				hold ++;
			}else if("NONE".equals(columns[4])) {
				none ++;
			}
			columns[5] = poolMap.getString("HOLD");
			columns[6] = poolMap.getString("TMR");
			columns[7] = poolMap.getString("DAYS");
			columns[8] = poolMap.getString("BASE");
			columns[9] = poolMap.getString("LAST");
			columns[10] = poolMap.getString("HISTORY");
			columns[11] = poolMap.getString("EXPECT");
			tableList.add(columns);
		}
		String body = DateUtil.now19() + "<br>AWAY：" + away + "，GOGO：" + gogo + "，HOLD：" + hold + "，NONE：" + none + "<br>";
		getSession().setAttribute("body", body + Common.htmlTable(tableList, 4, "GOGO"));
		return dispatcher("/jsp/result.jsp");
	}
	
}
