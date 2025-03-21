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
package de.flapdoodle.embed.mongo.client;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ReactiveClientAdapter extends ExecuteMongoClientAction<MongoClient> {

	private final MongoClientSettings clientSettings;

	public ReactiveClientAdapter(MongoClientSettings clientSettings) {
		this.clientSettings = clientSettings;
	}

	@Override
	protected Document resultOfAction(MongoClient client, MongoClientAction.Action action) {
		if (action instanceof MongoClientAction.RunCommand) {
			return get(client.getDatabase(action.database()).runCommand(((MongoClientAction.RunCommand) action).command()));
		}
		throw new IllegalArgumentException("Action not supported: "+action);
	}

	@Override
	protected MongoClient client(ServerAddress serverAddress) {
		return MongoClients.create(MongoClientSettings.builder(clientSettings)
			.applyConnectionString(new ConnectionString("mongodb://"+serverAddress))
			.build());
	}

	@Override
	protected MongoClient client(ServerAddress serverAddress, MongoCredential credential) {
		return MongoClients.create(MongoClientSettings.builder(clientSettings)
			.applyConnectionString(new ConnectionString("mongodb://"+serverAddress))
			.credential(credential)
			.build());
	}

	private static <T> T get(Publisher<T> publisher) {
		CompletableFuture<T> result = new CompletableFuture<>();

		publisher.subscribe(new Subscriber<T>() {
			@Override public void onSubscribe(Subscription s) {
				s.request(1);
			}
			@Override public void onNext(T t) {
				result.complete(t);
			}
			@Override public void onError(Throwable t) {
				result.completeExceptionally(t);
			}
			@Override public void onComplete() {
			}
		});

		try {
			return result.get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
