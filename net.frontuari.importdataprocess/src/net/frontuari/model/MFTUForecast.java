package net.frontuari.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MForecast;

@org.adempiere.base.Model(table = "M_Forecast")
public class MFTUForecast extends MForecast {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2102841879325417131L;

	public MFTUForecast(Properties ctx, int M_Forecast_ID, String trxName) {
		super(ctx, M_Forecast_ID, trxName);
	}

	public MFTUForecast(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
