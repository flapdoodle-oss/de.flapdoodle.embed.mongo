package de.flapdoodle.embed.mongo.client;

import org.immutables.value.Value;

@Value.Immutable
public interface UsernamePassword {
	@Value.Parameter
	String name();

	@Value.Parameter
	char[] password();

	@Value.Auxiliary
	default String passwordAsString() {
		return new String(password());
	}

	static UsernamePassword of(String username, char[] password) {
		return ImmutableUsernamePassword.of(username, password);
	}

	@Deprecated
	static UsernamePassword of(String username, String password) {
		return ImmutableUsernamePassword.of(username, password.toCharArray());
	}
}
