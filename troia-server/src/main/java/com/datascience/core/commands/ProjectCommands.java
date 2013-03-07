package com.datascience.core.commands;

import com.datascience.core.base.*;
import com.datascience.executor.JobCommand;

import java.util.*;

/**
 *
 * @author artur
 */
public class ProjectCommands {
	
	static public class Compute extends JobCommand<Object, Project> {

		int iterations;
		double epsilon;
		
		public Compute(int iterations, double epsilon){
			super(true);
			this.iterations = iterations;
			this.epsilon = epsilon;
		}
		
		@Override
		protected void realExecute() {
			project.getAlgorithm().estimate(epsilon, iterations);
			setResult("Computation done");
		}
	}
	
	static public class GetProjectInfo extends JobCommand<Map<String, String>, Project> {

		public GetProjectInfo(){
			super(false);
		}
		
		@Override
		protected void realExecute() {
			setResult(project.getInfo());
		}
	}
}