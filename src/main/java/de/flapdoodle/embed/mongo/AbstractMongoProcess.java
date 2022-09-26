/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github,Archimedes Trajano	(trajano@github)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.embed.mongo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flapdoodle.embed.mongo.config.MongoCommonConfig;
import de.flapdoodle.embed.mongo.runtime.Mongod;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.process.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.io.LogWatchStreamProcessor;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamToLineProcessor;
import de.flapdoodle.embed.process.runtime.AbstractProcess;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.IStopable;
import de.flapdoodle.embed.process.runtime.ProcessControl;


public abstract class AbstractMongoProcess<T extends MongoCommonConfig, E extends Executable<T, P>, P extends IStopable> extends AbstractProcess<T, E, P> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMongoProcess.class);
	
	private boolean stopped;
	
	public AbstractMongoProcess(Distribution distribution, T config, RuntimeConfig runtimeConfig, E executable)
			throws IOException {
		super(distribution, config, runtimeConfig, executable);
	}

	@Override
	protected final void onAfterProcessStart(ProcessControl process, RuntimeConfig runtimeConfig) {
		ProcessOutput outputConfig = runtimeConfig.processOutput();
		LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(successMessage(), knownFailureMessages(),
				StreamToLineProcessor.wrap(outputConfig.output()));
		Processors.connect(process.getReader(), logWatch);
		Processors.connect(process.getError(), StreamToLineProcessor.wrap(outputConfig.error()));
		long startupTimeout = getConfig().timeout().getStartupTimeout();
		logWatch.waitForResult(startupTimeout);
		if (logWatch.isInitWithSuccess()) {
			setProcessId(Mongod.getMongodProcessId(logWatch.getOutput(), -1));
		} else {
			String failureFound = logWatch.getFailureFound();
			if (failureFound==null) {
				failureFound="\n" +
						"----------------------\n" +
						"Hmm.. no failure message.. \n" +
						"...the cause must be somewhere in the process output\n" +
						"----------------------\n" +
						""+logWatch.getOutput();
			}
			try {
				// Process could be finished with success here! In this case no need to throw an exception!
				if(process.waitFor(getConfig().timeout().getStartupTimeout()) != 0){
					throw new RuntimeException("Could not start process: "+failureFound);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Could not start process: "+failureFound, e);
			}
		}
	}

	protected String successMessage() {
	  // old: waiting for connections on port
		// since 4.4.5: Waiting for connections
		return "aiting for connections";
	}
	
	private Set<String> knownFailureMessages() {
		HashSet<String> ret = new HashSet<>();
		ret.add("failed errno");
		ret.add("ERROR:");
		ret.add("error command line");
		return ret;
	}

	@Override
	public void stopInternal() {
		synchronized (this) {
			if (!stopped) {

				stopped = true;

				LOGGER.debug("try to stop mongod");
				if (!sendStopToMongoInstance()) {
					LOGGER.warn("could not stop mongod with db command, try next");
					if (!sendKillToProcess()) {
						LOGGER.warn("could not stop mongod, try next");
						if (!tryKillToProcess()) {
							LOGGER.warn("could not stop mongod the second time, try one last thing");
						}
					}
				}

				stopProcess();
			}
		}
	}
	
	@Override
	protected void cleanupInternal() {
		deleteTempFiles();
	}

	protected void deleteTempFiles() {

	}

	protected final boolean sendStopToMongoInstance() {
		try {
			InetAddress serverAddress = getConfig().net().getServerAddress();
			int port = getConfig().net().getPort();
			return Mongod.sendShutdownLegacy(serverAddress,port) || Mongod.sendShutdown(serverAddress, port);
		} catch (UnknownHostException e) {
			LOGGER.error("sendStop", e);
		}
		return false;
	}

}
