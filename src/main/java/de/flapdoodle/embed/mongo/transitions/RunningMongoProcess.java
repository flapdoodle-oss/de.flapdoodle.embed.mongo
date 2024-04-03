/*
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
package de.flapdoodle.embed.mongo.transitions;

import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.runtime.Mongod;
import de.flapdoodle.embed.process.config.SupportConfig;
import de.flapdoodle.embed.process.io.*;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import de.flapdoodle.embed.process.runtime.Processes;
import de.flapdoodle.embed.process.types.RunningProcessFactory;
import de.flapdoodle.embed.process.types.RunningProcessImpl;
import de.flapdoodle.os.Platform;
import de.flapdoodle.types.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public abstract class RunningMongoProcess extends RunningProcessImpl {

	private static Logger LOGGER= LoggerFactory.getLogger(RunningMongodProcess.class);

	private final String commandName;
	private final SupportConfig supportConfig;
	private final Platform platform;
	private final StreamProcessor commandOutput;
	private final int mongoProcessId;
	private final InetAddress serverAddress;
	private final int port;

	private boolean shutDownCommandAlreadyExecuted=false;

	protected RunningMongoProcess(
		String commandName,
		ProcessControl process,
		Path pidFile,
		long timeout,
		Runnable onStop,
		SupportConfig supportConfig,
		Platform platform,
		Net net,
		StreamProcessor commandOutput,
		int mongoProcessId
	) {
		super(process, pidFile, timeout, onStop);
		this.commandName = commandName;
		this.supportConfig = supportConfig;
		this.platform = platform;
		this.commandOutput = commandOutput;
		this.mongoProcessId = mongoProcessId;
		this.serverAddress = Try.get(net::getServerAddress);
		this.port = net.getPort();
	}

	public ServerAddress getServerAddress() {
		return ServerAddress.of(serverAddress, port);
	}

	@Override
	public int stop() {
		try {
			stopInternal();
		} finally {
			return super.stop();
		}
	}

	//	@Override
	private void stopInternal() {
		if (isAlive()) {
			LOGGER.debug("try to stop "+commandName);
			if (!shutDownCommandAlreadyExecuted && !sendStopToMongoInstance()) {
				LOGGER.warn("could not stop "+commandName+" with db command, try next");
				if (!sendKillToProcess()) {
					LOGGER.warn("could not stop "+commandName+", try next");
					if (!sendTermToProcess()) {
						LOGGER.warn("could not stop "+commandName+", try next");
						if (!tryKillToProcess()) {
							LOGGER.warn("could not stop "+commandName+" the second time, try one last thing");
						}
					}
				}
			}
		}
	}

	private long getProcessId() {
		return mongoProcessId;
	}

	protected boolean sendKillToProcess() {
		return getProcessId() > 0 && Processes.killProcess(supportConfig, platform,
			StreamToLineProcessor.wrap(commandOutput), getProcessId());
	}

	protected boolean sendTermToProcess() {
		return getProcessId() > 0 && Processes.termProcess(supportConfig, platform,
			StreamToLineProcessor.wrap(commandOutput), getProcessId());
	}

	protected boolean tryKillToProcess() {
		return getProcessId() > 0 && Processes.tryKillProcess(supportConfig, platform,
			StreamToLineProcessor.wrap(commandOutput), getProcessId());
	}

	protected final boolean sendStopToMongoInstance() {
		return Mongod.sendShutdownLegacy(serverAddress, port)
			|| Mongod.sendShutdown(serverAddress, port);
	}

	public void shutDownCommandAlreadyExecuted() {
		this.shutDownCommandAlreadyExecuted = true;
	}

	interface InstanceFactory<T extends RunningMongoProcess> {
		T create(ProcessControl process, Path pidFile, long timeout, Runnable closeAllOutputs, SupportConfig supportConfig, Platform platform, Net net, StreamProcessor commands, int pid);
	}

	static <T extends RunningMongoProcess> RunningProcessFactory<T> factory(InstanceFactory<T> instanceFactory, long startupTimeout, SupportConfig supportConfig, Platform platform, Net net) {
		return (process, processOutput, pidFile, timeout) -> {

//			LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(successMessage(), knownFailureMessages(),
//				StreamToLineProcessor.wrap(processOutput.output()));
			LOGGER.trace("setup logWatch");
			SuccessMessageLineListener logWatch = errorMessageAwareLogWatch();

			LOGGER.trace("connect io");
			ReaderProcessor output = Processors.connect(process.getReader(), new ListeningStreamProcessor(StreamToLineProcessor.wrap(processOutput.output()), logWatch::inspect));
			ReaderProcessor error = Processors.connect(process.getError(), new ListeningStreamProcessor(StreamToLineProcessor.wrap(processOutput.error()), logWatch::inspect));
			Runnable closeAllOutputs = () -> {
				LOGGER.trace("ReaderProcessor.abortAll");
				ReaderProcessor.abortAll(output, error);
				LOGGER.trace("ReaderProcessor.abortAll done");
			};

			LOGGER.trace("waitForResult");
			logWatch.waitForResult(startupTimeout);
			LOGGER.trace("check if successMessageFound");
			if (logWatch.successMessageFound()) {
				LOGGER.trace("get processId");
				int pid = Mongod.getMongodProcessId(logWatch.allLines(), -1);
				LOGGER.trace("return RunningMongodProcess");
				return instanceFactory.create(process, pidFile, timeout, closeAllOutputs, supportConfig, platform, net, processOutput.commands(), pid);

			} else {
				String failureFound = logWatch.errorMessage().isPresent()
					? logWatch.errorMessage().get()
					: "\n" +
					"----------------------\n" +
					"Hmm.. no failure message.. \n" +
					"...the cause must be somewhere in the process output\n" +
					"----------------------\n" +
					""+logWatch.allLines();

				return Try.<T, RuntimeException>supplier(() -> {
						throw new RuntimeException("Could not start process: "+failureFound);
					})
					.andFinally(() -> {
						process.stop(timeout);
					})
					.andFinally(closeAllOutputs)
					.get();
			}
		};
	}

	// VisibleForTesting
	static SuccessMessageLineListener errorMessageAwareLogWatch() {
		return SuccessMessageLineListener.of(successMessage(), knownFailureMessages(), "error");
	}

	private static List<String> successMessage() {
		// old: waiting for connections on port
		// since 4.4.5: Waiting for connections
		return Arrays.asList("aiting for connections");
	}

	private static List<String> knownFailureMessages() {
		return Arrays.asList(
			"(?<error>failed errno)",
			"ERROR:(?<error>.*)",
			"(?<error>error command line)",
			"(?<error>Error parsing command line:.*)",
			"(?<error>Address already in use)",
			"(?<error>error while loading shared libraries:.*)",
			"(?<error>SSLEAY32\\.dll was not found)",
			"(?<error>LIBEAY32\\.dll was not found)",
			"(?<error>the code execution cannot proceed because.*)",
			"(?<error>An attempt was made to access a socket in a way forbidden by its access permissions)"
		);
	}

}
