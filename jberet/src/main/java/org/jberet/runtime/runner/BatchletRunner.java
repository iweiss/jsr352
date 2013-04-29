/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.runtime.runner;

import java.util.concurrent.TimeUnit;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.Batchlet;
import org.jberet.job.Collector;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.context.JobContextImpl;
import org.jberet.runtime.context.StepContextImpl;
import org.jberet.util.ConcurrencyService;

import static org.jberet.util.BatchLogger.LOGGER;

public final class BatchletRunner extends AbstractRunner<StepContextImpl> implements Runnable {
    private Batchlet batchlet;
    private StepExecutionRunner stepRunner;
    private PartitionCollector collector;

    public BatchletRunner(StepContextImpl stepContext, CompositeExecutionRunner enclosingRunner, StepExecutionRunner stepRunner, Batchlet batchlet) {
        super(stepContext, enclosingRunner);
        this.stepRunner = stepRunner;
        this.batchlet = batchlet;
    }

    @Override
    public void run() {
        try {
            if (stepRunner.dataQueue != null) {
                Collector collectorConfig = batchContext.getStep().getPartition().getCollector();
                if (collectorConfig != null) {
                    collector = batchContext.getJobContext().createArtifact(collectorConfig.getRef(), collectorConfig.getProperties(), batchContext);
                }
            }
            JobContextImpl jobContext = batchContext.getJobContext();
            final javax.batch.api.Batchlet batchletObj = jobContext.createArtifact(batchlet.getRef(), batchlet.getProperties(), batchContext);

            ConcurrencyService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        batchContext.getJobContext().getJobExecution().awaitStop(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT, TimeUnit.SECONDS);
                        if (batchContext.getBatchStatus() == BatchStatus.STARTED) {
                            batchContext.setBatchStatus(BatchStatus.STOPPING);
                            batchletObj.stop();
                        }
                    } catch (Exception e) {
                        LOGGER.failToStopJob(e, batchContext.getJobContext().getJobName(), batchContext.getStepName(), batchletObj);
                    }
                }
            });

            String exitStatus = batchletObj.process();
            batchContext.setExitStatus(exitStatus);
            if (collector != null) {
                stepRunner.dataQueue.put(collector.collectPartitionData());
            }
        } catch (Throwable e) {
            if (collector != null) {
                try {
                    stepRunner.dataQueue.put(collector.collectPartitionData());
                } catch (Exception e1) {
                    //ignore
                }
            }
            LOGGER.failToRunBatchlet(e, batchlet);
            batchContext.setBatchStatus(BatchStatus.FAILED);
        } finally {
            try {
                if (stepRunner.dataQueue != null) {
                    stepRunner.dataQueue.put(batchContext.getStepExecution());
                }
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

}