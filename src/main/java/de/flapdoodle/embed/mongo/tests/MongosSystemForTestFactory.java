package de.flapdoodle.embed.mongo.tests;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.MongosExecutable;
import de.flapdoodle.embed.mongo.MongosProcess;
import de.flapdoodle.embed.mongo.MongosStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.MongosConfig;
import de.flapdoodle.embed.mongo.config.MongosRuntimeConfig;

public class MongosSystemForTestFactory {

	private final static Logger logger = Logger
			.getLogger(MongosSystemForTestFactory.class.getName());

	public static final String ADMIN_DATABASE_NAME = "admin";
	public static final String LOCAL_DATABASE_NAME = "local";
	public static final String REPLICA_SET_NAME = "rep1";
	public static final String OPLOG_COLLECTION = "oplog.rs";

	private final MongosConfig config;
	private final Map<String, List<MongodConfig>> replicaSets;
	private final List<MongodConfig> configServers;
	private final String shardDatabase;
	private final String shardCollection;
	private final String shardKey;

	private MongosExecutable mongosExecutable;
	private MongosProcess mongosProcess;
	private List<MongodProcess> mongodProcessList;
	private List<MongodProcess> mongodConfigProcessList;

	public MongosSystemForTestFactory(MongosConfig config,
			Map<String, List<MongodConfig>> replicaSets,
			List<MongodConfig> configServers, String shardDatabase,
			String shardCollection, String shardKey) {
		this.config = config;
		this.replicaSets = replicaSets;
		this.configServers = configServers;
		this.shardDatabase = shardDatabase;
		this.shardCollection = shardCollection;
		this.shardKey = shardKey;
	}

	public void start() throws Throwable {
		this.mongodProcessList = new ArrayList<MongodProcess>();
		this.mongodConfigProcessList = new ArrayList<MongodProcess>();
		for (Entry<String, List<MongodConfig>> entry : replicaSets.entrySet()) {
			initializeReplicaSet(entry);
		}
		for (MongodConfig config : configServers) {
			initializeConfigServer(config);
		}
		initializeMongos();
		configureMongos();
	}

	private void initializeReplicaSet(Entry<String, List<MongodConfig>> entry)
			throws Exception {
		String replicaName = entry.getKey();
		List<MongodConfig> mongoConfigList = entry.getValue();

		if (mongoConfigList.size() < 3) {
			throw new Exception(
					"A replica set must contain at least 3 members.");
		}
		// Create 3 mongod processes
		for (MongodConfig mongoConfig : mongoConfigList) {
			if (!mongoConfig.getReplSetName().equals(replicaName)) {
				throw new Exception(
						"Replica set name must match in mongo configuration");
			}
			MongodStarter starter = MongodStarter.getDefaultInstance();
			MongodExecutable mongodExe = starter.prepare(mongoConfig);
			MongodProcess process = mongodExe.start();
			mongodProcessList.add(process);
		}
		Thread.sleep(1000);
		MongoOptions mo = new MongoOptions();
		mo.autoConnectRetry = true;
		Mongo mongo = new Mongo(new ServerAddress(mongoConfigList.get(0)
				.getServerAddress().getHostName(), mongoConfigList.get(0)
				.getPort()), mo);
		DB mongoAdminDB = mongo.getDB(ADMIN_DATABASE_NAME);

		CommandResult cr = mongoAdminDB
				.command(new BasicDBObject("isMaster", 1));
		System.out.println("isMaster: " + cr);

		// Build json replica set setting
		String replicaSetSetting = "{'_id': '" + replicaName
				+ "', 'members': [";
		int i = 0;
		for (MongodConfig mongoConfig : mongoConfigList) {
			if (i > 0) {
				replicaSetSetting += ", ";
			}
			replicaSetSetting += "{'_id': " + i++ + ", 'host': '"
					+ mongoConfig.getServerAddress().getHostName() + ":"
					+ mongoConfig.getPort() + "'}";
		}
		replicaSetSetting += "]}";
		System.out.println(replicaSetSetting);
		// Initialize replica set
		cr = mongoAdminDB.command(new BasicDBObject("replSetInitiate",
				(DBObject) JSON.parse(replicaSetSetting)));
		logger.info("replSetInitiate: " + cr);

		Thread.sleep(5000);
		cr = mongoAdminDB.command(new BasicDBObject("replSetGetStatus", 1));
		logger.info("replSetGetStatus: " + cr);

		// Check replica set status before to proceed
		while (!isReplicaSetStarted(cr)) {
			logger.info("Waiting for 3 seconds...");
			Thread.sleep(1000);
			cr = mongoAdminDB.command(new BasicDBObject("replSetGetStatus", 1));
			logger.info("replSetGetStatus: " + cr);
		}

		mongo.close();
		mongo = null;
	}

	private boolean isReplicaSetStarted(BasicDBObject setting) {
		if (setting.get("members") == null) {
			return false;
		}

		BasicDBList members = (BasicDBList) setting.get("members");
		for (Object m : members.toArray()) {
			BasicDBObject member = (BasicDBObject) m;
			logger.info(member.toString());
			int state = member.getInt("state");
			logger.info("state: " + state);
			// 1 - PRIMARY, 2 - SECONDARY, 7 - ARBITER
			if (state != 1 && state != 2 && state != 7) {
				return false;
			}
		}
		return true;
	}

	private void initializeConfigServer(MongodConfig config) throws Exception {
		if (!config.isConfigServer()) {
			throw new Exception(
					"Mongo configuration is not a defined for a config server.");
		}
		MongodStarter starter = MongodStarter.getDefaultInstance();
		MongodExecutable mongodExe = starter.prepare(config);
		MongodProcess process = mongodExe.start();
		mongodProcessList.add(process);
	}

	private void initializeMongos() throws Exception {
		MongosStarter runtime = MongosStarter.getInstance(MongosRuntimeConfig
				.getInstance(logger));
		mongosExecutable = runtime.prepare(config);
		mongosProcess = mongosExecutable.start();
	}

	private void configureMongos() throws Exception {
		CommandResult cr;
		MongoOptions mo = new MongoOptions();
		mo.autoConnectRetry = true;
		Mongo mongo = new Mongo(new ServerAddress(this.config
				.getServerAddress().getHostName(), this.config.getPort()), mo);
		DB mongoAdminDB = mongo.getDB(ADMIN_DATABASE_NAME);

		// Add shard from the replica set list
		for (Entry<String, List<MongodConfig>> entry : this.replicaSets
				.entrySet()) {
			int i = 0;
			String replicaName = entry.getKey();
			String command = "";
			for (MongodConfig mongodConfig : entry.getValue()) {
				if (command.isEmpty()) {
					command = replicaName + "/";
				} else {
					command += ",";
				}
				command += mongodConfig.getServerAddress().getHostName() + ":"
						+ mongodConfig.getPort();
			}
			logger.info("Execute add shard command: " + command);
			cr = mongoAdminDB.command(new BasicDBObject("addShard", command));
			logger.info(cr.toString());
		}

		logger.info("Execute list shards.");
		cr = mongoAdminDB.command(new BasicDBObject("listShards", 1));
		logger.info(cr.toString());

		// Enabled sharding at database level
		logger.info("Enabled sharding at database level");
		cr = mongoAdminDB.command(new BasicDBObject("enableSharding",
				this.shardDatabase));
		logger.info(cr.toString());

		// Create index in sharded collection
		logger.info("Create index in sharded collection");
		DB db = mongo.getDB(this.shardDatabase);
		db.getCollection(this.shardCollection).ensureIndex(this.shardKey);

		// Shard the collection
		logger.info("Shard the collection: " + this.shardCollection);
		DBObject cmd = (DBObject) JSON.parse("{ 'shardCollection' : '"
				+ this.shardCollection + "', 'key' : {'" + this.shardKey
				+ "':1} }");
		cr = mongoAdminDB.command(cmd);
		logger.info(cr.toString());

		logger.info("Get info from config/shards");
		DBCursor cursor = mongo.getDB("config").getCollection("shards").find();
		while (cursor.hasNext()) {
			DBObject item = cursor.next();
			logger.info(item.toString());
		}

	}

	public Mongo getMongo() throws UnknownHostException, MongoException {
		return new Mongo(new ServerAddress(mongosProcess.getConfig()
				.getServerAddress(), mongosProcess.getConfig().getPort()));
	}

	public void stop() {
		for (MongodProcess process : this.mongodProcessList) {
			process.stop();
		}
		for (MongodProcess process : this.mongodConfigProcessList) {
			process.stop();
		}
		this.mongosProcess.stop();
	}
}
