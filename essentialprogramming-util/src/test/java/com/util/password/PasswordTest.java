package com.util.password;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

public class PasswordTest {

	@Test
	void generate_7_unique_chars_alphanumeric_only() {
		PasswordOptions passwordOptions = buildPasswordOptions(7, false, true);
		String validPasswordAlphanumericOnly = "Awk123c";
		String invalidPasswordNoDigits = "Aab.bcd";
		String invalidPasswordNoLowercase = "AD1.BCE";
		String invalidPasswordNoUppercase = "da1.bce";
		String invalidPasswordNoNonUniqueChars = "aa1.bCC";

		assert PasswordUtil.isValidPassword(validPasswordAlphanumericOnly, passwordOptions);
		Assertions.assertFalse(PasswordUtil.isValidPassword(invalidPasswordNoDigits, passwordOptions));
		Assertions.assertFalse(PasswordUtil.isValidPassword(invalidPasswordNoLowercase, passwordOptions));
		Assertions.assertFalse(PasswordUtil.isValidPassword(invalidPasswordNoUppercase, passwordOptions));
		Assertions.assertFalse(PasswordUtil.isValidPassword(invalidPasswordNoNonUniqueChars, passwordOptions));
	}

	@Test
	void password_strength() {
		String passwordStrong = "Aa1.bCAwk";
		String passwordMedium = "Aa1.bc";
		String passwordWeak = "Aa1dbc";
		String passwordVeryWeak = "abc";
		String passwordBlank = "";

		Assertions.assertTrue(PasswordUtil.isStrongPassword(passwordStrong));
		Assertions.assertFalse(PasswordUtil.isStrongPassword(passwordMedium));
		Assertions.assertEquals(PasswordStrength.Strong, PasswordUtil.getPasswordStrength(passwordStrong));
		Assertions.assertEquals(PasswordStrength.Medium, PasswordUtil.getPasswordStrength(passwordMedium));
		Assertions.assertEquals(PasswordStrength.Weak, PasswordUtil.getPasswordStrength(passwordWeak));
		Assertions.assertEquals(PasswordStrength.VeryWeak, PasswordUtil.getPasswordStrength(passwordVeryWeak));
		Assertions.assertEquals(PasswordStrength.None, PasswordUtil.getPasswordStrength(passwordBlank));
	}

	@Test
	public void generate_1000_random_passwords() {
		PasswordOptions passwordOptions = buildPasswordOptions(16, false, false);
		for (int i = 0 ; i < 1000; i++) {
			String password = PasswordUtil.generateRandomPassword(passwordOptions);
			System.out.println("Random password: " + password);
			Assertions.assertEquals(passwordOptions.getRequiredLength(), password.length());
			Assertions.assertTrue(PasswordUtil.isValidPassword(password, passwordOptions));
		}
	}

	@Test
	public void generate_1000_random_passwords_with_default_options() {
		for (int i = 0 ; i < 1000; i++) {
			String password = PasswordUtil.generateRandomPassword();
			System.out.println("Random password: " + password);
			Assertions.assertTrue(PasswordUtil.isValidPassword(password, PasswordUtil.DEFAULT_PASSWORD_OPTIONS));
		}
	}

	@Test
	public void generate_1000_random_passwords_with_non_alphanumeric() {
		PasswordOptions passwordOptions = buildPasswordOptions(16, true, false);
		for (int i = 0 ; i < 1000; i++) {
			String password = PasswordUtil.generateRandomPassword(passwordOptions);
			System.out.println("Random password: " + password);
			String nonAlphanumerics = "@%+\\/'!#$^?:,(){}[]~-_.";

			boolean containsNonAlphanumeric = false;
			if (password.chars().anyMatch(character -> nonAlphanumerics.chars().mapToObj(ch -> (char) ch)
					.collect(Collectors.toSet()).contains((char) character))) {
				containsNonAlphanumeric = true;
			}

			Assertions.assertTrue(containsNonAlphanumeric);
			Assertions.assertEquals(passwordOptions.getRequiredLength(), password.length());
			Assertions.assertTrue(PasswordUtil.isValidPassword(password, passwordOptions));
		}
	}

	@Test
	public void generate_1000_random_passwords_alphanumeric_only() {
		PasswordOptions passwordOptions = buildPasswordOptions(6, false, true);
		for (int i = 0 ; i < 1000; i++) {
			String password = PasswordUtil.generateRandomPassword(passwordOptions);
			System.out.println("Random password: " + password);
			String nonAlphanumerics = "@%+\\/'!#$^?:,(){}[]~-_.";

			boolean containsNonAlphanumeric = false;
			if (password.chars().anyMatch(character -> nonAlphanumerics.chars().mapToObj(ch -> (char) ch)
					.collect(Collectors.toSet()).contains((char) character))) {
				containsNonAlphanumeric = true;
			}

			Assertions.assertFalse(containsNonAlphanumeric);
			Assertions.assertEquals(passwordOptions.getRequiredLength(), password.length());
			Assertions.assertTrue(PasswordUtil.isValidPassword(password, passwordOptions));
		}
	}

	private PasswordOptions buildPasswordOptions(int length, boolean requiresNonAlphanumeric, boolean alphaNumericOnly) {
		return new PasswordOptions(length, 6, requiresNonAlphanumeric, true, true, true, alphaNumericOnly);
	}

}
