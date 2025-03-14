package net.frontuari.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.WarehouseInvalidForOrgException;
import org.compiere.model.MForecastLine;
import org.compiere.model.MOrg;
import org.compiere.model.MPeriod;
import org.compiere.model.MSalesRegion;
import org.compiere.model.MWarehouse;

@org.adempiere.base.Model(table = "M_ForecastLine")
public class MFTUForecastLine extends MForecastLine {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7282026459617444887L;
	/** Parent					*/
	private MFTUForecast			m_parent = null;

	public MFTUForecastLine(Properties ctx, int M_ForecastLine_ID, String trxName) {
		super(ctx, M_ForecastLine_ID, trxName);
	}

	public MFTUForecastLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**************************************************************************
	 * 	Before Save
	 *	@param newRecord
	 *	@return true if it can be saved
	 */
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		MFTUForecast f = getParent();
		String ft = f.get_ValueAsString("ForecastType");
		
		if(ft.equals("P"))
		{
			if (newRecord 
					|| is_ValueChanged("AD_Org_ID") || is_ValueChanged("M_Warehouse_ID"))
				{	
					MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
					if (wh.getAD_Org_ID() != getAD_Org_ID())
					{
						throw new WarehouseInvalidForOrgException(wh.getName(), MOrg.get(getCtx(), getAD_Org_ID()).getName());
					}
				}
		}
		else
		{
			if(newRecord
					|| is_ValueChanged("C_SalesRegion_ID"))
			{
				if(get_ValueAsInt("C_SalesRegion_ID")>0)
				{
					MSalesRegion sr = new MSalesRegion(getCtx(), get_ValueAsInt("C_SalesRegion_ID"), get_TrxName());
					
					if(getSalesRep_ID()!=sr.getSalesRep_ID())
						setSalesRep_ID(sr.getSalesRep_ID());
				}
				MPeriod p = new MPeriod(getCtx(), getC_Period_ID(), get_TrxName());
				setDatePromised(p.getEndDate());
			}
		}
		return true;
	}
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	@Override
	public MFTUForecast getParent()
	{
		if (m_parent == null)
			m_parent = new MFTUForecast(getCtx(), getM_Forecast_ID(), get_TrxName());
		return m_parent;
	}	//	getParent

}
