package com.datascience.galc;

import com.datascience.core.base.LObject;
import org.apache.log4j.Logger;

import java.util.*;

import com.datascience.core.base.Data;
import com.datascience.core.base.ContValue;
import com.datascience.core.base.AssignedLabel;
import com.datascience.core.base.Worker;

/**
 * @Author: konrad
 */
public class ContinuousIpeirotis {

	private static Logger logger = Logger.getLogger(ContinuousIpeirotis.class);

	protected Data<ContValue> data;
	private Map<LObject<ContValue>, DatumContResults> objectsResults;
	private Map<Worker<ContValue>, WorkerContResults> workersResults;

	public ContinuousIpeirotis(Data<ContValue> data) {
		this.data = data;

		objectsResults = new HashMap<LObject<ContValue>, DatumContResults>();
		workersResults = new HashMap<Worker<ContValue>, WorkerContResults>();

		initWorkers();
		estimateObjectZetas();
	}

	protected Double getLabel(AssignedLabel<ContValue> assign){
		return assign.getLabel().getValue().getValue();
	}

	private void initWorkers() {
		double initial_rho = 0.9;
		for (Worker<ContValue> w : data.getWorkers()) {
			WorkerContResults wcr = new WorkerContResults(w);
			wcr.setEst_rho(initial_rho);
			wcr.computeZetaValues();
			workersResults.put(w, wcr);
		}
	}

	public Double getAverageLabel(LObject<ContValue> object) {
		Set<AssignedLabel<ContValue>> assigns = data.getAssignsForObject(object);
		return getAverageLabel(assigns);
	}

	public Double getAverageLabel(Collection<AssignedLabel<ContValue>> assigns){
		double sum = 0;

		for (AssignedLabel<ContValue> al: assigns) {
			sum += getLabel(al);
		}
		return sum / assigns.size();
	}

	public double estimate(double epsilon, int max_iters) {
		logger.info("GALC estimate START");

		double pastLogLikelihood = Double.POSITIVE_INFINITY;
		double logLikelihood = 0d;

		int round = 0;
		double diff = Double.POSITIVE_INFINITY;
		while (diff > epsilon && round < max_iters) {
			round++;
			Double diffZetas = estimateObjectZetas();
			Double diffWorkers = estimateWorkerRho();
			if (Double.isNaN(diffZetas + diffWorkers)) {
				logger.error("ERROR: Check for division by 0");
				break;
			}
			pastLogLikelihood = logLikelihood;
			logLikelihood = getLogLikelihood();
			diff = Math.abs(logLikelihood - pastLogLikelihood);
		}
		logger.info(String.format("GALC estimate STOP. iterations %d/%d, loglikelihood =%f",
				round, max_iters, logLikelihood));

		return logLikelihood;
	}

	protected double estimate(){
		return estimate(0.00001, 50);
	}

	private double getLogLikelihood() {

		double result = 0d;
		for (WorkerContResults wr: workersResults.values()) {
			Worker<ContValue> workerToIgnore = wr.getWorker();
			for (AssignedLabel<ContValue> al : wr.getZetaValues()) {
				HashMap<LObject<ContValue>, Double> zetas = estimateObjectZetas(workerToIgnore);
				LObject<ContValue> object = al.getLobject();
				Double zeta = zetas.get(object);
				double rho = wr.getEst_rho();
				result += 0.5 * Math.pow(zeta, 2) / (1 - Math.pow(rho, 2)) - Math.log(Math.sqrt(1 - Math.pow(rho, 2)));
			}
		}
		return result;

	}

	private Double estimateObjectZetas() {

		// See equation 9
		double diff = 0.0;
		for (DatumContResults dr: this.objectsResults.values()) {
			Double oldZeta;
			Double newZeta;
			Double zeta = 0.0;
			Double betasum = 0.0;
			LObject<ContValue> object = dr.getObject();
			if(!object.isGold()) {
				oldZeta = dr.getEst_zeta();

				for (AssignedLabel<ContValue> al : data.getAssignsForObject(object)) {
					WorkerContResults wr = workersResults.get(al.getWorker());
					Double b = wr.getBeta();
					Double r = wr.getEst_rho();
					Double z = wr.getZeta(getLabel(al));

					zeta += b * r * z;
					betasum += b;

					//Single Label Worker gives a z=NaN, due to its current est_sigma which is equal to 0
					if (Double.isNaN(zeta))
						logger.warn("["+ z + "," + al.getLabel() + "," + wr.getEst_mu() + "," +
								wr.getEst_sigma() + "," + wr.getWorker().getName()+"], ");

				}

				//d.setEst_zeta(zeta / betasum);
				newZeta = zeta / betasum;
			} else {
				oldZeta = object.getGoldLabel().getValue().getZeta();
				newZeta = oldZeta;
			}

			dr.setEst_zeta(newZeta);

			if (object.isGold())
				continue;
			else if (oldZeta == null) {
				diff += 1;
				continue;
			}

			diff += Math.abs(dr.getEst_zeta() - oldZeta);
		}
		return diff;

	}

	private HashMap<LObject<ContValue>, Double> estimateObjectZetas(Worker<ContValue> workerToIgnore) {

		HashMap<LObject<ContValue>, Double> result = new HashMap<LObject<ContValue>, Double>();

		// See equation 9 without the influence of worker w
		for (LObject<ContValue> object: data.getObjects()) {
			Double newZeta = 0.0;
			Double zeta = 0.0;
			Double betasum = 0.0;

			for (AssignedLabel<ContValue> al : data.getAssignsForObject(object)) {
				Worker<ContValue>  worker = al.getWorker();
				if(worker.equals(workerToIgnore))
					continue;
				WorkerContResults wr = workersResults.get(worker);
				Double b = wr.getBeta();
				Double r = wr.getEst_rho();
				Double z = wr.getZeta(getLabel(al));
				zeta += b * r * z;
				betasum += b;
			}

			//d.setEst_zeta(zeta / betasum);
			newZeta = zeta / betasum;
			result.put(object, newZeta);

			//if (Double.isNaN(newZeta)) logger.info("estimateObjectZetas NaNbug@: " + zeta +","+ betasum + "," +d.getName());

		}

		return result;

	}

	private double estimateWorkerRho() {

		// See equation 10

		double diff = 0.0;
		for (WorkerContResults wr : workersResults.values()) {
			Worker workerToIgnore = wr.getWorker();
			Double sum_prod = 0.0;
			Double sum_zi = 0.0;
			Double sum_zij = 0.0;

			double oldrho = wr.getEst_rho();
			for (AssignedLabel<ContValue> al : wr.getZetaValues()) {

				HashMap<LObject<ContValue>, Double> zeta = estimateObjectZetas(workerToIgnore);

				LObject<ContValue> object = al.getLobject();
				Double z_i = zeta.get(object);
				double z_ij = getLabel(al);

				sum_prod += z_i * z_ij;
				sum_zi += z_i * z_i;
				sum_zij += z_ij * z_ij;
			}
			double rho = sum_prod / Math.sqrt(sum_zi * sum_zij);

			if (Double.isNaN(rho)) {
				logger.warn("estimateWorkerRho NaNbug@: " + sum_zi +","+ sum_zij + ","
						+ workerToIgnore.getName());
				rho = 0.0;
			}
			wr.setEst_rho(rho);

			diff += Math.abs(wr.getEst_rho() - oldrho);
		}
		return diff;
	}

	public Map<LObject<ContValue>, DatumContResults> getObjectsResults() {
		return objectsResults;
	}

	public Map<Worker<ContValue>, WorkerContResults> getWorkersResults() {
		return workersResults;
	}
}