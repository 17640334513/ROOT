package action.out;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import action.BaseAction;
import action.Common;
import action.Dispatcher;
import dao.Dao;
import uf.UFMap;
import util.Util;

public class FundAction extends BaseAction{

	/**
	 * 回测场外基金近一周涨跌幅在start%到end%之间的情况下买入，持有一周卖出的涨跌概率
	 * http://127.0.0.1:8080/Fund/weekScopeNextWeek.o?code=320007&name=诺安成长混合&start=1&end=2
	 * */
	static Dispatcher weekScopeNextWeek(UFMap map) throws Exception {
		String code = map.getT("code");
		String name = map.getT("name");
		BigDecimal start = map.getBigDecimal("start");
		BigDecimal end = map.getBigDecimal("end");
		List<UFMap> dataList = Common.getDataList(code);
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"日期", "近一周涨幅", "持有一周涨幅"});
		int totalDays = 0, upDays = 0, downDays = 0;
		BigDecimal nextRateSum = BigDecimal.ZERO;
		for(int i = 1; i < dataList.size(); i ++) {
			UFMap dataMap = dataList.get(i);
			LocalDate dt = dataMap.getT("DT");
			BigDecimal value = dataMap.getT("VALUE");
			BigDecimal weekRate = getLastRate(dataList, i, dt, value, 7);
			if(weekRate == null) continue;
			if(weekRate.compareTo(start) != -1 && weekRate.compareTo(end) != 1) {
				totalDays ++;
				BigDecimal nextWeekRate = getNextRate(dataList, i, dt, value, 7);
				if(nextWeekRate == null) break;
				if(nextWeekRate.compareTo(BigDecimal.ZERO) == 1) {
					upDays ++;
				}else if(nextWeekRate.compareTo(BigDecimal.ZERO) == -1) {
					downDays ++;
				}
				nextRateSum = nextRateSum.add(nextWeekRate);
				tableList.add(new String[] {dt.toString(), weekRate.toString(), nextWeekRate.toString()});
			}
		}
		String h1 = "回测" + name + "(" + code + ")" + "近一周涨跌幅在" + start + "%到" + end + "%之间的情况下买入，持有一周的涨跌概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p>满足条件总天数：").append(totalDays).append("</p>");
		body.append("<p style=\"color:red\">其中持有一周涨的天数：").append(upDays).append("</p>");
		body.append("<p style=\"color:green\">其中持有一周跌的天数：").append(downDays).append("</p>");
		body.append("<p>其中持有一周平的天数：").append(totalDays - upDays - downDays).append("</p>");
		body.append("<p style=\"color:red\">持有一周涨概率：" + Util.proportion(new BigDecimal(upDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:green\">持有一周跌概率：" + Util.proportion(new BigDecimal(downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p>持有一周平概率：" + Util.proportion(new BigDecimal(totalDays - upDays - downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:red\">持有一周平均涨幅：" + nextRateSum.divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP) + "%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 2, BigDecimal.ZERO));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	private static BigDecimal getLastRate(List<UFMap> dataList, int i, LocalDate dt, BigDecimal value, int days) {
		LocalDate compareDt = dt.minusDays(days);
		UFMap lastMap = null;
		for(int j = i - 1; j > i - days; j --) {
			if(j < 0) return null;
			lastMap = dataList.get(j);
			if(compareDt.compareTo(lastMap.getT("DT")) != -1) break;
		}
		BigDecimal lastValue = lastMap.getT("VALUE");
		return Util.riseRate(lastValue, value);
	}
	private static BigDecimal getNextRate(List<UFMap> dataList, int i, LocalDate dt, BigDecimal value, int days) {
		LocalDate compareDt = dt.plusDays(days);
		UFMap nextMap = null;
		for(int j = i + 1; j < i + days; j ++) {
			if(j == dataList.size()) return null;
			nextMap = dataList.get(j);
			if(compareDt.compareTo(nextMap.getT("DT")) != 1) break;
		}
		BigDecimal nextValue = nextMap.getT("VALUE");
		return Util.riseRate(value, nextValue);
	}
	
	/**
	 * 回测场外基金近一月涨跌幅在start%到end%之间的情况下买入，持有一月卖出的涨跌概率
	 * http://127.0.0.1:8080/Fund/monthScopeNextMonth.o?code=002611&name=博时黄金&start=-6&end=-3（✔）
	 * */
	static Dispatcher monthScopeNextMonth(UFMap map) throws Exception {
		String code = map.getT("code");
		String name = map.getT("name");
		BigDecimal start = map.getBigDecimal("start");
		BigDecimal end = map.getBigDecimal("end");
		List<UFMap> dataList = Common.getDataList(code);
		List<String[]> tableList = new ArrayList<>();
		tableList.add(new String[] {"日期", "近一月涨幅", "持有一月涨幅"});
		int totalDays = 0, upDays = 0, downDays = 0;
		BigDecimal nextRateSum = BigDecimal.ZERO;
		for(int i = 1; i < dataList.size(); i ++) {
			UFMap dataMap = dataList.get(i);
			LocalDate dt = dataMap.getT("DT");
			BigDecimal value = dataMap.getT("VALUE");
			BigDecimal monthRate = getLastRate(dataList, i, dt, value, 30);
			if(monthRate == null) continue;
			if(monthRate.compareTo(start) != -1 && monthRate.compareTo(end) != 1) {
				totalDays ++;
				BigDecimal nextMonthRate = getNextRate(dataList, i, dt, value, 30);
				if(nextMonthRate == null) break;
				if(nextMonthRate.compareTo(BigDecimal.ZERO) == 1) {
					upDays ++;
				}else if(nextMonthRate.compareTo(BigDecimal.ZERO) == -1) {
					downDays ++;
				}
				nextRateSum = nextRateSum.add(nextMonthRate);
				tableList.add(new String[] {dt.toString(), monthRate.toString(), nextMonthRate.toString()});
			}
		}
		String h1 = "回测" + name + "(" + code + ")" + "近一月涨跌幅在" + start + "%到" + end + "%之间的情况下买入，持有一月的涨跌概率";
		StringBuilder body = new StringBuilder("<h2>结果</h2>");
		body.append("<p>满足条件总天数：").append(totalDays).append("</p>");
		body.append("<p style=\"color:red\">其中持有一月涨的天数：").append(upDays).append("</p>");
		body.append("<p style=\"color:green\">其中持有一月跌的天数：").append(downDays).append("</p>");
		body.append("<p>其中持有一月平的天数：").append(totalDays - upDays - downDays).append("</p>");
		body.append("<p style=\"color:red\">持有一月涨概率：" + Util.proportion(new BigDecimal(upDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:green\">持有一月跌概率：" + Util.proportion(new BigDecimal(downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p>持有一月平概率：" + Util.proportion(new BigDecimal(totalDays - upDays - downDays), new BigDecimal(totalDays)) + "%</p>");
		body.append("<p style=\"color:red\">持有一月平均涨幅：" + nextRateSum.divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP) + "%</p>");
		body.append("<h2>明细</h2>");
		body.append(Common.htmlTable(tableList, 2, BigDecimal.ZERO));
		getSession().setAttribute("body", "<h1>" + h1 + "</h1>" + body);
		System.out.println(h1);
		System.out.println(body);
		return dispatcher("/jsp/result.jsp");
	}
	
	static String addPoolFund(UFMap map) throws Exception {
		List<UFMap> poolList = Dao.queryAll("POOL_FUND_BAK");
		for(UFMap poolMap : poolList) {
			String code = poolMap.getT("CODE");
			String name = poolMap.getT("NAME");
			CommonAction.chaseUpBest(new UFMap().add("code", code).add("name", name));
		}
		return "success";
	}
	
}
