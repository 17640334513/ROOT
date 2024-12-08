package dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import util.StringUtil;
import util.Util;

@SuppressWarnings({"rawtypes"})
public class SqlUtil {
	
	private static final String encStr="#";				
	
	/**把sql中单引号中的值打包*/
	public static String encSql(String sql,List<String> encList){
		String[] splitSqls=sql.split("'");
		StringBuilder tempSql = new StringBuilder(splitSqls[0]);
		for(int i=1;i<splitSqls.length;i++){
			if((i+1)%2==0){
				tempSql.append(encStr);
				encList.add(splitSqls[i]);
			}else{
				tempSql.append(splitSqls[i]);
			}
		}
		return tempSql.toString();
	}
	
	/**把打包过的sql还原*/
	public static String resSql(String sql,List<String> encList){
		if(!sql.contains(encStr)) return sql;
		String[] splitSqls=sql.split(encStr);
		StringBuilder tempSql = new StringBuilder();
		for(int i=0;i<splitSqls.length;i++){
			if(i==encList.size()) tempSql.append(splitSqls[i]);
			else tempSql.append(splitSqls[i]+"'"+encList.get(i)+"'");			
		}
		return tempSql.toString();
	}
	
	/** 把所有参数转换成jdbc执行格式 */
	static String toJdbc(SqlBean sqlBean, Object params, List<Object> paramList, StringBuilder printSql){
		String retSql = sqlBean.getSql();
		List<String> encList = sqlBean.getEncList();
		List<String> keyList = sqlBean.getKeyList();
		Map<Integer,String> checkMap = sqlBean.getCheckMap();
		List<String> replaceList = sqlBean.getReplaceList();
		boolean isCheck = checkMap!=null;
		boolean isReplace = replaceList!=null;
		if(Util.isEmpty(params)){//若参数为空
			if(isCheck) retSql = retSql.replaceAll("\\[[^]]*\\]", "").replaceAll("\\s{1,}"," ");//把[]内的部分去掉，把多个空格替换成一个空格
			if(isReplace) for(String key : replaceList) retSql=retSql.replaceAll(key, "");
			printSql.append(retSql=resSql(retSql.replaceAll("\\?", "null"), encList));
		}else if(keyList==null){			
			if(isReplace){
				if(params instanceof Map){
					Map paramMap=(Map)params;
					for(String key : replaceList){
						retSql=retSql.replaceAll(key, paramMap.get(key.substring(2, key.length()-2)).toString());
					}
				}else{
					for(String key : replaceList) retSql=retSql.replaceAll(key, params.toString());
				}
			}
			retSql = resSql(retSql, encList);
			printSql.append(retSql);
		}else if(params instanceof Map){//若参数不为空且为Map类型	
			Map paramMap = (Map)params;
			if(isCheck){
				List<String> tmpKeylist = new ArrayList<>(keyList);
				List<Integer> removeIndexList = new ArrayList<>();
				for(Map.Entry<Integer,String> entry : checkMap.entrySet()){
					int tmpIndex = entry.getKey();
					String key = keyList.get(tmpIndex);
					if(Util.isEmpty(paramMap.get(key))){
						String target = entry.getValue();
						retSql = retSql.replace(target, "");
						int paramNum = StringUtil.findSubStrCount(target, "?");
						for(int i = 0; i < paramNum; i ++) {
							removeIndexList.add(tmpIndex + i);
						}
					}
				}
				Collections.sort(removeIndexList);
				for(int i = removeIndexList.size() - 1; i >= 0; i --) {
					int removeIndex = removeIndexList.get(i);
					tmpKeylist.remove(removeIndex);
				}
				retSql = retSql.replaceAll("\\[|\\]", "").replaceAll("\\s{1,}"," ");//把[和]都去掉，把多个空格替换成一个空格
				if(isReplace){
					for(String key : replaceList){
						retSql = retSql.replaceAll(key, paramMap.get(key.substring(2, key.length()-2)).toString());
					}
				}
				retSql = paging(retSql, paramMap);
				retSql = resSql(retSql,encList);				
				String[] markChips = (retSql+" ").split("\\?");
				StringBuilder print = new StringBuilder(markChips[0]);
				for(int i=0;i<tmpKeylist.size();i++){
					Object param = paramMap.get(tmpKeylist.get(i));
					paramList.add(param);
					if(param==null) print.append("null").append(markChips[i+1]);
					else print.append("'"+param+"'").append(markChips[i+1]);		
				}
				printSql.append(print);
			}else{
				if(isReplace){
					for(String key : replaceList){
						retSql=retSql.replaceAll(key, paramMap.get(key.substring(2, key.length()-2)).toString());
					}
				}
				retSql = paging(retSql, paramMap);
				retSql = resSql(retSql,encList);				
				String[] markChips = (retSql+" ").split("\\?");
				StringBuilder print = new StringBuilder(markChips[0]);
				for(int i=0;i<keyList.size();i++){
					Object param = paramMap.get(keyList.get(i));
					paramList.add(param);
					if(param==null) print.append("null").append(markChips[i+1]);
					else print.append("'"+param+"'").append(markChips[i+1]);			
				}				
				printSql.append(print);
			}					
		}else{//若参数不为空且为单参数类型
			if(isCheck) retSql = retSql.replaceAll("\\[|\\]", "");//把[和]都去掉
			if(isReplace) for(String key : replaceList) retSql=retSql.replaceAll(key, params.toString());		
			String print = retSql = resSql(retSql,encList);
			for(int i=0;i<keyList.size();i++){
				paramList.add(params);
				print=print.replaceAll("\\?", "'"+params+"'");	
			}			
			printSql.append(print);
		}
		return retSql;		
	}
	
	/**ORACLE分页查询*/
	/*private static String paging(String sql,Map paramMap) {
		Object pagestart=paramMap.get(Dao.pagestart);
		Object pageend=paramMap.get(Dao.pageend);
		if(pagestart==null&&pageend==null){
			return sql;
		}else if(pagestart!=null&&pageend!=null){
			return "SELECT * FROM (SELECT CORE.*, ROWNUM RN FROM ("+sql+") CORE WHERE ROWNUM <= "+pageend+") WHERE RN >= "+pagestart;
		}else if(pagestart!=null&&pageend==null){
			return "SELECT * FROM (SELECT CORE.*, ROWNUM RN FROM ("+sql+") CORE) WHERE RN >= "+pagestart;
		}else{
			return "SELECT CORE.*, ROWNUM RN FROM ("+sql+") CORE WHERE RN <= "+pageend;
		}
	}*/
	
	/**MYSQL分页查询*/
	private static String paging(String sql, Map paramMap) {
		Object pagestart = paramMap.get(Dao.pagestart);
		Object pageend = paramMap.get(Dao.pageend);
		if(pagestart == null && pageend == null){
			return sql;
		}else if(pagestart != null&&pageend != null){
			return "SELECT * FROM ("+sql+") CORE LIMIT " + pagestart + "," + (Integer.parseInt(pageend.toString()) - Integer.parseInt(pagestart.toString()));
		}else if(pagestart != null&&pageend == null){
			return "SELECT * FROM ("+sql+") CORE LIMIT " + pagestart + ",-1";
		}else{
			return "SELECT * FROM ("+sql+") CORE LIMIT " + pageend;
		}
	}
	
}
