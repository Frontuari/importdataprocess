package net.frontuari.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.DBException;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;

import net.frontuari.base.CustomProcess;

import net.frontuari.model.MFTUForecast;
import net.frontuari.model.MFTUForecastLine;
import net.frontuari.model.X_I_Forecast;
@Process
public class ImportForecast extends CustomProcess{

	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		m_AD_Client_ID = getAD_Client_ID();
	}

	

	@Override
	protected String doIt() throws Exception {
		StringBuilder sql = null;
		int no = 0;
		String clientCheck = getWhereClause();
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE FROM I_Forecast ")
					.append("WHERE I_IsImported='Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}
//		Set Client, Org, IsActive, Created/Updated
			sql = new StringBuilder ("UPDATE I_Forecast ")
					.append("SET AD_Client_ID = COALESCE (AD_Client_ID, ").append(m_AD_Client_ID).append("),")
							.append(" AD_Org_ID = COALESCE (AD_Org_ID, 0),")
							.append(" IsActive = COALESCE (IsActive, 'Y'),")
							.append(" Created = COALESCE (Created, SysDate),")
							.append(" CreatedBy = COALESCE (CreatedBy, 0),")
							.append(" Updated = COALESCE (Updated, SysDate),")
							.append(" UpdatedBy = COALESCE (UpdatedBy, 0),")
							.append(" I_ErrorMsg = ' ',")
							.append(" I_IsImported = 'N' ")
							.append("WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Reset=" + no);
			
			//set Org
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET AD_OrgTrx_ID=(SELECT MAX(ao.AD_Org_ID) FROM AD_Org ao ")
					.append("WHERE ao.Value=i.OrgValue")
					.append(" AND ao.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE OrgValue IS NOT NULL AND AD_OrgTrx_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Org=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Organization, ' ")
					.append("WHERE OrgValue IS NOT NULL AND AD_OrgTrx_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid Org=" + no);
			
			//	Set BPartner
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET C_BPartner_ID=(SELECT MAX(bp.C_BPartner_ID) FROM C_BPartner bp ")
					.append("WHERE bp.Value=i.BPartnerValue")
					.append(" AND bp.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE BPartnerValue IS NOT NULL AND C_BPartner_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set BPartner=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid BPartner, ' ")
					.append("WHERE BPartnerValue IS NOT NULL AND C_BPartner_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid BPartner=" + no);
			
			//set warehouse
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET M_Warehouse_ID =(SELECT MAX(w.M_Warehouse_ID) FROM M_Warehouse w ")
					.append("WHERE w.Value=i.WarehouseValue")
					.append(" AND w.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE WarehouseValue IS NOT NULL AND M_Warehouse_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Warehouse=" + no);
			//
			/*sql = new StringBuilder ("UPDATE I_Forecast i")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Warehouse, ' ")
					.append("WHERE M_Warehouse_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid Warehouse=" + no);*/
			//warehouse is not mandatory so we dont set error msg
			
			//Product
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET M_Product_ID=(SELECT MAX(p.M_Product_ID) FROM M_Product p ")
					.append("WHERE p.Value=i.ProductValue")
					.append(" AND p.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE ProductValue IS NOT NULL AND M_Product_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Product=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' ")
					.append("WHERE M_Product_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid M_Product=" + no);
			
			//Period
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET C_Period_ID=(SELECT MAX(p.C_Period_ID) FROM C_Period p ")
					.append("WHERE i.PeriodDate BETWEEN p.StartDate and p.EndDate ")
					.append(" AND p.AD_Client_ID=i.AD_Client_ID AND (p.AD_Org_ID = i.AD_OrgTrx_ID OR p.AD_Org_ID = 0)) ");
			sql.append("WHERE PeriodDate IS NOT NULL AND C_Period_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Period=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Period, ' ")
					.append("WHERE C_Period_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid Period=" + no);
			
			//	Added by Jorge Colmenarez, 2021-11-24 16:58
			//	Set SalesRegion
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET C_SalesRegion_ID=(SELECT MAX(sr.C_SalesRegion_ID) FROM C_SalesRegion sr ")
					.append("WHERE sr.Value=i.SalesRegionValue")
					.append(" AND sr.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE SalesRegionValue IS NOT NULL AND C_SalesRegion_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set SalesRegion=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid SalesRegion, ' ")
					.append("WHERE SalesRegionValue IS NOT NULL AND C_SalesRegion_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid SalesRegion=" + no);
			
			//SalesRep
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET SalesRep_ID=(SELECT u.AD_User_ID FROM AD_User u ")
					.append("WHERE u.Name = i.SalesRep_Name ")
					.append(" AND u.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE SalesRep_Name IS NOT NULL AND SalesRep_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set SalesRep_ID=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid SalesRep, ' ")
					.append("WHERE SalesRep_Name IS NOT NULL AND SalesRep_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid SalesRep_Name=" + no);

			//SalesRep from SalesRegion
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET SalesRep_ID=(SELECT sr.SalesRep_ID FROM C_SalesRegion sr ")
					.append("WHERE sr.C_SalesRegion_ID = i.C_SalesRegion_ID ")
					.append(" AND sr.AD_Client_ID=i.AD_Client_ID) ");
			sql.append("WHERE C_SalesRegion_ID IS NOT NULL AND SalesRep_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set SalesRep_ID=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid SalesRep from SalesRegion, ' ")
					.append("WHERE C_SalesRegion_ID IS NOT NULL AND SalesRep_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid SalesRep_Name=" + no);
			//	End Jorge Colmenarez
			//Year
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET C_Year_ID=(SELECT MAX(p.C_Year_ID) FROM C_Period p ")
					.append("WHERE p.C_Period_ID = i.C_Period_ID")
					.append(" AND p.AD_Client_ID = i.AD_Client_ID) ");
			sql.append("WHERE C_Period_ID IS NOT NULL AND C_Year_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Year=" + no);
			
			
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Year, ' ")
					.append("WHERE C_Year_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid Year=" + no);
			
			//Calendar
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET C_Calendar_ID = (SELECT MAX(c.C_Calendar_ID) FROM C_Year c ")
					.append("WHERE c.C_Year_ID = i.C_Year_ID")
					.append(" AND c.AD_Client_ID = i.AD_Client_ID) ");
			sql.append("WHERE C_Year_ID IS NOT NULL AND C_Calendar_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Calendar=" + no);
			
			
			sql = new StringBuilder ("UPDATE I_Forecast i ")
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Calendar, ' ")
					.append("WHERE C_Year_ID IS NULL")
					.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Invalid Calendar=" + no);
			
			//	Added by Jorge Colmenarez, 2021-12-02 09:22
			//	Check if exists column used only on Arichuna Company
			X_I_Forecast f = new X_I_Forecast(getCtx(), 0, get_TrxName());
			int existsCustomerType = f.get_ColumnIndex("ARI_CustomerType_ID");
			if(existsCustomerType != -1)
			{
				//	Set CustomerType
				sql = new StringBuilder ("UPDATE I_Forecast i ")
						.append("SET ARI_CustomerType_ID=(SELECT MAX(ct.ARI_CustomerType_ID) FROM ARI_CustomerType ct ")
						.append("WHERE (ct.Value=i.CustomerTypeValue OR ct.Name = i.CustomerTypeValue)")
						.append(" AND ct.AD_Client_ID=i.AD_Client_ID) ");
				sql.append("WHERE CustomerTypeValue IS NOT NULL AND ARI_CustomerType_ID IS NULL")
						.append(" AND I_IsImported<>'Y'").append(clientCheck);
				no = DB.executeUpdateEx(sql.toString(), get_TrxName());
				if (log.isLoggable(Level.FINE)) log.fine("Set CustomerType=" + no);
				//
				sql = new StringBuilder ("UPDATE I_Forecast i ")
						.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid CustomerType, ' ")
						.append("WHERE CustomerTypeValue IS NOT NULL AND ARI_CustomerType_ID IS NULL")
						.append(" AND I_IsImported<>'Y'").append(clientCheck);
				no = DB.executeUpdateEx(sql.toString(), get_TrxName());
				if (log.isLoggable(Level.CONFIG)) log.config("Invalid Customer Type=" + no);
			}
			else
				f = null;
			//	End Jorge Colmenarez
			
			commitEx();
			if (p_IsValidateOnly)
			{
				return "Validated";
			}
			//	-------------------------------------------------------------------
			int noInsert = 0;
			int noUpdate = 0;
			int noLineInsert = 0;
			int noLineNoInsert = 0;

			//	Go through Records
			sql = new StringBuilder ("SELECT * FROM I_Forecast ")
					.append("WHERE I_IsImported='N'").append(clientCheck);
			sql.append(" ORDER BY C_Year_ID,C_Period_ID ");
			PreparedStatement pstmt =  null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					X_I_Forecast IForecast = new X_I_Forecast(getCtx(), rs, get_TrxName());
					
					String sqlForecast = "Select M_Forecast_ID from M_Forecast where C_Calendar_ID = ? and C_Year_ID = ? and ForecastType = ? AND Name = ?";
					int M_Forecast_ID = DB.getSQLValue(get_TrxName(), sqlForecast, IForecast.getC_Calendar_ID(),IForecast.getC_Year_ID(),IForecast.getForecastType(),IForecast.getName());
					
					if (M_Forecast_ID <= 0)
						M_Forecast_ID = 0;
					
					MFTUForecast Forecast = new MFTUForecast(getCtx(), M_Forecast_ID, get_TrxName());
					
					Forecast.setC_Calendar_ID(IForecast.getC_Calendar_ID());
					Forecast.setName(IForecast.getName());
					Forecast.setDescription(IForecast.getDescription());
					Forecast.setHelp(IForecast.getHelp());
					Forecast.setC_Year_ID(IForecast.getC_Year_ID());
					Forecast.setAD_Org_ID(IForecast.getAD_OrgTrx_ID());
					Forecast.set_ValueOfColumn("ForecastType", IForecast.getForecastType());
					
					if(Forecast.save()) {
						IForecast.setM_Forecast_ID(Forecast.getM_Forecast_ID());
						IForecast.saveEx();
						if(M_Forecast_ID > 0)
							noUpdate++;
						else
							noInsert++;
					}
					else
					{
						sql = new StringBuilder ("UPDATE I_Forecast i ")
								.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||")
						.append("'Cannot Insert Forecast, ' ")
						.append("WHERE I_Forecast_ID=").append(IForecast.getI_Forecast_ID());
						DB.executeUpdateEx(sql.toString(), get_TrxName());
						noLineNoInsert++;
						continue;
					}
					
					MFTUForecastLine line = new MFTUForecastLine(getCtx(), 0, get_TrxName());
					line.setM_Forecast_ID(Forecast.get_ID());
					line.setAD_Org_ID(IForecast.getAD_OrgTrx_ID());
					line.setM_Product_ID(IForecast.getM_Product_ID());
					line.setM_Warehouse_ID(IForecast.getM_Warehouse_ID());
					
					if (IForecast.getSalesRep_ID() > 0)
						line.setSalesRep_ID(IForecast.getSalesRep_ID());
					if(IForecast.getC_BPartner_ID() > 0)
						line.set_ValueOfColumn("C_BPartner_ID", IForecast.getC_BPartner_ID());
					//	Added by Jorge Colmenarez, 2021-11-24 17:00
					if(IForecast.getC_SalesRegion_ID() > 0)
						line.set_ValueOfColumn("C_SalesRegion_ID", IForecast.getC_SalesRegion_ID());
					//	Added by Jorge Colmenarez, 2021-12-02 09:33 
					//	set Customer Type if exists 
					if(IForecast.get_ColumnIndex("ARI_CustomerType_ID") != -1)
						if(IForecast.get_ValueAsInt("ARI_CustomerType_ID") > 0)
							line.set_ValueOfColumn("ARI_CustomerType_ID", IForecast.get_ValueAsInt("ARI_CustomerType_ID"));
					//	End Jorge Colmenarez
					line.setQty(IForecast.getQty());
					
					line.setC_Period_ID(IForecast.getC_Period_ID());
					line.setDatePromised(IForecast.getDatePromised());
					if(line.save()) {
						IForecast.setM_ForecastLine_ID(line.getM_ForecastLine_ID());
						IForecast.setI_IsImported(true);
						IForecast.set_ValueOfColumn("Processed", "Y");
						IForecast.saveEx();
						noLineInsert++;
					}else {
						sql = new StringBuilder ("UPDATE I_Forecast i ")
								.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||")
								.append("'Cannot Insert Forecast Line, ' ")
								.append("WHERE I_Forecast_ID=")
								.append(IForecast.getI_Forecast_ID());
						DB.executeUpdateEx(sql.toString(), get_TrxName());
						noLineNoInsert++;
						continue;
					}
				}
				DB.close(rs, pstmt);
			}
			catch (SQLException e)
			{
				rollback();
				throw new DBException(e, sql.toString());
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
				//	Set Error to indicator to not imported
				sql = new StringBuilder ("UPDATE I_Forecast ")
						.append("SET I_IsImported='N', Updated=SysDate ")
						.append("WHERE I_IsImported<>'Y'").append(clientCheck);
				no = DB.executeUpdateEx(sql.toString(), get_TrxName());
				addLog (0, null, new BigDecimal (no), "@Errors@");
				addLog (0, null, new BigDecimal (noLineNoInsert), "@Errors@");
				addLog (0, null, new BigDecimal (noInsert), "@M_Forecast_ID@: @Inserted@");
				addLog (0, null, new BigDecimal (noUpdate), "@M_Forecast_ID@: @Updated@");
				addLog (0, null, new BigDecimal (noLineInsert), "@M_ForecastLine_ID@: @Inserted@");
			}
			return "@OK@";
	}
		
	
	public String getWhereClause()
	{
		StringBuilder msgreturn = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);
		return msgreturn.toString();
	}

	public String getImportTableName()
	{
		return X_I_Forecast.Table_Name;
	}
}
