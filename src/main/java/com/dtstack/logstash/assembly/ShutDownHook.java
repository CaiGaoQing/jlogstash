package com.dtstack.logstash.assembly;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dtstack.logstash.assembly.qlist.QueueList;
import com.dtstack.logstash.inputs.BaseInput;
import com.dtstack.logstash.outputs.BaseOutput;

/**
 * 
 * Reason: TODO ADD REASON(可选)
 * Date: 2016年8月31日 下午1:25:34
 * Company: www.dtstack.com
 * @author sishu.yss
 *
 */
public class ShutDownHook {
	
	private Logger logger = LoggerFactory.getLogger(ShutDownHook.class);
	
    private QueueList initInputQueueList;
    
    private QueueList initOutputQueueList;

    private List<BaseInput> baseInputs; 
    
    private List<BaseOutput> baseOutputs;
        
    public ShutDownHook(QueueList initInputQueueList,QueueList initOutputQueueList,List<BaseInput> baseInputs,List<BaseOutput> baseOutputs){
    	this.initInputQueueList = initInputQueueList;
    	this.initOutputQueueList = initOutputQueueList;
    	this.baseInputs  = baseInputs;
    	this.baseOutputs = baseOutputs;
    }
	
	public void addShutDownHook(){
	   Thread shut =new Thread(new ShutDownHookThread());
	   shut.setDaemon(true);
	   Runtime.getRuntime().addShutdownHook(shut);
	   logger.debug("addShutDownHook success ...");
	}
	
	class ShutDownHookThread implements Runnable{
		private void inputRelease(){
			try{
				if(baseInputs!=null){
					for(BaseInput input:baseInputs){
						input.release();
					}
				}
				logger.warn("inputRelease success...");
			}catch(Exception e){
				logger.error("inputRelease error:{}",e.getMessage());
			}
		}
		
		private void outPutRelease(){
			try{
				if(baseOutputs!=null){
					for(BaseOutput outPut:baseOutputs){
						outPut.release();
					}
				}
				logger.warn("outPutRelease success...");
			}catch(Exception e){
				logger.error("outPutRelease error:{}",e.getMessage());
			}
		}
		

		@Override
		public void run() {
			// TODO Auto-generated method stub
			inputRelease();
			if(initInputQueueList!=null)initInputQueueList.queueRelease();
			if(initOutputQueueList!=null)initOutputQueueList.queueRelease();
			outPutRelease();	
		}
	}
}
