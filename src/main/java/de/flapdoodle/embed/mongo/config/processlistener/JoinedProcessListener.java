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
package de.flapdoodle.embed.mongo.config.processlistener;

import java.io.File;

public class JoinedProcessListener implements IMongoProcessListener {

	private final IMongoProcessListener first;
	private final IMongoProcessListener second;

	public JoinedProcessListener(IMongoProcessListener first, IMongoProcessListener second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
	public void onBeforeProcessStart(File dbDir, boolean dbDirIsTemp) {
		first.onBeforeProcessStart(dbDir, dbDirIsTemp);
		second.onBeforeProcessStart(dbDir, dbDirIsTemp);
	}

	@Override
	public void onAfterProcessStop(File dbDir, boolean dbDirIsTemp) {
		first.onAfterProcessStop(dbDir, dbDirIsTemp);
		second.onAfterProcessStop(dbDir, dbDirIsTemp);
	}

}
