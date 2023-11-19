package de.flapdoodle.embed.mongo.client;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.reverse.Listener;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public class SyncClientAdapter extends ExecuteMongoClientAction<MongoClient> {
	@Override
	protected MongoClient client(ServerAddress serverAddress) {
		return MongoClients.create("mongodb://" + serverAddress);
	}

	@Override
	protected MongoClient client(ServerAddress serverAddress, MongoCredential credential) {
		return MongoClients.create(MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString("mongodb://" + serverAddress))
			.credential(credential)
			.build());
	}

	@Override
	protected Document resultOfAction(MongoClient client, MongoClientAction.Action action) {
		if (action instanceof MongoClientAction.RunCommand) {
			return client.getDatabase(action.database()).runCommand(((MongoClientAction.RunCommand) action).command());
		}
		throw new IllegalArgumentException("Action not supported: " + action);
	}
}
