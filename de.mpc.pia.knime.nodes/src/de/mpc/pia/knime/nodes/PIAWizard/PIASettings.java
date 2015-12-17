package de.mpc.pia.knime.nodes.PIAWizard;

import de.mpc.pia.modeller.score.FDRData;

/**
 * This class holds some keys and default values for stored settings.
 * 
 * @author julian
 *
 */
public enum PIASettings {
	
	CREATE_PSMSETS("create_psmsets", new Boolean(true)),
	FDR_THRESHOLD("fdr_threshold", new Double(0.01)),
	
	DECOY_STRATEGY("fdr_decoy_strategy", FDRData.DecoyStrategy.ACCESSIONPATTERN.toString()),
	DECOY_PATTERN("fdr_decoy_pattern", "rev_.*"),
	USED_IDENTIFICATIONS("fdr_used_identifications", 1),	// 1 = top identifications, 0 = all identifications
	
	PREFERRED_SCORES("fdr_preferred_scores", new String[]{}),
	;
	
	
	private String key;
	private Object default_value;
	
	
	private PIASettings(String key, Object default_value) {
		this.key = key;
		this.default_value = default_value;
	}
	
	
	public String getKey() {
		return key;
	}
	
	
	public Object getDefaultValue() {
		return default_value;
	}
	
	
	public Boolean getDefaultBoolean() {
		if (default_value instanceof Boolean) {
			return (Boolean)default_value;
		}
		return null;
	}
	
	
	public Double getDefaultDouble() {
		if (default_value instanceof Double) {
			return (Double)default_value;
		}
		return null;
	}
	
	
	public Integer getDefaultInteger() {
		if (default_value instanceof Integer) {
			return (Integer)default_value;
		}
		return null;
	}
	
	
	public String getDefaultString() {
		if (default_value instanceof String) {
			return (String)default_value;
		}
		return null;
	}
	
	
	public String[] getDefaultStringArray() {
		if (default_value instanceof String[]) {
			return (String[])default_value;
		}
		return null;
	}
}