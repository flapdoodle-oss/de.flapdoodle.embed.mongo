package de.flapdoodle.embed.mongo.doc;

import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;

public class MongoJUnitTest {

	protected TransitionWalker.ReachedState<RunningMongodProcess> running;
	protected ServerAddress serverAddress;

	@BeforeEach
	void startMongodb() {
		ImmutableMongod mongodConfig = Mongod.instance();
		Version.Main version = Version.Main.V8_0;

		running = mongodConfig.start(version);
		serverAddress = serverAddress(running.current().getServerAddress());
	}

	@AfterEach
	void teardownMongodb() {
		serverAddress = null;
		if (running != null) { running.close(); }
		running = null;
	}

	@Test
	void testStuff() {
		try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
			MongoDatabase db = mongo.getDatabase("test");
			MongoCollection<Document> col = db.getCollection("testCol");
			col.insertOne(new Document("testDoc", new Date()));
			assertThat(col.countDocuments()).isEqualTo(1L);
		}
	}
}
