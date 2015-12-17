package de.mpc.pia.knime.nodes.PIAWizard;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.score.FDRData;

public class PIAViewModel {
	
	/** the viewed PIA modeller */
	private PIAModeller piaModeller;
	
	/** the settings, which are executed */
	private Map<String, Object> settings;
	
	
	public PIAViewModel(PIAModeller piaModeller) {
		this.piaModeller = piaModeller;
		this.settings = new HashMap<String, Object>();
	}
	
	
	/**
	 * Getter for the {@link PIAModeller}
	 * @return
	 */
	public PIAModeller getPIAModeller() {
		return piaModeller;
	}
	
	
	/**
	 * Getter for the {@link PSMModeller}
	 * @return
	 */
	public PSMModeller getPSMModeller() {
		return piaModeller.getPSMModeller();
	}
	
	
	/**
	 * Adds the specified setting to the settings.
	 * 
	 * @param key
	 * @param value
	 * @return the value of the setting with the given key, before adding the new value (might be null)
	 */
	public Object addSetting(String key, Object value) {
		return settings.put(key, value);
	}
	
	
	/**
	 * Gets the value of the setting with the given key.
	 * 
	 * @param key
	 * @return
	 */
	public Object getSetting(String key) {
		return settings.get(key);
	}
	
	
	/**
	 * Gets the value of the setting with the given key or the default value,
	 * if there is no setting with this key.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Object getSetting(String key, Object defaultValue) {
		if (settings.containsKey(key)) {
			return settings.get(key);
		}
		return defaultValue;
	}
	
	
	private String getSettingString(String key, String defaultValue) {
		Object value = getSetting(key, defaultValue);
		if (value instanceof String) {
			return (String)value;
		}
		return null;
	}
	
	
	private String[] getSettingStringArray(String key, String[] defaultValue) {
		Object value = getSetting(key, defaultValue);
		if (value instanceof String[]) {
			return (String[])value;
		}
		return null;
	}
	
	
	private Double getSettingDouble(String key, Double defaultValue) {
		Object value = getSetting(key, defaultValue);
		if (value instanceof Double) {
			return (Double)value;
		}
		return null;
	}
	
	
	private Integer getSettingInteger(String key, Integer defaultValue) {
		Object value = getSetting(key, defaultValue);
		if (value instanceof Integer) {
			return (Integer)value;
		}
		return null;
	}
	
	
	private Boolean getSettingBoolean(String key, Boolean defaultValue) {
		Object value = getSetting(key, defaultValue);
		if (value instanceof Boolean) {
			return (Boolean)value;
		}
		return null;
	}
	
	
	/**
	 * Execute analysis on PSM level, after all settings are set.
	 * <p>
	 * If a required setting is not given, the default value is used.
	 */
	public void executePSMOperations() {
		// calculate all PSM FDRs
		
		if (piaModeller == null) {
			// actually, this point should never be reached
			return;
		}
		
		boolean createPSMSets = 
				getSettingBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean());
		piaModeller.setCreatePSMSets(createPSMSets);
		
		String decoyStrategy = getSettingString(PIASettings.DECOY_STRATEGY.getKey(), PIASettings.DECOY_STRATEGY.getDefaultString());
		String decoyPattern = getSettingString(PIASettings.DECOY_PATTERN.getKey(), PIASettings.DECOY_PATTERN.getDefaultString());
		if (decoyStrategy.equals(FDRData.DecoyStrategy.SEARCHENGINE.toString())) {
			// set the strategy to searchengine
			piaModeller.getPSMModeller().setAllDecoyPattern(FDRData.DecoyStrategy.SEARCHENGINE.toString());
		} else {
			piaModeller.getPSMModeller().setAllDecoyPattern(decoyPattern);
		}
		
		for (FDRData fdrData : piaModeller.getPSMModeller().getFileFDRData().values()) {
			fdrData.setFDRThreshold(getSettingDouble(PIASettings.FDR_THRESHOLD.getKey(), PIASettings.FDR_THRESHOLD.getDefaultDouble()));
		}
		
		// set the top identifications
		piaModeller.getPSMModeller().setAllTopIdentifications(
				getSettingInteger(PIASettings.USED_IDENTIFICATIONS.getKey(), PIASettings.USED_IDENTIFICATIONS.getDefaultInteger()));
		
		
		// set the preferred scores for FDR calculation
		String[] preferredScores = getSettingStringArray(PIASettings.PREFERRED_SCORES.getKey(), PIASettings.PREFERRED_SCORES.getDefaultStringArray());
		piaModeller.getPSMModeller().resetPreferredFDRScores();
		for (int i=0; i < preferredScores.length; i++) {
			piaModeller.getPSMModeller().addPreferredFDRScore(preferredScores[i]);
		}
		
		
		// calculate the FDR
		piaModeller.getPSMModeller().calculateAllFDR();
		if (createPSMSets) {
			piaModeller.getPSMModeller().calculateCombinedFDRScore();
		}
	}
}
