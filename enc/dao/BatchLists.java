package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import uf.UFMap;

public class BatchLists {
	
	private List<String> sqlList = new ArrayList<String>();
	private List<List<Object>> paramListList = new ArrayList<List<Object>>();
	private List<String> printSqlList = new ArrayList<String>();
	
	public void add(String sqlKey,Object params) {
		SqlBean sqlBean=SqlReader.get(sqlKey);
		List<Object> paramList = new ArrayList<Object>();
		StringBuilder printSql = new StringBuilder();
		sqlList.add(SqlUtil.toJdbc(sqlBean,params,paramList,printSql));
		printSqlList.add(printSql.toString());
		paramListList.add(paramList);
	}	
	
	public void addSave(String tableName, UFMap columnDataMap) {
		StringBuilder columns=new StringBuilder();
		StringBuilder values=new StringBuilder();
		StringBuilder printValues=new StringBuilder();
		List<Object> paramList=new ArrayList<Object>();
		for (Map.Entry<String,Object> entry : columnDataMap.entrySet()) {	
			String columnName=entry.getKey();
			if(columnName.equals(columnName.toUpperCase())){
				columns.append(","+columnName);
				values.append(",?");
				printValues.append(","+entry.getValue());
				paramList.add(entry.getValue());
			}	
		}
		sqlList.add("INSERT INTO "+tableName+"("+columns.substring(1)+") VALUES("+values.substring(1)+")");
		printSqlList.add("INSERT INTO "+tableName+"("+columns.substring(1)+") VALUES("+printValues.substring(1)+")");
		paramListList.add(paramList);
	}
	
	public List<String> getSqlList(){
		return sqlList;
	}
	
	public List<List<Object>> getParamListList(){
		return paramListList;
	}
	
	public List<String> getPrintSqlList(){
		return printSqlList;
	}
	
}
