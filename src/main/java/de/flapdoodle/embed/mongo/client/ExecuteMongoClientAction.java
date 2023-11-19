package de.flapdoodle.embed.mongo.client;

import com.mongodb.MongoCredential;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import org.bson.Document;

import java.io.Closeable;
import java.io.IOException;

public abstract class ExecuteMongoClientAction<C extends Closeable> {
	public void execute(RunningMongodProcess runningMongodProcess, MongoClientAction action) {
		try (C client = action.credentials()
			.map(c -> client(runningMongodProcess.getServerAddress(),
				MongoCredential.createCredential(c.username(), c.database(), c.password().toCharArray())))
			.orElseGet(() -> client(runningMongodProcess.getServerAddress()))) {

			action.onResult()
				.accept(resultOfAction(client, action.action()));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (RuntimeException rx) {
			action.onError().accept(rx);
		}
	}

	protected abstract C client(ServerAddress serverAddress);

	protected abstract C client(ServerAddress serverAddress, MongoCredential credential);

	protected abstract Document resultOfAction(C client, MongoClientAction.Action action);
}
