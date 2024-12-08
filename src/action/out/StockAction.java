package action.out;

import java.util.List;

import action.BaseAction;
import action.Common;
import dao.Dao;
import uf.UFMap;

public class StockAction extends BaseAction{

	static String addPoolStock(UFMap map) throws Exception {
		List<UFMap> poolList = Dao.queryAll("POOL_STOCK_BAK");
		for(UFMap poolMap : poolList) {
			String code = Common.addStockBourse(poolMap.getT("CODE"));
			String name = poolMap.getT("NAME");
			CommonAction.chaseUpBest(new UFMap().add("code", code).add("name", name));
		}
		return "success";
	}
	
}
