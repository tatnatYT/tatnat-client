package com.tatnat.client.modules;

/** Groups shown as tabs along the top of the ClickGUI. */
public enum Category {
	HUD("HUD"),
	VISUAL("Visual"),
	UTILITY("Utility"),
	COSMETIC("Cosmetic");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}
