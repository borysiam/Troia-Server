/*******************************************************************************
 * Copyright (c) 2012 Panagiotis G. Ipeirotis & Josh M. Attenberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.datascience.gal.dawidSkeneProcessors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.apache.log4j.Logger;



public class DawidSkeneProcessorManager extends Thread  {
    

    public DawidSkeneProcessorExecutor(int threadPoolSize,int sleepPeriod){
	this.processorQueue = new HashMap<String,Queue<DawidSkeneProcessor>>();
	this.executor =  Executors.newFixedThreadPool(threadPoolSize);
	this.stopped = false;
	this.sleepPeriod = sleepPeriod;
    }

    @Override
	public void run(){
	while(!this.stopped){
	    this.executeProcessors();
	    try{
		this.sleep(this.sleepPeriod);
	    }catch(InterruptedException e){
		//THIS CAN BE EMPTY
	    }
	}
    }


    /**
     * This function adds a new processor to manager
     * @param processor Processor that's going to be added to manager
     */
    public void addProcessor(DawidSkeneProcessor processor){
	if(!this.processorQueue.containsKey(processor.getDawidSkeneId()){
		this.processorQueue.put(processor.getDawidSkeneId(),new ConcurentLinkedQueue<DawidSkeneProcessor>());	
	    }
	    this.processorQueue.get(processor.getDawidSkeneId()).put(processor.getDawidSkeneId(),processor);
	    }
    }

    private void executeProcessors(){
	Collection<Queue<DawidSkeneProcessor> queues = this.processorQueue.entrySet();
	for (Queue<DawidSkeneProcessor> queue : queues) {
	    if(queue.peek()!=null&&queue.peek().isProcessed()){
		queue.pool();
		if(queue.peek()!=null){
		    this.executor.execute(queue.peek());
		}
	    }
	}
    }

    /**
     * Map that holds queues of processors for each project
     */
    private Map<String,Queue<DawidSkeneProcessor>> processorQueue;
    
    /**
     * @return Map that holds queues of processors for each project
     */
    public Map<String,Queue<DawidSkeneProcessor>> getProcessorQueue() {
	return processorQueue;
    }
    
    /**
     * @param processorQueue Map that holds queues of processors for each project
     */
    public void setProcessorQueue(Map<String,Queue<DawidSkeneProcessor>> processorQueue) {
	this.processorQueue = processorQueue;
    }

    /**
     * Executor that executes processor threads
     */
    private ExecutorService executor;
    
    /**
     * @return Executor that executes processor threads
     */
    public ExecutorService getExecutor() {
	return executor;
    }
    
    /**
     * @param executor Executor that executes processor threads
     */
    public void setExecutor(ExecutorService executor) {
	this.executor = executor;
    }

/**
 * Set to true if manager is stopped
 */
    private boolean stopped;
    
    /**
     * @return Set to true if manager is stopped
     */
    public boolean isStopped() {
	return stopped;
    }
    
    /**
     * @param stopped Set to true if manager is stopped
     */
    public void stop() {
	this.stopped = true;
    }

/**
 * Time for wtih manager thread will be put to sleep between executions
 */
    private int sleepPeriod;
    
    /**
     * @return Time for wtih manager thread will be put to sleep between executions
     */
    public int getSleepPeriod() {
	return sleepPeriod;
    }
    
    /**
     * @param sleepPeriod Time for wtih manager thread will be put to sleep between executions
     */
    public void setSleepPeriod(int sleepPeriod) {
	this.sleepPeriod = sleepPeriod;
    }


    private static Logger logger = Logger.getLogger(DawidSkeneProcessorManager.class);
}