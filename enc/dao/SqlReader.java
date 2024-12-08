package dao;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.LogUtil;
import util.PropUtil;
import util.StringUtil;

public class SqlReader {

	public static final Map<String, SqlBean> sqlMap = new HashMap<String, SqlBean>();
	
	public static SqlBean get(String sqlKey) {
		return sqlMap.get(sqlKey);
	}

	/** 加载sql文件 */
	public static void init() {
		try {
			Path dir = Paths.get(PropUtil.FILE_DIR, "sql");			
			for (Path file : Files.newDirectoryStream(dir)) {
				String fileName = file.getFileName().toString();
				if (fileName.endsWith(".sql")) {
					readSql(file, fileName.substring(0, fileName.length() - 3));
				}
			}
			sqlMap.forEach((key, sqlBean) -> {
				int control = 0;
				String sql = completeSql(sqlBean.getSql(), key.split("\\.")[0] + ".", control);
				sqlBean.setSql(sql);
			});
			sqlMap.forEach((key, sqlBean) -> {
				String sql = sqlBean.getSql();
				if (sql.contains("'")) {
					List<String> encList = new ArrayList<String>();
					sql = SqlUtil.encSql(sql, encList);
					sqlBean.setEncList(encList);
				}
				if (sql.contains("{")) {
					List<String> keyList = new ArrayList<String>();
					sql = toJdbcSql(sql, keyList);
					sqlBean.setKeyList(keyList);
				}
				if (sql.contains("<<")) {
					int len = StringUtil.findSubStrCount(sql, "<<");
					List<String> replaceList = new ArrayList<String>();
					for (int i = 0; i < len; i++) {
						int leftIndex = StringUtil.findSubStrIndex(sql, "<<", i + 1);
						int rightIndex = sql.indexOf(">>", leftIndex);
						replaceList.add(sql.substring(leftIndex, rightIndex + 2));
					}
					sqlBean.setReplaceList(replaceList);
				}
				sqlBean.setSql(sql);
				if (sql.contains("[")) {
					int len = StringUtil.findSubStrCount(sql, "[");
					Map<Integer, String> checkMap = new HashMap<Integer, String>();
					for (int i = 0; i < len; i++) {
						int leftIndex = StringUtil.findSubStrIndex(sql, "[", i + 1);
						int rightIndex = sql.indexOf("]", leftIndex);
						int markIndex = sql.indexOf("?", leftIndex);// 以第一个问号为判断的依据
						checkMap.put(StringUtil.findSubStrCount(sql.substring(0, markIndex), "?"),
								sql.substring(leftIndex, rightIndex + 1));
					}
					sqlBean.setCheckMap(checkMap);
				}
			});
			LogUtil.print("加载sql文件成功,共" + sqlMap.size() + "条语句", "");
		}catch(Exception e) {
			LogUtil.print(e);
		}		
	}

	/** 读取单个sql文件 */
	private static void readSql(Path sqlFile, String preSqlKey) throws Exception {
		if("root.".equals(preSqlKey)) preSqlKey = "";
		try (BufferedReader br =Files.newBufferedReader(sqlFile)) {
			String sqlNamePrefix = "\"SQLNAME=";
			StringBuilder sqlName = new StringBuilder();
			StringBuilder sql = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith(sqlNamePrefix)) {
					if (sql.length() > 0 && sqlName.length() > 0) {
						SqlBean sqlBean = new SqlBean();
						sqlBean.setSql(sql.toString().trim().replaceAll("\\s{1,}", " "));
						sqlMap.put(preSqlKey + sqlName, sqlBean);
					}
					sqlName.delete(0, sqlName.length());
					sql.delete(0, sql.length());
					int preLen = sqlNamePrefix.length();
					sqlName.append(line.substring(preLen, line.indexOf("\"", preLen)).trim());
				} else {
					sql.append(line.split("--")[0].trim() + " ");
				}
			}
			if (sql.length() > 0) {
				SqlBean sqlBean = new SqlBean();
				sqlBean.setSql(sql.toString().trim().replaceAll("\\s{1,}", " "));
				sqlMap.put(preSqlKey + sqlName, sqlBean);
			}
		}
	}

	/** 补全嵌套的sql语句 */
	private static String completeSql(String sql, String keyPrefix, int control) {
		if (control++ > 2) {// 控制sql最多能嵌套三层
			return sql;
		}
		List<String> encList = new ArrayList<String>();
		sql = SqlUtil.encSql(sql, encList);
		String[] sqlChips = sql.split("\"");
		StringBuilder sb = new StringBuilder(sqlChips[0]);
		for (int i = 1; i < sqlChips.length; i++) {
			if ((i + 1) % 2 == 0) {
				String sqlChip = sqlChips[i];
				if (!sqlChip.contains(".")) {
					sqlChip = keyPrefix + sqlChip;
				}
				sb.append(completeSql(sqlMap.get(sqlChip).getSql(), sqlChip.substring(0, sqlChip.indexOf(".") + 1),
						control));
			} else {
				sb.append(sqlChips[i]);
			}
		}
		return SqlUtil.resSql(sb.toString(), encList);
	}

	private static String toJdbcSql(String sql, List<String> keyList) {
		String[] splitSqls = sql.split("\\{");
		String retSql = splitSqls[0];
		for (int i = 1; i < splitSqls.length; i++) {
			String[] splitSql = splitSqls[i].split("\\}");
			keyList.add(splitSql[0]);
			if (splitSql.length == 2) {
				retSql += "?" + splitSql[1];
			} else {
				retSql += "?";
			}
		}
		return retSql;
	}

}
