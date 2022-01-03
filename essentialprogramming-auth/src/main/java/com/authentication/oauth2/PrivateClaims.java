package com.authentication.oauth2;

/**
 * Supported private claim types for building the Token and the ID Token
 */
public enum PrivateClaims {
	/**
	 * Email address of the customer
	 */
	MAIL("email"),

	/**
	 * alias of the customer
	 */
	NAME("name"),

	PLATFORM("platform"),

	ACTIVE("active"),

	ROLES("roles");


	private final String loginType;

	PrivateClaims(final String loginType) {
		this.loginType = loginType;
	}

	public String getType() {
		return this.loginType;
	}
}
