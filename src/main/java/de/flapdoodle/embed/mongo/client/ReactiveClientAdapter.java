package de.flapdoodle.embed.mongo.client;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.reverse.Listener;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ReactiveClientAdapter extends ExecuteMongoClientAction<MongoClient> {

	@Override
	protected Document resultOfAction(MongoClient client, MongoClientAction.Action action) {
		if (action instanceof MongoClientAction.RunCommand) {
			return get(client.getDatabase(action.database()).runCommand(((MongoClientAction.RunCommand) action).command()));
		}
		throw new IllegalArgumentException("Action not supported: "+action);
	}

	@Override
	protected MongoClient client(ServerAddress serverAddress) {
		return MongoClients.create("mongodb://"+serverAddress);
	}

	@Override
	protected MongoClient client(ServerAddress serverAddress, MongoCredential credential) {
		return MongoClients.create(MongoClientSettings.builder()
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
