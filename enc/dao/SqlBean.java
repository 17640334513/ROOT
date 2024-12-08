package dao;

import java.util.List;
import java.util.Map;

public class SqlBean {
	
	private String sql;
	private List<String> keyList;
	private List<String> encList;
	private Map<Integer,String> checkMap;//第key个问号值为空时，replace掉value
	private List<String> replaceList;
	
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	
	public List<String> getKeyList() {
		return keyList;
	}
	public void setKeyList(List<String> keyList) {
		this.keyList = keyList;
	}
	
	public List<String> getEncList() {
		return encList;
	}
	public void setEncList(List<String> encList) {
		this.encList = encList;
	}
	
	public Map<Integer, String> getCheckMap() {
		return checkMap;
	}
	public void setCheckMap(Map<Integer, String> checkMap) {
		this.checkMap = checkMap;
	}
	
	public List<String> getReplaceList() {
		return replaceList;
	}
	public void setReplaceList(List<String> replaceList) {
		this.replaceList = replaceList;
	}
	
}
