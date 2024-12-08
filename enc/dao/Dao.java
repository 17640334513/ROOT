package dao;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import uf.UFMap;
import util.DateUtil;
import util.LogUtil;
import util.PropUtil;
import util.ThreadUtil;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Dao{
	
	/**查询超时时间*/
	public static int JDBC_QUERY_TIMEOUT=PropUtil.getInt("sys.JDBC_QUERY_TIMEOUT");
	/**批处理提交单位*/
	public static int JDBC_BATCH_UNIT=PropUtil.getInt("sys.JDBC_BATCH_UNIT");
	
	/** 分页查询起始位的key */
	public static final String pagestart = "pagestart";
	/** 分页查询截止位的key */
	public static final String pageend = "pageend";
	
	/** 数据源 */
	private static DataSource dataSource;
	
	/**用来生成自动ID*/
	private static AtomicInteger seq = new AtomicInteger(10000);
	/** 获取防止重复的5位序列号 */
	public static long getSeq() {
		long ret = seq.getAndIncrement();
		if (ret == 99999) seq.set(10000);
		return ret;
	}
	
	/** 初始化数据源 */
	public static void init() {
		try {
			String DATA_SOURCE = PropUtil.get("sys.DATA_SOURCE");
			Context context = null;
			try {
				context = new InitialContext();
				dataSource = (DataSource) context.lookup(DATA_SOURCE);
			} finally {
				if(context != null) context.close();
			}
			LogUtil.print("连接数据源["+DATA_SOURCE+"]成功", "");
		}catch(Exception e) {
			LogUtil.print(e);
		}		
    }
    
	/** 获取连接 */
	public static Connection getConn() throws Exception {
		return dataSource.getConnection();
	}
	
	/**
	 * 查询表数据
	 * @param sqlKey : sql文件名.sqlName
	 * @param params : 参数(Map类型或单参数类型)
	 * */
	public static List<UFMap> query(String sqlKey, Object params) throws Exception {						
		List<Object> paramList = new ArrayList<>();
		StringBuilder printSql = new StringBuilder();
		String sql=SqlUtil.toJdbc(SqlReader.get(sqlKey), params, paramList, printSql);
		return getAll(sql, paramList, printSql.toString());
	}
	/**查询tableName表的所有数据*/
	public static List<UFMap> queryAll(String tableName) throws Exception {
		String sql = "SELECT * FROM " + tableName;
		return getAll(sql, null, sql);
	}
	/**
	 * 查询表数据
	 * @param sql : 执行的sql语句(参数用问号代替的那种)
	 * @param paramList : 参数序列
	 * @param printSql : 打印的sql语句
	 * */
	public static List<UFMap> getAll(String sql, List<Object> paramList, String printSql) throws Exception {
		if(isPrint()) print("语句：" + printSql);
		long startTime=System.currentTimeMillis();	
		try (Connection conn = getConn(); PreparedStatement ps=conn.prepareStatement(sql)) {
			ps.setQueryTimeout(JDBC_QUERY_TIMEOUT);
			if(paramList != null) {
				for(int i = 0; i < paramList.size(); i++) ps.setObject(i + 1, paramList.get(i));
			}
			try(ResultSet rs = ps.executeQuery()){
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				List<UFMap> retList = new ArrayList<UFMap>();
				while(rs.next()){
					UFMap map = new UFMap();
					for(int i = 1; i <= columnCount; i ++) {
						Object obj = rs.getObject(i);
						if(obj instanceof Clob) {//把Clob字段转成String
							Clob clob = (Clob) obj;
							obj = clob.getSubString(1l, (int) clob.length());
						}
						map.put(rsmd.getColumnName(i), obj);
					}
					retList.add(map);
				}
				if(isPrint()) print("本次查到" + retList.size() + "条记录，消耗时间：" + (System.currentTimeMillis()-startTime) + " 毫秒");
				return retList;
			}						
		}
	}
	
	/**
	 * 查询表数据(只返回第一列)
	 * @param sqlKey : sql文件名.sqlName
	 * @param params : 参数(Map类型或单参数类型)
	 * */
	public static <T> List<T> queryList(String sqlKey,Object params) throws Exception{
		List<Object> paramList=new ArrayList<Object>();
		StringBuilder printSql = new StringBuilder();
		String sql=SqlUtil.toJdbc(SqlReader.get(sqlKey), params, paramList, printSql);
		return getList(sql, paramList, printSql.toString());
	}
	public static <T> List<T> getList(String sql, List<Object> paramList, String printSql) throws Exception {
		if(isPrint()) print("语句：" + printSql);
		long startTime=System.currentTimeMillis();
		try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setQueryTimeout(JDBC_QUERY_TIMEOUT);
			if(paramList != null) {
				for(int i = 0; i < paramList.size(); i ++) ps.setObject(i + 1, paramList.get(i));
			}
			try (ResultSet rs = ps.executeQuery()) {
				List<T> retList = new ArrayList<>();
				while(rs.next()) {
					T obj = (T) rs.getObject(1);
					if(obj instanceof Clob) {//把Clob字段转成String
						Clob clob = (Clob) obj;
						obj = (T) clob.getSubString(1l, (int) clob.length());
					}
					retList.add(obj);
				}
				if(isPrint()) print("本次查到" + retList.size() + "条记录，消耗时间：" + (System.currentTimeMillis() - startTime) + " 毫秒");
				return retList;
			}			
		}
	}
	
	/**
	 * 查询表数据(只返回第一行)
	 * @param sqlKey : sql文件名.sqlName
	 * @param params : 参数(Map类型或单参数类型)
	 * */
	public static UFMap queryMap(String sqlKey, Object params) throws Exception{		
		List<Object> paramList = new ArrayList<Object>();
		StringBuilder printSql = new StringBuilder();
		String sql = SqlUtil.toJdbc(SqlReader.get(sqlKey), params, paramList, printSql);
		return getMap(sql, paramList, printSql.toString());
	}
	public static UFMap getMap(String sql, List<Object> paramList, String printSql) throws Exception {
		if(isPrint()) print("语句：" + printSql);
		long startTime=System.currentTimeMillis();
		try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setQueryTimeout(JDBC_QUERY_TIMEOUT);
			if(paramList != null) {
				for(int i = 0; i < paramList.size(); i ++) ps.setObject(i + 1, paramList.get(i));
			}
			try(ResultSet rs = ps.executeQuery()){
				ResultSetMetaData rsmd=rs.getMetaData();
				int columnCount=rsmd.getColumnCount();
				UFMap map=new UFMap();
				if(rs.next()) {
					for(int i=1;i<=columnCount;i++) {
						Object obj = rs.getObject(i);
						if(obj instanceof Clob) {//把Clob字段转成String
							Clob clob = (Clob) obj;
							obj = clob.getSubString(1l, (int) clob.length());
						}
						map.put(rsmd.getColumnName(i), obj);
					}
				}
				if(isPrint()) print("本次查询结果："+map+"，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
				return map;
			}			
		}
	}
	
	/**
	 * 查询表数据(只返回第一行第一列的值)
	 * @param sqlKey : sql文件名.sqlName
	 * @param params : 参数(Map类型或单参数类型)
	 * */
	public static <T> T queryObject(String sqlKey, Object params) throws Exception{		
		List<Object> paramList = new ArrayList<Object>();
		StringBuilder printSql = new StringBuilder();
		String sql=SqlUtil.toJdbc(SqlReader.get(sqlKey), params, paramList, printSql);
		return getObject(sql, paramList, printSql.toString());
	}
	public static <T> T getObject(String sql, List<Object> paramList, String printSql) throws Exception {
		if(isPrint()) print("语句："+printSql);
		long startTime=System.currentTimeMillis();
		try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {	
			ps.setQueryTimeout(JDBC_QUERY_TIMEOUT);
			if(paramList != null) {
				for(int i = 0; i < paramList.size(); i ++) ps.setObject(i + 1, paramList.get(i));
			}
			try (ResultSet rs = ps.executeQuery()) {	
				Object ret = null;
				if(rs.next()) {
					ret = rs.getObject(1);
					if(ret instanceof Clob) {//把Clob字段转成String
						Clob clob = (Clob) ret;
						ret = clob.getSubString(1l, (int) clob.length());
					}
				}
				if(isPrint()) {
					String print = ret == null ? "null" : ret.toString();
					int length = print.length();
					if(length > 1000) {
						print = print.substring(0, 1000) + "...(总长度：" + length + "，过长，日志只打印前1000位)";
					}
					print("本次查询结果：" + print + "，消耗时间：" + (System.currentTimeMillis() - startTime) + " 毫秒");
				}
				return (T) ret;
			}			
		}
	}
	
	/**查询序列*/
	public static String querySequence(String sequenceName) throws Exception{
		String sql="SELECT "+sequenceName+".NEXTVAL FROM DUAL";
		boolean isPrint = isPrint();
		if(isPrint) print("语句:"+sql);
		long startTime=System.currentTimeMillis();
		try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setQueryTimeout(JDBC_QUERY_TIMEOUT);
			try (ResultSet rs = ps.executeQuery()) {
				String ret=null;
				if(rs.next()) ret=rs.getString(1);
				if(isPrint) print("本次查到序列："+ret+"，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
				return ret;
			}
		}
	}
	
	/**MYSQL查询database库中所有表名*/
	public static List<String> getTables(String database) throws Exception{
		return getTables(database, null, true);
	}
	/**
	 * MYSQL查询database库中所有表名
	 * @param likes : 用来对表名做匹配，例："A\\_%"
	 * @param likeOrNot : true-LIKE; false-NOT LIKE
	 */
	public static List<String> getTables(String database, String likes, boolean likeOrNot) throws Exception{
		String sql;
		if(database == null) {
			sql = "SHOW TABLES";
		}else {
			sql = "SHOW TABLES FROM " + database;
		}
		if(likes != null) {
			if(likeOrNot) {
				sql = sql + " WHERE Tables_in_" + database + " LIKE '" + likes + "'";
			}else {
				sql = sql + " WHERE Tables_in_" + database + " NOT LIKE '" + likes + "'";
			}
		}
		boolean isPrint = isPrint();
		if(isPrint) print("语句:" + sql);
		long startTime = System.currentTimeMillis();
		try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setQueryTimeout(JDBC_QUERY_TIMEOUT);
			try (ResultSet rs = ps.executeQuery()) {
				List<String> retList=new ArrayList<>();
				while(rs.next()) {
					retList.add(rs.getString(1));
				}
				if(isPrint) print("本次查到"+retList.size()+"条记录，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
				return retList;
			}
		}
	}

	/**
	 * 运行增、删、改语句，返回成功数量
	 * @param sqlKey : sql文件名.sqlName
	 * @param params : 参数（Map类型或单参数类型）
	 * */
	public static int executeSql(String sqlKey,Object params) throws Exception{
		try (Connection conn = getConn()) {
			return executeSql(sqlKey, params, conn);
		}
	}
	/**
	 * 运行增、删、改语句，返回成功数量
	 * @param sqlKey : sql文件名.sqlName
	 * @param params : 参数（Map类型或单参数类型）
	 * @param conn : 本方法不负责close
	 * */
	public static int executeSql(String sqlKey,Object params,Connection conn) throws Exception{
		List<Object> paramList=new ArrayList<Object>();
		StringBuilder printSql = new StringBuilder();
		String sql=SqlUtil.toJdbc(SqlReader.get(sqlKey), params, paramList, printSql);
		if(isPrint()) print("语句："+printSql);
		long startTime=System.currentTimeMillis();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for(int i=0;i<paramList.size();i++) ps.setObject(i+1, paramList.get(i));
			int ret=ps.executeUpdate();		
			if(isPrint()) print("本次修改成功"+ret+"条记录，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
			return ret;
		}		
	}	
	
	/**
	 * 插表，返回成功或失败
	 * @param tableName : 表名
	 * @param map : Map<列名,值>
	 * */
	public static boolean save(String tableName,UFMap map) throws Exception{
		try (Connection conn = getConn()) {
			return save(tableName, map, conn);
		}
	}
	/**
	 * 插表，返回成功或失败
	 * @param tableName : 表名
	 * @param map : Map<列名,值>
	 * @param conn : 本方法不负责close
	 * */
	public static boolean save(String tableName,UFMap map,Connection conn) throws Exception{
		StringBuilder columns=new StringBuilder();
		StringBuilder values=new StringBuilder();
		StringBuilder printValues=new StringBuilder();
		List<Object> paramList=new ArrayList<Object>();
		map.forEach((columnName, value) -> {
			if(columnName.equals(columnName.toUpperCase())){
				columns.append(","+columnName);
				values.append(",?");
				paramList.add(value);
				if(value==null) printValues.append(",null");
				else printValues.append(",'"+value+"'");			
			}						
		});
		String sql="INSERT INTO "+tableName+"("+columns.substring(1)+") VALUES("+values.substring(1)+")";
		boolean isPrint = isPrint();
		if(isPrint) print("语句："+"INSERT INTO "+tableName+"("+columns.substring(1)+") VALUES("+printValues.substring(1)+")");
		long startTime=System.currentTimeMillis();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for(int i=0;i<paramList.size();i++) ps.setObject(i+1, paramList.get(i));
			int ret=ps.executeUpdate();
			if(isPrint) print("本次插表完成，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
			return ret == 0 ? false : true;
		}		
	}	
	
	/** 
	 * 删除表数据
	 * @param tableName : 表名
	 * @param map : 条件Map<列名,值> ，为null则清空表数据
	 *  */
	public static int delete(String tableName, UFMap map) throws Exception {
		try (Connection conn = getConn()) {
			return delete(tableName, map, conn);
		}
	}
	/** 
	 * 删除表数据
	 * @param tableName : 表名
	 * @param map : 条件Map<列名,值> ，为null则清空表数据
	 * @param conn : 本方法不负责close
	 *  */
	public static int delete(String tableName, UFMap map, Connection conn) throws Exception {
		String sql = null, printSql = null;
		List<Object> paramList = new ArrayList<Object>();
		if(map == null) {
			sql = "TRUNCATE TABLE " + tableName;
			printSql = sql;
		}else {
			StringBuilder sqlSb = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
			StringBuilder printSqlSb = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
			int[] is = {0};
			map.forEach((columnName, value) -> {
				if(columnName.equals(columnName.toUpperCase())){
					if(is[0] == 0) {
						is[0] = 1;
					}else {
						sqlSb.append(" AND ");
						printSqlSb.append(" AND ");
					}			
					if(value==null){
						sqlSb.append(columnName + "IS NULL");
						printSqlSb.append(columnName + "IS NULL");
					}else{
						sqlSb.append(columnName + "=?");
						paramList.add(value);
						printSqlSb.append(columnName + "='" + value + "'");
					}				
				}
			});
			sql = sqlSb.toString();
			printSql = printSqlSb.toString();
		}
		boolean isPrint = isPrint();
		if(isPrint) print("语句：" + printSql);
		long startTime = System.currentTimeMillis();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for(int i = 0; i < paramList.size(); i ++) ps.setObject(i + 1, paramList.get(i));
			int ret = ps.executeUpdate();
			if(isPrint) print("本次共删除" + ret + "条记录，消耗时间：" + (System.currentTimeMillis() - startTime) + " 毫秒");
			return ret;
		}
	}
	
	/** 
	 * 批量运行增、删、改语句<br>
	 * 可执行不同形式的sql语句，速度较慢<br>
	 * 遇异常时回滚，语句未对任何记录生效时也回滚<br>
	 *  */
	public static boolean executeSqls(BatchLists batchLists) throws Exception{
		try (Connection conn = getConn()) {
			return executeSqls(batchLists, conn);
		}
	}
	/** 
	 * 批量运行增、删、改语句<br>
	 * 可执行不同形式的sql语句，速度较慢<br>
	 * 遇异常时回滚，语句未对任何记录生效时也回滚<br>
	 * @param conn : 本方法不负责close
	 *  */
	public static boolean executeSqls(BatchLists batchLists,Connection conn) throws Exception{
		boolean autoCommit = conn.getAutoCommit();
		try{
			conn.setAutoCommit(false);
			List<String> sqlList=batchLists.getSqlList();
			List<List<Object>> paramListList=batchLists.getParamListList();
			List<String> printSqlList = batchLists.getPrintSqlList();
			int length=sqlList.size();
			print("批量执行sql语句开始，本次共执行"+length+"条语句");
			long startTime=System.currentTimeMillis();						
			int num=0;
			for(;num<length;num++){				
				String sql=sqlList.get(num);
				List<Object> paramList=paramListList.get(num);
				if(isPrint()) print("语句："+printSqlList.get(num));
				try (PreparedStatement ps = conn.prepareStatement(sql)) {
					for(int i=0;i<paramList.size();i++) ps.setObject(i+1, paramList.get(i));
					if(ps.executeUpdate()<1) break;
				} catch(Exception e) {
					print(e);
					break;
				}							
			}			
			if(num==length){
				conn.commit();
				print("本次批量结束，共执行成功"+num+"条语句，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
				return true;
			}
			conn.rollback();
			print("本次批量结束，第"+(num+1)+"条语句执行失败，全部回滚，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
			return false;
		}finally{
			conn.setAutoCommit(autoCommit);
		}		
	}	
	
	/** 
	 * 批量运行增、删、改语句<br>
	 * 只能执行同一形式的sql语句，速度较快<br>
	 * 不回滚,遇异常时continue
	 * */
	public static void executeSqlBatch(String sqlKey,List batchParamsList) throws Exception{					
		if(batchParamsList==null||batchParamsList.isEmpty()){
			print("executeSqlBatch批处理参数序列不能为空");
			return;
		}						
		SqlBean sqlBean=SqlReader.get(sqlKey);
		String sql=SqlUtil.resSql(sqlBean.getSql(), sqlBean.getEncList());
		List<String> keyList=sqlBean.getKeyList();
		int length=batchParamsList.size();
		print("批处理开始，本次共执行"+length+"条语句,预编译语句："+sql);
		long startTime=System.currentTimeMillis();		
		try (Connection conn = getConn(); PreparedStatement ps=conn.prepareStatement(sql)) {
			if(batchParamsList.get(0) instanceof Map){
				for(int num=0;num<length;num++){
					try{
						Map paramMap=(Map)batchParamsList.get(num);
						for(int i=0;i<keyList.size();i++) ps.setObject(i+1, paramMap.get(keyList.get(i)));					
						ps.addBatch();
						if((num+1) % JDBC_BATCH_UNIT == 0) {
							try{
								ps.executeBatch();
							}finally{
								ps.clearBatch();
							}					        										        			        
					    }
					}catch(Exception e){
						print(e);
					}					
				}
			}else{				
				for(int num=0;num<length;num++){
					try{
						for(int i=0;i<keyList.size();i++) ps.setObject(i+1, batchParamsList.get(num));										
						ps.addBatch();
						if((num+1) % JDBC_BATCH_UNIT == 0) {
							try{
								ps.executeBatch();
							}finally{
								ps.clearBatch();
							}			        
					    }
					}catch(Exception e){
						print(e);
					}					
				}
			}												
			try {
				ps.executeBatch();
			} catch(Exception e) {
				print(e);
			}
			print("批处理结束，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
		}	
	}
	
	/**
	 * 批量运行insert语句<br>
	 * 只能插同一张表，速度较快<br>
	 * 不回滚,遇异常时continue<br>
	 * 参数：表名,List<Map<列名,值>>
	 * */
	public static void saveBatch(String tableName, List<UFMap> columnDataList) throws Exception{
		if(columnDataList == null || columnDataList.isEmpty()) {
			print("saveBatch批处理参数序列不能为空");
			return;
		}
		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		int length = columnDataList.size();
		List<String> columnList = getColumns(tableName);
		for(String columnName : columnList) {
			if(columnName.equals(columnName.toUpperCase())){
				columns.append(","+columnName);
				values.append(",?");
			}
		}
		String sql="INSERT INTO "+tableName+"("+columns.substring(1)+") VALUES("+values.substring(1)+")";
		print("批处理插库开始，本次共执行" + length + "条语句,预编译语句：" + sql);
		long startTime=System.currentTimeMillis();
		try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
			for (int num = 0; num < length; num ++) {
				try {
					UFMap map = columnDataList.get(num);	
					for(int i = 0;i < columnList.size();i ++) {
						ps.setObject(i + 1, map.get(columnList.get(i)));
					}
					ps.addBatch();
					if((num+1) % JDBC_BATCH_UNIT == 0) {
						try{
							ps.executeBatch();
						}finally{
							ps.clearBatch();
						}				        									        
				    }
				} catch(Exception e) {
					print(e);
				}
			}
			try {
				ps.executeBatch();
			} catch(Exception e) {
				print(e);
			}
			print("批处理结束，消耗时间："+(System.currentTimeMillis()-startTime)+" 毫秒");
		}		
	}
	
	/**最好不要缓存表结构，因为缓存的话，如果后台修改表结构，就必须得重启程序*/
	public static List<String> getColumns(String tableName) throws Exception {
		String sql = "DESC " + tableName;
		return getList(sql, null, sql);
	}
	
	/** 返回的conn务必记得关 */
	public static Connection lockTable(String tableName) throws Exception{
		Connection conn=getConn();
		conn.setAutoCommit(false);
		String sql = "LOCK TABLE "+tableName+" IN EXCLUSIVE MODE";//增删改和FOR UPDATE都要等待
		print("执行锁表语句："+sql);
		try (PreparedStatement ps=conn.prepareStatement(sql)) {
			if(ps.execute()) print(tableName+"锁表成功！");
			else print(tableName+"锁表失败！");
			return conn;
		}						
	}
	
	/** 获取20位新ID */
	public static String getID(){
		return DateUtil.now15() + getSeq();
	}
	/**把20位ID转成yyyy-MM-dd HH:mm:ss*/
	public static String getTimeFromID(String id){
		return new StringBuilder("20").append(id.substring(0, 2)).append("-")
			.append(id.substring(2, 4)).append("-").append(id.substring(4, 6)).append(" ")
			.append(id.substring(6, 8)).append(":").append(id.substring(8, 10)).append(":")
			.append(id.substring(10, 12)).toString();
	}
	
	/** 是否打印sql（只对本线程有效） */
	public static void isPrint(boolean isPrint){		
		if(isPrint) ThreadUtil.remove(ThreadUtil.Isprint);
		else ThreadUtil.set(ThreadUtil.Isprint, isPrint);		
	}
	
	private static boolean isPrint(){
		return ThreadUtil.get(ThreadUtil.Isprint)==null;
	}
	
	/** 专用打印方法 */
	private static void print(Object obj){
		LogUtil.print(obj, Dao.class);
	}
	
}
