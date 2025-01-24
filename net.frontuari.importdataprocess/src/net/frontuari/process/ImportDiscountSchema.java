package net.frontuari.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MDiscountSchemaLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;

import net.frontuari.base.CustomProcess;

import net.frontuari.model.X_I_DiscountSchema;
@Process
public class ImportDiscountSchema extends CustomProcess {

	public ImportDiscountSchema() {
	}
	
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	private int				m_AD_Org_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;

	char DiscountType = 'P';
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Client_ID"))
				m_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("AD_Org_ID"))
				m_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}

		
	}

	@Override
	protected String doIt() throws Exception {
		//	Set Optional BPartner
		StringBuilder sql = null;
		int no = 0;
		String clientCheck = getWhereClause();
		
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE I_DiscountSchema ")
				.append("WHERE I_IsImported='Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.INFO)) log.info("Delete Old Imported =" + no);
		}
		
		//	Set Client, Org, IaActive, Created/Updated, 	ProductType
	sql = new StringBuilder ("UPDATE I_DiscountSchema ")
		.append("SET AD_Client_ID = COALESCE (AD_Client_ID, ").append(m_AD_Client_ID).append("),")
		//.append(" AD_OrgTrx_ID = COALESCE (AD_Org_ID, 0),")
		.append(" IsActive = COALESCE (IsActive, 'Y'),")
		.append(" Created = COALESCE (Created, SysDate),")
		.append(" CreatedBy = COALESCE (CreatedBy, 0),")
		.append(" Updated = COALESCE (Updated, SysDate),")
		.append(" UpdatedBy = COALESCE (UpdatedBy, 0),")

		.append(" I_ErrorMsg = ' ',")
		.append(" I_IsImported = 'N' ")
		.append("WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
	no = DB.executeUpdate(sql.toString(), get_TrxName());
	if (log.isLoggable(Level.INFO)) log.info("Reset=" + no);

	
	
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
		.append("SET C_BPartner_ID=(SELECT C_BPartner_ID FROM C_BPartner p")
		.append(" WHERE i.BPartner_Value=p.Value AND i.AD_Client_ID=p.AD_Client_ID) ")
		.append("WHERE C_BPartner_ID IS NULL")
		.append(" AND I_IsImported<>'Y'").append(clientCheck);
	no = DB.executeUpdate(sql.toString(), get_TrxName());
	if (log.isLoggable(Level.INFO)) log.info("BPartner=" + no);
	//
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
		.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid BPartner,' ")
		.append("WHERE C_BPartner_ID IS NULL AND BPartner_Value IS NOT NULL")
		.append(" AND I_IsImported<>'Y'").append(clientCheck);
	no = DB.executeUpdate(sql.toString(), get_TrxName());
	if (no != 0)
		log.warning("Invalid BPartner=" + no);


	//	****	Find Product
	//	SKU
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
		.append("SET M_Product_ID=(SELECT M_Product_ID FROM M_Product p")
		.append(" WHERE i.SKU=p.SKU AND i.AD_Client_ID=p.AD_Client_ID) ")
		.append("WHERE M_Product_ID IS NULL")
		.append(" AND I_IsImported='N'").append(clientCheck);
	no = DB.executeUpdate(sql.toString(), get_TrxName());
	if (log.isLoggable(Level.INFO)) log.info("Product Existing UPC=" + no);
	
	//Value
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
			.append("SET M_Product_ID=(SELECT M_Product_ID FROM M_Product p")
			.append(" WHERE i.Value=p.Value AND i.AD_Client_ID=p.AD_Client_ID) ")
			.append("WHERE M_Product_ID IS NULL")
			.append(" AND I_IsImported='N'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("Product Existing Value=" + no);

	//	Name
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
		.append("SET M_Product_ID=(SELECT M_Product_ID FROM M_Product p")
		.append(" WHERE i.ProductName=p.Name AND i.AD_Client_ID=p.AD_Client_ID) ")
		.append("WHERE M_Product_ID IS NULL")
		.append(" AND I_IsImported='N'").append(clientCheck);
	no = DB.executeUpdate(sql.toString(), get_TrxName());
	if (log.isLoggable(Level.INFO)) log.info("Product Existing Value=" + no);

//products classification 1
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
			.append("SET FTU_ProductClassifications_ID = ")
			.append("(SELECT MAX(FTU_ProductClassifications_ID) FROM FTU_ProductClassifications pc WHERE pc.Value = i.ftu_productclassifications_val AND pc.AD_Client_ID IN (0,i.AD_Client_ID)) ")
			.append("WHERE ftu_productclassifications_val IS NOT NULL AND FTU_ProductClassifications_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product Classifications=" + no);
		//
	sql = new StringBuilder ("UPDATE I_DiscountSchema ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product Classifications, ' ")
			.append("WHERE FTU_ProductClassifications_ID IS NULL AND ftu_productclassifications_val IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("Invalid Product Classifications=" + no);
		
		//products classification 2
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
			.append("SET FTU_ProductClassifications2_ID = ")
			.append("(SELECT MAX(FTU_ProductClassifications_ID) FROM FTU_ProductClassifications pc WHERE pc.Value = i.ftu_productclassifications2_va AND pc.AD_Client_ID IN (0,i.AD_Client_ID)) ")
			.append("WHERE ftu_productclassifications2_va IS NOT NULL AND FTU_ProductClassifications2_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product Classifications2=" + no);
			//
	sql = new StringBuilder ("UPDATE I_DiscountSchema ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product Classifications2, ' ")
			.append("WHERE FTU_ProductClassifications2_ID IS NULL AND ftu_productclassifications2_va IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("Invalid Product Classifications2=" + no);
			
//products classification 3			
	sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
			.append("SET FTU_ProductClassifications3_ID = ")
			.append("(SELECT MAX(FTU_ProductClassifications_ID) FROM FTU_ProductClassifications pc WHERE pc.Value = i.ftu_productclassifications3_va AND pc.AD_Client_ID IN (0,i.AD_Client_ID)) ")
			.append("WHERE ftu_productclassifications3_va IS NOT NULL AND FTU_ProductClassifications3_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product Classifications3=" + no);
				//
	sql = new StringBuilder ("UPDATE I_DiscountSchema ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product Classifications3, ' ")
			.append("WHERE FTU_ProductClassifications3_ID IS NULL AND ftu_productclassifications3_va IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("Invalid Product Classifications3=" + no);
//				Set Product Category
		sql = new StringBuilder ("UPDATE I_DiscountSchema ")
			.append("SET ProductCategory_Value =(SELECT MAX(Value) FROM M_Product_Category")
			.append(" WHERE IsDefault='Y' AND AD_Client_ID=").append(m_AD_Client_ID).append(") ")
			.append("WHERE ProductCategory_Value IS NULL AND M_Product_Category_ID IS NULL")
			.append(" AND M_Product_ID IS NULL")	//	set category only if product not found 
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Category Default Value=" + no);
		//
		sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
			.append("SET M_Product_Category_ID=(SELECT M_Product_Category_ID FROM M_Product_Category c")
			.append(" WHERE i.ProductCategory_Value=c.Value AND i.AD_Client_ID=c.AD_Client_ID) ")
			.append("WHERE ProductCategory_Value IS NOT NULL AND M_Product_Category_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("Set Category=" + no);
		
		// Set Conversion Type
		sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
				.append("SET C_ConversionType_ID=(SELECT C_ConversionType_ID FROM C_ConversionType a")
				.append(" WHERE i.ConversionTypeValue=a.Value AND a.AD_Client_ID = i.AD_Client_ID) ")
				.append(" WHERE C_ConversionType_ID IS NULL and ConversionTypeValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set C_ConversionType_ID=" + no);
		
		sql = new StringBuilder ("UPDATE I_DiscountSchema ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid ConversionTypeValue, ' ")
				.append("WHERE C_ConversionType_ID IS NULL AND (ConversionTypeValue IS NOT NULL)")
				.append(" AND I_IsImported<>'Y' ").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid ConversionTypeValue=" + no);
		//ORG
		sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
				.append("SET AD_OrgTrx_ID=(SELECT AD_Org_ID FROM AD_Org p")
				.append(" WHERE i.OrgName=p.Name AND i.AD_Client_ID=p.AD_Client_ID) ")
				.append("WHERE AD_OrgTrx_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.INFO)) log.info("Org=" + no);
	
		sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org,' ")
				.append("WHERE AD_OrgTrx_ID IS NULL AND OrgName IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				log.warning("Invalid ORG=" + no);
			
		//M_DiscountSchema_ID
		sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
				.append("SET M_DiscountSchema_ID=(SELECT M_DiscountSchema_ID FROM M_DiscountSchema p")
				.append(" WHERE TRIM(i.Name)=TRIM(p.Name) AND i.AD_Client_ID=p.AD_Client_ID) ")
				.append("WHERE M_DiscountSchema_ID IS NULL AND Name IS NOT NULL AND AD_Client_ID IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.INFO)) log.info("M_DiscountSchema_ID=" + no);

		//M_DiscountSchemaLine_ID
		sql = new StringBuilder ("UPDATE I_DiscountSchema i ")
				.append("SET M_DiscountSchemaLine_ID=(SELECT M_DiscountSchemaLine_ID FROM M_DiscountSchemaLine p")
				.append(" WHERE i.M_DiscountSchema_ID=p.M_DiscountSchema_ID AND i.M_Product_ID=p.M_Product_ID) ")
				.append("WHERE M_DiscountSchemaLine_ID IS NULL AND M_DiscountSchema_ID IS NOT NULL AND M_Product_ID IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.INFO)) log.info("M_DiscountSchemaLine_ID=" + no);
						
				
		commitEx();
		//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		int noInsertppvb = 0;
		int noInsertpl = 0;

		//	Go through Records
		log.fine("start inserting/updating ...");
		sql = new StringBuilder ("SELECT * FROM I_DiscountSchema WHERE I_IsImported='N'")
			.append(clientCheck);
		//PreparedStatement pstmt_setImported = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			//
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				X_I_DiscountSchema imp = new X_I_DiscountSchema(getCtx(), rs, get_TrxName());
				int I_DiscountSchema_ID = imp.get_ID();
				int M_DiscountSchema_ID = imp.getM_DiscountSchema_ID();
				if (M_DiscountSchema_ID <= 0) {
					// try to obtain the ID directly from DB
					M_DiscountSchema_ID = DB.getSQLValue(get_TrxName(), "SELECT M_DiscountSchema_ID FROM M_DiscountSchema WHERE IsActive='Y' AND AD_Client_ID=? AND Name=?", m_AD_Client_ID, imp.getName());
					if (M_DiscountSchema_ID < 0)
						M_DiscountSchema_ID = 0;
				}
				
				StringBuilder msglog = new StringBuilder("I_DiscountSchema_ID=").append(I_DiscountSchema_ID).append(", M_DiscountSchema_ID=").append(M_DiscountSchema_ID);
				log.fine(msglog.toString());

				MDiscountSchema DiscountSchema = null; 
				//	PriceList
				if (M_DiscountSchema_ID <= 0)			//	Insert new Price List
				{
					DiscountSchema = new MDiscountSchema(getCtx(),0,get_TrxName());
					DiscountSchema.setAD_Org_ID((Integer) imp.get_Value("AD_OrgTrx_ID"));
					DiscountSchema.setName(imp.getName());
					DiscountSchema.setDescription(imp.getDescription());
					DiscountSchema.setValidFrom(imp.getValidFrom());
					DiscountSchema.setDiscountType("P");
					DiscountSchema.set_ValueOfColumn("isSOPriceList", imp.isSOPriceList());
					if (DiscountSchema.save())
					{
						M_DiscountSchema_ID = DiscountSchema.getM_DiscountSchema_ID();
						imp.setM_DiscountSchema_ID(M_DiscountSchema_ID);
						imp.saveEx();
						log.finer("Insert Discount Schema");
						noInsertpl++;
					}
					else
					{
						StringBuilder sql0 = new StringBuilder ("UPDATE I_DiscountSchema i ")
							.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||").append(DB.TO_STRING("Insert Discount Schema failed"))
							.append("WHERE I_DiscountSchema_ID=").append(I_DiscountSchema_ID);
						DB.executeUpdate(sql0.toString(), get_TrxName());
						continue;
					}
				} 
					int M_DiscountSchemaLine_ID = imp.getM_DiscountSchemaLine_ID()>0?imp.getM_DiscountSchemaLine_ID():0;
					MDiscountSchemaLine Line = new MDiscountSchemaLine(getCtx(), M_DiscountSchemaLine_ID, get_TrxName());
					String sqlLineNo = "SELECT MAX(SeqNo) From M_DiscountSchemaLine Where M_DiscountSchema_ID =" + M_DiscountSchema_ID;
					int LineNo = 10;
					int seqnobd = DB.getSQLValue(get_TrxName(), sqlLineNo);
					if (DB.getSQLValue(get_TrxName(), sqlLineNo) > 0) {
					LineNo = DB.getSQLValue(get_TrxName(), sqlLineNo) +10;}						
					
					Line.setAD_Org_ID((Integer) imp.get_Value("AD_OrgTrx_ID"));
					Line.setSeqNo(LineNo);					
					Line.setM_DiscountSchema_ID(M_DiscountSchema_ID);
					Line.setC_ConversionType_ID(imp.getC_ConversionType_ID());
					Line.setConversionDate(imp.getConversionDate());
					if(imp.getC_BPartner_ID()>0)
					Line.setC_BPartner_ID(imp.getC_BPartner_ID());
					Line.setM_Product_ID(imp.getM_Product_ID());
					if(imp.getM_Product_Category_ID()>0)
					Line.setM_Product_Category_ID(imp.getM_Product_Category_ID());
					if (imp.getClassification() != null)
					Line.setClassification(imp.getClassification());
					if (imp.getGroup1()!= null)
					Line.setGroup1(imp.getGroup1());
					if (imp.getGroup2()!= null)
					Line.setGroup2(imp.getGroup2());
					if (imp.getList_Base() != null)
					Line.setList_Base(imp.getList_Base());
					if (imp.getStd_Base() != null)
					Line.setStd_Base(imp.getStd_Base());
					if (imp.getLimit_Base() != null)
					Line.setLimit_Base(imp.getLimit_Base());
					if (imp.getList_Discount() != null)
					Line.setList_Discount(imp.getList_Discount());
					if (imp.getStd_Discount() != null)
					Line.setStd_Discount(imp.getStd_Discount());
					if(imp.getLimit_Discount() != null)
					Line.setLimit_Discount(imp.getLimit_Discount());
					if(imp.getList_Rounding() != null)
					Line.setList_Rounding(imp.getList_Rounding());
					if (imp.getStd_Rounding() != null)
					Line.setStd_Rounding(imp.getStd_Rounding());
					if (imp.getLimit_Rounding() != null)
					Line.setLimit_Rounding(imp.getLimit_Rounding());
					if(Line.save()) {
						imp.setM_DiscountSchema_ID(M_DiscountSchema_ID);
						imp.setM_DiscountSchemaLine_ID(Line.getM_DiscountSchemaLine_ID());
						imp.setI_IsImported(true);
						imp.saveEx();
						noInsertppvb++;
					}else {
						StringBuilder sql0 = new StringBuilder ("UPDATE I_DiscountSchema i ")
								.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||").append(DB.TO_STRING("Insert Discount Schema Line failed"))
								.append("WHERE I_DiscountSchema_ID=").append(I_DiscountSchema_ID);
							DB.executeUpdate(sql0.toString(), get_TrxName());
							continue;
					}
				
				
				//
				commitEx();
			}	//	for all I_DiscountSchema
		}
			//
		catch (SQLException e)
		{
		}

		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Set Error to indicator to not imported
		sql = new StringBuilder ("UPDATE I_DiscountSchema ")
			.append("SET I_IsImported='N', Updated=SysDate ")
			.append("WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog (0, null, new BigDecimal (no), "@Errors@");
		addLog (0, null, new BigDecimal (noInsertpl), "@M_Discount_Squema@: @Inserted@");
		addLog (0, null, new BigDecimal (noInsertppvb), "@M_Discount_Squema Line@: @Inserted@");
		return "";
		//	doIt		
	}
	
	public String getWhereClause() {
		StringBuilder msgreturn = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);
		return msgreturn.toString();
	}
}//Import DIscount Schema
