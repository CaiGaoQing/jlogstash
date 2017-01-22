/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtstack.jlogstash.assembly;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dtstack.jlogstash.assembly.disruptor.JDisruptor;
import com.dtstack.jlogstash.assembly.pthread.FilterHandler;
import com.dtstack.jlogstash.assembly.pthread.InputThread;
import com.dtstack.jlogstash.assembly.pthread.OutputHandler;
import com.dtstack.jlogstash.classloader.JarClassLoader;
import com.dtstack.jlogstash.configs.YamlConfig;
import com.dtstack.jlogstash.exception.LogstashException;
import com.dtstack.jlogstash.factory.InputFactory;
import com.dtstack.jlogstash.factory.InstanceFactory;
import com.dtstack.jlogstash.inputs.BaseInput;
import com.dtstack.jlogstash.outputs.BaseOutput;
import com.google.common.collect.Lists;

/**
 * 
 * Reason: TODO ADD REASON(可选)
 * Date: 2016年8月31日 下午1:25:11
 * Company: www.dtstack.com
 * @author sishu.yss
 *
 */
public class AssemblyPipeline {
	
	private static Logger logger = LoggerFactory.getLogger(AssemblyPipeline.class);
				
	private List<List<BaseOutput>> allBaseOutputs = Lists.newCopyOnWriteArrayList();
	
	private JarClassLoader JarClassLoader = new JarClassLoader();
	

	/**
	 * 组装管道
	 * @param cmdLine
	 * @return 
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void assemblyPipeline() throws Exception{
			logger.debug("load config start ...");
			Map configs = new YamlConfig().parse(CmdLineParams.getConfigFilePath());
			logger.debug(configs.toString());
			logger.debug("load plugin...");
			InstanceFactory.setClassCloaders(JarClassLoader.loadJar());
			List<Map> inputs = (List<Map>) configs.get("inputs");
			if(inputs==null||inputs.size()==0){
				throw new LogstashException("input plugin is not empty");
			}
			List<Map> outputs = (List<Map>) configs.get("outputs");
			if(outputs==null||outputs.size()==0){
				throw new LogstashException("output plugin is not empty");
			}
		    List<Map> filters = (List<Map>) configs.get("filters");
		    JDisruptor inputToFilterDisruptor = initInputToFilterDisruptor(filters);
		    JDisruptor filterToOutputDisruptor = filterToOutputDisruptor(outputs);
		    if(inputToFilterDisruptor!=null){
		    	FilterHandler.setFilterToOutputDisruptor(filterToOutputDisruptor);
		    	inputToFilterDisruptor.start();
		    }
		    filterToOutputDisruptor.start();
		    List<BaseInput> baseInputs =InputFactory.getBatchInstance(inputs,inputToFilterDisruptor!=null?inputToFilterDisruptor:filterToOutputDisruptor);
			InputThread.initInputThread(baseInputs);
    		//add shutdownhook
    		ShutDownHook shutDownHook = new ShutDownHook(inputToFilterDisruptor,filterToOutputDisruptor,baseInputs,allBaseOutputs);
    		shutDownHook.addShutDownHook();
	}
	
	
	@SuppressWarnings("rawtypes")
	private JDisruptor initInputToFilterDisruptor(List<Map> filters) throws LogstashException, Exception{
	    if(filters!=null&&filters.size()>0){
		    int filterWorks = CmdLineParams.getFilterWork();
		    return new JDisruptor(FilterHandler.getArrayHandlerInstance(filters,filterWorks),CmdLineParams.getFilterRingBuffer(),CmdLineParams.getWaitStrategy());
	    }
	    return null;
	}
	
	@SuppressWarnings("rawtypes")
	private JDisruptor filterToOutputDisruptor(List<Map> outputs) throws LogstashException, Exception{
	    int outputWorks = CmdLineParams.getOutputWork();
	    JDisruptor filterToOutputDisruptor = new JDisruptor(OutputHandler.getArrayHandlerInstance(outputs,outputWorks,allBaseOutputs),CmdLineParams.getOutputRingBuffer(),CmdLineParams.getWaitStrategy());
	    return filterToOutputDisruptor;
	}
}