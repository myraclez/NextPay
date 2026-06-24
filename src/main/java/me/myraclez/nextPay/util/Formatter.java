package me.myraclez.nextPay.util;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class Formatter {

	private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

	private static final Pattern PLAIN_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

	public static String format(double amount) {

		if (amount >= 1.0E12) {
			return decimalFormat.format(amount / 1.0E12) + "T";
		} else if (amount >= 1.0E9) {
			return decimalFormat.format(amount / 1.0E9) + "B";
		} else if (amount >= 1000000.0) {
			return decimalFormat.format(amount / 1000000.0) + "M";
		} else if (amount >= 1000.0) {
			return decimalFormat.format(amount / 1000.0) + "K";
		} else {
			return decimalFormat.format(amount);
		}
	}

	public static double deformat(String formatted) {
		if (formatted == null || formatted.isEmpty()) {
			throw new IllegalArgumentException("Input cannot be null or empty");
		}

		String trimmed = formatted.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Input cannot be blank");
		}

		char suffix = Character.toUpperCase(trimmed.charAt(trimmed.length() - 1));
		long multiplier;

		switch (suffix) {
			case 'K': multiplier = 1_000L; break;
			case 'M': multiplier = 1_000_000L; break;
			case 'B': multiplier = 1_000_000_000L; break;
			case 'T': multiplier = 1_000_000_000_000L; break;
			case 'Q': multiplier = 1_000_000_000_000_000L; break;
			default:
				return parseStrict(trimmed);
		}

		String numberPart = trimmed.substring(0, trimmed.length() - 1).trim();
		if (numberPart.isEmpty()) {
			throw new IllegalArgumentException("Missing numeric value before suffix: " + formatted);
		}

		return parseStrict(numberPart) * multiplier;
	}

	private static double parseStrict(String value) {
		if (!PLAIN_NUMBER.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid number format: " + value);
		}
		return Double.parseDouble(value);
	}

}
