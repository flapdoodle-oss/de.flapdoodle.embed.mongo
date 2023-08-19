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
package de.flapdoodle.embed.mongo.examples;

import de.flapdoodle.embed.process.io.StreamProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

// ...
public class FileStreamProcessor implements StreamProcessor {

	private final FileOutputStream outputStream;

	public FileStreamProcessor(File file) throws FileNotFoundException {
		outputStream = new FileOutputStream(file);
	}

	@Override
	public void process(String block) {
		try {
			outputStream.write(block.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onProcessed() {
		try {
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
// ...
// <-