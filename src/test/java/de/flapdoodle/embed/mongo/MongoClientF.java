package de.flapdoodle.embed.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoClientF {

	public static MongoClient client(ServerAddress serverAddress) {
		return MongoClients.create("mongodb://"+serverAddress);
//		return MongoClients.create(MongoClientSettings.builder()
//			.applyConnectionString(new ConnectionString("mongodb://"+serverAddress))
//			.build());
	}

	public static MongoClient client(ServerAddress serverAddress, MongoCredential credential) {
		return MongoClients.create(MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString("mongodb://"+serverAddress))
			.credential(credential)
			.build());
	}
}
