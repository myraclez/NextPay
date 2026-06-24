package me.myraclez.nextPay.model;

public class PlayerSettings {

	private boolean payments;
	private boolean paynotifications;

	public PlayerSettings(boolean payments, boolean paynotifications) {
		this.payments = payments;
		this.paynotifications = paynotifications;
	}

	public boolean isPayments() {
		return payments;
	}

	public boolean isNnotifications() {
		return paynotifications;
	}
}
