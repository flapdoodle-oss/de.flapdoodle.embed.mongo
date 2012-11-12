/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github,Archimedes Trajano (trajano@github)
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
package de.flapdoodle.embed.mongo.config;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor;

/**
 *
 */
public class MongosRuntimeConfig implements IRuntimeConfig {

	private IDirectory directory = new PropertyOrPlatformTempDir();
	private ITempNaming defaultfileNaming = new UUIDTempNaming();
	private ITempNaming executableNaming = defaultfileNaming;
	private ProcessOutput mongodOutputConfig = MongodProcessOutputConfig.getDefaultInstance();
	private ICommandLinePostProcessor commandLinePostProcessor = new ICommandLinePostProcessor.Noop();
	private MongosDownloadConfig downloadConfig=new MongosDownloadConfig();

	@Override
	public IDirectory getTempDirFactory() {
		return directory;
	}
	
	public void setTempDirFactory(IDirectory directory) {
		this.directory = directory;
	}
	
	
	@Override
	public ITempNaming getDefaultfileNaming() {
		return defaultfileNaming;
	}

	public void setDefaultfileNaming(ITempNaming defaultfileNaming) {
		this.defaultfileNaming = defaultfileNaming;
	}

	@Override
	public ITempNaming getExecutableNaming() {
		return executableNaming;
	}

	public void setExecutableNaming(ITempNaming executableNaming) {
		this.executableNaming = executableNaming;
	}

	@Override
	public ProcessOutput getProcessOutput() {
		return mongodOutputConfig;
	}

	public void setProcessOutput(ProcessOutput mongodOutputConfig) {
		this.mongodOutputConfig = mongodOutputConfig;
	}

	public void setCommandLinePostProcessor(ICommandLinePostProcessor commandLinePostProcessor) {
		this.commandLinePostProcessor = commandLinePostProcessor;
	}

	@Override
	public ICommandLinePostProcessor getCommandLinePostProcessor() {
		return commandLinePostProcessor;
	}

	@Override
	public IDownloadConfig getDownloadConfig() {
		return downloadConfig;
	}

	public static MongosRuntimeConfig getInstance(Logger logger) {
		MongosRuntimeConfig ret = new MongosRuntimeConfig();
		ret.setProcessOutput(MongodProcessOutputConfig.getInstance(logger));
		((MongosDownloadConfig)ret.getDownloadConfig()).setProgressListener(new LoggingProgressListener(logger, Level.FINE));
		return ret;
	}
}
