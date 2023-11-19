package de.flapdoodle.embed.mongo.client;

import org.immutables.value.Value;

import java.util.Collections;
import java.util.List;

@Value.Immutable
public abstract class AuthenticationSetup {
	@Value.Parameter
	protected abstract UsernamePassword admin();

	@Value.Default
	protected List<Entry> entries() {
		return Collections.emptyList();
	}

	public interface Entry {

	}

	@Value.Immutable
	public interface Role extends Entry {
		@Value.Parameter
		String database();
		@Value.Parameter
		String collection();
		@Value.Parameter
		String name();
		List<String> actions();
	}

	@Value.Immutable
	public interface User extends Entry {
		@Value.Parameter
		String database();
		@Value.Parameter
		UsernamePassword user();
		List<String> roles();
	}

	public static ImmutableRole role(String database, String collection, String name) {
		return ImmutableRole.of(database, collection, name);
	}

	public static ImmutableUser user(String database, UsernamePassword usernamePassword) {
		return ImmutableUser.of(database, usernamePassword);
	}

	public static ImmutableAuthenticationSetup of(UsernamePassword adminUsernamePassword) {
		return ImmutableAuthenticationSetup.of(adminUsernamePassword);
	}

}
