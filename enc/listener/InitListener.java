package listener;

import static util.LogUtil.SEPARATOR;
import static util.LogUtil.out;
import static util.LogUtil.print;
import static util.LogUtil.writer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import uf.Cron;
import uf.UF;
import util.DateUtil;
import dao.Dao;
import dao.SqlReader;

@WebListener
public class InitListener implements ServletContextListener {
	
	/** 系统启动时调用 */
    public void contextInitialized(ServletContextEvent arg0)  {
    	try{
    		String startLogo = "▓   ▓ ▓▓▓▓▓" + SEPARATOR
			   		 		 + "▓   ▓ ▓" + SEPARATOR
			   		 		 + "▓   ▓ ▓▓▓▓" + SEPARATOR
			   		 		 + "▓▓▓▓▓ ▓";
    		out(startLogo);
    		SqlReader.init();
    		Dao.init();
    		Cron.init();
	    	print("------------------------------------UF系统启动成功！", "");
	    }catch(Exception e){
			print(e);
			System.exit(0);
		}
    }

	/** 系统停止时调用 */
    public void contextDestroyed(ServletContextEvent arg0)  {    	
    	try {    		
			UF.THREADS.shutdown();
			Cron.close();
			String info=DateUtil.now23()+"------------------------------------UF系统已停止服务！"+SEPARATOR;
			writer.write(info);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}   	
    }	   

}
