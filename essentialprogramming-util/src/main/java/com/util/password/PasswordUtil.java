package com.util.password;


import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class PasswordUtil {

	private static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
	private static final String DIGITS = "1234567890";
	private static final String SYMBOLS = "@%+\\/'!#$^?:,(){}[]~-_.";

	private final static String[] CHARACTER_SETS = new String[]{UPPERCASE_LETTERS, LOWERCASE_LETTERS, DIGITS, SYMBOLS};
	public final static PasswordOptions DEFAULT_PASSWORD_OPTIONS = new PasswordOptions(6, 6, false, true, true, true, false);

	/**
	 * Generate a password randomly with help from a random number generator.
	 * Steps:
	 * 1. Determine which character sets to include based on given options
	 * 2. Randomly choose a character set to select a character from
	 * 3. Randomly choose a character from that randomly chosen character set
	 * 4. Repeat steps 2 and 3 (in that order) for as many times as the password's length
	 */
	public static String generateRandomPassword(){
		return generateRandomPassword(DEFAULT_PASSWORD_OPTIONS);
	}
	public static String generateRandomPassword(final PasswordOptions options) {

		final Random random = new SecureRandom();
		final List<Character> chars = new ArrayList<>();

		if (options.isRequireUppercase()) {
			int index = random.nextInt(UPPERCASE_LETTERS.length());
			chars.add(UPPERCASE_LETTERS.charAt(index));
		}
		if (options.isRequireLowercase()) {
			int index = random.nextInt(LOWERCASE_LETTERS.length());
			chars.add(LOWERCASE_LETTERS.charAt(index));
		}
		if (options.isRequireDigit()) {
			int index = random.nextInt(DIGITS.length());
			chars.add(DIGITS.charAt(index));
		}
		if (options.isRequireNonAlphanumeric()) {
			int index = random.nextInt(SYMBOLS.length());
			chars.add(SYMBOLS.charAt(index));
		}
		for (int i = chars.size(); i < options.getRequiredLength(); i++) {
			chars.add(generateRandomChar(random, options.isAlphanumericOnly()));
		}

		final HashSet<Character> charsSet = new HashSet<>(chars);
		final int duplicatesNumber = chars.size() - charsSet.size();
		final Map<Character, Long> frequencyMap = chars.stream().collect(Collectors.groupingBy(c -> c, Collectors.counting()));
		if (chars.size() - duplicatesNumber < options.getRequiredUniqueChars()) {
			IntStream.range(0, chars.size()).forEach(index -> {
				final Character character = chars.get(index);
				if (frequencyMap.get(character) > 1) {
					Character generated = generateRandomChar(random, options.isAlphanumericOnly());
					while (chars.contains(generated)) {
						generated = generateRandomChar(random, options.isAlphanumericOnly());
					}
					//frequencyMap.merge(character, 1L, (oldValue, value) -> oldValue - 1);
					frequencyMap.computeIfPresent(character, (k, v) -> v - 1);
					chars.set(index, generated);
				}
			});
		}

		Collections.shuffle(chars);

		final StringBuilder builder = new StringBuilder(chars.size());
		chars.forEach(builder::append);

		return builder.toString();
		//return chars.stream().map(String::valueOf).collect(Collectors.joining());
	}

	private static Character generateRandomChar(Random rand, boolean alphanumericOnly) {
		final int length = CHARACTER_SETS.length - 1;
		int randomCharSetMaxIndex = alphanumericOnly ? length - 1 : length;

		final String characters = CHARACTER_SETS[rand.nextInt(randomCharSetMaxIndex)];
		int randomIndex = rand.nextInt(characters.length());
		return characters.charAt(randomIndex);
	}

	public static PasswordStrength getPasswordStrength(String password) {
		int score = 0;
		if (password == null || password.trim().isEmpty())
			return PasswordStrength.None;
		if (!hasMinimumLength(password, 5))
			return PasswordStrength.VeryWeak;
		if (hasMinimumLength(password, 8))
			score++;
		if (containsUpperCaseLetter(password) && containsLowerCaseLetter(password))
			score++;
		if (containsDigit(password))
			score++;
		if (hasSpecialChar(password))
			score++;
		return PasswordStrength.get(score);
	}


	/**
	 * Requirements for a strong password:
	 * - minimum 8 characters
	 * - at lease one UC letter
	 * - at least one LC letter
	 * - at least one non-letter char (digit OR special char)
	 */
	public static boolean isStrongPassword(String password) {
		if (password == null || password.isEmpty()) return false;
		return hasMinimumLength(password, 8) && containsUpperCaseLetter(password) && containsLowerCaseLetter(password)
				&& (containsDigit(password) || hasSpecialChar(password));
	}


	public static boolean isValidPassword(final String password, final PasswordOptions options) {
		return isValidPassword(password,
				options.getRequiredLength(),
				options.getRequiredUniqueChars(),
				options.isAlphanumericOnly(),
				options.isRequireNonAlphanumeric(),
				options.isRequireLowercase(),
				options.isRequireUppercase(),
				options.isRequireDigit());
	}

	private static boolean isValidPassword(String password, int requiredLength, int requiredUniqueChars,boolean alphanumericOnly,
										   boolean requireNonAlphanumeric, boolean requireLowercase, boolean requireUppercase, boolean requireDigit) {
		if (password == null || password.isEmpty())
			return false;
		if (!hasMinimumLength(password, requiredLength))
			return false;
		if (!hasMinimumUniqueChars(password, requiredUniqueChars))
			return false;
		if (requireNonAlphanumeric && !hasSpecialChar(password))
			return false;
		if (alphanumericOnly && hasSpecialChar(password))
			return false;
		if (requireLowercase && !containsLowerCaseLetter(password))
			return false;
		if (requireUppercase && !containsUpperCaseLetter(password))
			return false;
		if (requireDigit && !containsDigit(password))
			return false;
		return true;
	}

	private static boolean hasMinimumLength(String password, int minLength) {
		return password.length() >= minLength;
	}

	private static boolean hasMinimumUniqueChars(String password, int minUniqueChars) {
		return password.chars().distinct().count() >= minUniqueChars;
	}


	/**
	 * Returns TRUE if the password contains at least one uppercase letter
	 */
	private static boolean containsUpperCaseLetter(final String password) {
		return password.chars().filter(Character::isUpperCase).findAny().isPresent();
	}

	/**
	 * Returns TRUE if the password contains at least one lowercase letter
	 */
	private static boolean containsLowerCaseLetter(final String password) {
		return password.chars().filter(Character::isLowerCase).findAny().isPresent();
	}

	/**
	 * Returns TRUE if the password has at least one digit
	 */
	private static boolean containsDigit(String password) {
		return password.chars().filter(Character::isDigit).findAny().isPresent();
	}

	/**
	 * Returns TRUE if the password has at least one special character
	 */
	private static boolean hasSpecialChar(String password) {
		// return password.Any(c => char.IsPunctuation(c)) || password.Any(c =>char.IsSeparator(c)) || password.Any(c => char.IsSymbol(c));
		Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(password);
		return m.find();
	}

}
