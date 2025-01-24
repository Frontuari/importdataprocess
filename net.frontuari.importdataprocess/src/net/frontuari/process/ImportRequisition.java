/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 * @autor	Jorge Colmenarez, jcolmenarez@frontuari.net Frontuari, C.A.		  * 
 *****************************************************************************/
package net.frontuari.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.model.ImportValidator;
import org.adempiere.process.ImportProcess;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.ModelValidationEngine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.compiere.util.Env;

import net.frontuari.base.CustomProcess;

import net.frontuari.model.X_I_Requisition;

/**
 *	Import Requisition from I_Requisition
 *  @author Jorge Colmenarez, Frontuari, C.A. https://frontuari.net
 * 	@version 	$Id: ImportRequisition.java,v 1.0 2021/07/11 10:08 jlctmaster Exp $
 */
@Process
public class ImportRequisition extends CustomProcess implements ImportProcess {

	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;
	/**	Document Action					*/
	private String			m_docAction = MRequisition.DOCACTION_Prepare;
	
	public ImportRequisition() {
	}
	
	@Override
	public String getImportTableName() {
		return X_I_Requisition.Table_Name;
	}

	@Override
	public String getWhereClause() {
		StringBuilder msgreturn = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);
		return msgreturn.toString();
	}

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
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = "Y".equals(para[i].getParameter());
			else if (name.equals("DocAction"))
				m_docAction = (String)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		StringBuilder sql = null;
		int no = 0;
		StringBuilder clientCheck = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE FROM I_Requisition ")
				  .append("WHERE I_IsImported='Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}
		
		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_Requisition ")
			  .append("SET AD_Client_ID = COALESCE (AD_Client_ID,").append (m_AD_Client_ID).append ("),")
			  .append(" IsActive = COALESCE (IsActive, 'Y'),")
			  .append(" Created = COALESCE (Created, SysDate),")
			  .append(" CreatedBy = COALESCE (CreatedBy, 0),")
			  .append(" Updated = COALESCE (Updated, SysDate),")
			  .append(" UpdatedBy = COALESCE (UpdatedBy, 0),")
			  .append(" Description = substring(Description,0,250),")
			  .append(" I_ErrorMsg = ' ',")
			  .append(" I_IsImported = 'N' ")
			  .append("WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info ("Reset=" + no);
		
		ModelValidationEngine.get().fireImportValidate(this, null, null, ImportValidator.TIMING_BEFORE_VALIDATE);
		
		//	Set Organization Trx
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	org
				  .append("SET AD_OrgTrx_ID=(SELECT AD_Org_ID FROM AD_Org d WHERE d.Value=o.OrgValue")
				  .append(" AND o.AD_Client_ID=d.AD_Client_ID) ")
				  .append("WHERE AD_OrgTrx_ID IS NULL AND OrgValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		
		sql = new StringBuilder ("UPDATE I_Requisition o ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '")
				.append("WHERE (AD_OrgTrx_ID IS NULL OR AD_OrgTrx_ID=0")
				.append(" OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_OrgTrx_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Org=" + no);
		
		//	Set Document Type
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Document Type Name
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType='POR' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid Doc Type Name
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid DocTypeName, ' ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid DocTypeName=" + no);
		
		//	Set User Requisition
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	User Name
			  .append("SET AD_User_ID=(SELECT MAX(AD_User_ID) FROM AD_User u WHERE u.Name=o.UserName")
			  .append(" AND o.AD_Client_ID=u.AD_Client_ID AND u.IsActive = 'Y') ")
			  .append("WHERE AD_User_ID IS NULL AND UserName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set User=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid User Name
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid UserName, ' ")
			  .append("WHERE AD_User_ID IS NULL AND UserName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid UserName=" + no);
		
		//	Set Warehouse
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Warehouse
			  .append("SET M_Warehouse_ID=(SELECT MAX(M_Warehouse_ID) FROM M_Warehouse w WHERE w.Value=o.WarehouseValue")
			  .append(" AND o.AD_Client_ID=w.AD_Client_ID) ")
			  .append("WHERE M_Warehouse_ID IS NULL AND WarehouseValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Warehouse=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid Warehouse
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Warehouse, ' ")
			  .append("WHERE M_Warehouse_ID IS NULL AND WarehouseValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Warehouse=" + no);
		
		//	Set Price List
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Price List
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.Name=o.PriceListName")
			  .append(" AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND PriceListName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid PriceList
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid PriceList, ' ")
			  .append("WHERE M_PriceList_ID IS NULL AND PriceListName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid PriceList=" + no);
		
		//	Set Activity
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Activity
			  .append("SET C_Activity_ID=(SELECT MAX(C_Activity_ID) FROM C_Activity a WHERE a.Value=o.ActivityValue")
			  .append(" AND o.AD_Client_ID=a.AD_Client_ID) ")
			  .append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Activity=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid Activity
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, ' ")
			  .append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Activity=" + no);
		
		//	Set Project
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Project
			  .append("SET C_Project_ID=(SELECT MAX(C_Project_ID) FROM C_Project p WHERE p.Value=o.ProjectValue")
			  .append(" AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_Project_ID IS NULL AND ProjectValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Project=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid Project
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Project, ' ")
			  .append("WHERE C_Project_ID IS NULL AND ProjectValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Project=" + no);
		
		//	Set Campaign
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Campaign
			  .append("SET C_Campaign_ID=(SELECT MAX(C_Campaign_ID) FROM C_Campaign c WHERE c.Value=o.CampaignValue")
			  .append(" AND o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE C_Campaign_ID IS NULL AND CampaignValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Campaign=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid Campaign
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Campaign, ' ")
			  .append("WHERE C_Campaign_ID IS NULL AND CampaignValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Campaign=" + no);
		
		//	Set User1
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	User1
			  .append("SET User1_ID=(SELECT MAX(ev.C_ElementValue_ID) FROM C_ElementValue ev ")
			  .append(" JOIN C_AcctSchema_Element e ON e.C_Element_ID = ev.C_Element_ID AND e.AD_Client_ID=ev.AD_Client_ID ")
			  .append(" WHERE e.ElementType = 'U1' AND ev.Value=o.User1Value AND o.AD_Client_ID=ev.AD_Client_ID) ")
			  .append("WHERE User1_ID IS NULL AND User1Value IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Cost Center=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid User1
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Cost Center, ' ")
			  .append("WHERE User1_ID IS NULL AND User1Value IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Cost Center=" + no);
		
		//	Set Business Partner
		sql = new StringBuilder ("UPDATE I_Requisition o ")	//	Business Partner
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp WHERE bp.Value=o.BPartnerValue")
			  .append(" AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BPartner=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")	//	Error Invalid Business Partner
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Business Partner, ' ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Business Partner=" + no);
		
		//	Product
		sql = new StringBuilder ("UPDATE I_Requisition o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.ProductValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from Value=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.UPC=p.UPC AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND UPC IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from UPC=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.SKU=p.SKU AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND SKU IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product fom SKU=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' ")
			  .append("WHERE M_Product_ID IS NULL AND (ProductValue IS NOT NULL OR UPC IS NOT NULL OR SKU IS NOT NULL)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product=" + no);

		//	Charge
		sql = new StringBuilder ("UPDATE I_Requisition o ")
			  .append("SET C_Charge_ID=(SELECT MAX(C_Charge_ID) FROM C_Charge c")
			  .append(" WHERE o.ChargeName=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE C_Charge_ID IS NULL AND ChargeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Charge=" + no);
		sql = new StringBuilder ("UPDATE I_Requisition ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Charge, ' ")
				  .append("WHERE C_Charge_ID IS NULL AND (ChargeName IS NOT NULL)")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Charge=" + no);
		//
		
		sql = new StringBuilder ("UPDATE I_Requisition ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Product and Charge, ' ")
				  .append("WHERE M_Product_ID IS NOT NULL AND C_Charge_ID IS NOT NULL ")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product and Charge exclusive=" + no);
		
		//	UoM
		sql = new StringBuilder ("UPDATE I_Requisition o ")
				  .append("SET C_UoM_ID=(SELECT MAX(C_UoM_ID) FROM C_UoM c")
				  .append(" WHERE o.UoMName=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
				  .append("WHERE C_UoM_ID IS NULL AND UoMName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set UoM=" + no);
		// Set proper error message
		sql = new StringBuilder ("UPDATE I_Requisition i ")
				.append("SET C_UOM_ID = (SELECT MAX(C_UOM_ID) FROM C_UOM u WHERE u.X12DE355=i.X12DE355 AND u.AD_Client_ID IN (0,i.AD_Client_ID))")
				.append("WHERE C_UOM_ID IS NULL AND X12DE355 IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.INFO)) log.info("Set UOM=" + no);
			//
		sql = new StringBuilder ("UPDATE I_Requisition ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found UOM, ' ")
			  .append("WHERE C_UoM_ID IS NULL AND (UoMName IS NOT NULL OR X12DE355 IS NOT NULL) AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No UoM=" + no);
		
		ModelValidationEngine.get().fireImportValidate(this, null, null, ImportValidator.TIMING_AFTER_VALIDATE);

		commitEx();
		if (p_IsValidateOnly)
		{
			return "OK";
		}
		
		//	****	Process New Requisition	****
		
		int noInsert = 0;
		int noInsertLine = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Timestamp now = new Timestamp(System.currentTimeMillis());
		
		//	Go through Requisition Records w/o
		sql = new StringBuilder ("SELECT * FROM I_Requisition ")
			  .append("WHERE I_IsImported='N'").append (clientCheck)
			.append(" ORDER BY AD_OrgTrx_ID,AD_User_ID,C_DocType_ID,DocumentNo,I_Requisition_ID");
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			rs = pstmt.executeQuery ();
			
			int oldUser_ID = 0;
			String oldDocumentNo = "";
			//
			MRequisition req = null;
			int lineNo = 0;
			while (rs.next ())
			{
				X_I_Requisition imp = new X_I_Requisition(getCtx(), rs, get_TrxName());
				String cmpDocumentNo = imp.getDocumentNo();
				if (cmpDocumentNo == null)
					cmpDocumentNo = "";
				//	New Requisition
				if(oldUser_ID != imp.getAD_User_ID()
					|| !oldDocumentNo.equals(cmpDocumentNo))
				{
					if(req != null)
					{
						if (m_docAction != null && m_docAction.length() > 0)
						{
							req.setDocAction(m_docAction);
							if(!req.processIt (m_docAction)) {
								log.warning("Requisition Process Failed: " + req + " - " + req.getProcessMsg());
								DB.close(rs, pstmt);
								throw new IllegalStateException("Order Process Failed: " + req + " - " + req.getProcessMsg());
								
							}
						}
						req.saveEx();
					}
					oldUser_ID = imp.getAD_User_ID();
					oldDocumentNo = imp.getDocumentNo();
					if (oldDocumentNo == null)
						oldDocumentNo = "";
					req = new MRequisition(getCtx(), 0, get_TrxName());
					req.setAD_Org_ID(imp.getAD_OrgTrx_ID());
					req.setC_DocType_ID(imp.getC_DocType_ID());
					req.setAD_User_ID(imp.getAD_User_ID());
					//	Valid DocumentNo
					if(imp.getDocumentNo() != null)
						req.setDocumentNo(imp.getDocumentNo());
					req.setDescription(imp.getDescription());
					req.setHelp(imp.getHelp());
					//	Valid Priority Rule
					if(imp.getPriorityRule() != null)
						req.setPriorityRule(imp.getPriorityRule());
					else
						req.setPriorityRule(MRequisition.PRIORITYRULE_Medium);
					//	Valid Date Required
					if(imp.getDateRequired() != null)
						req.setDateRequired(imp.getDateRequired());
					else
						req.setDateRequired(now);
					//	Valid Date Document
					if(imp.getDateDoc() != null)
						req.setDateDoc(imp.getDateDoc());
					else
						req.setDateDoc(now);
					//	Valid Warehouse
					if(imp.getM_Warehouse_ID() > 0)
						req.setM_Warehouse_ID(imp.getM_Warehouse_ID());
					else
						req.setM_Warehouse_ID(Env.getContextAsInt(getCtx(), "#M_Warehouse_ID"));
					//	Valid PriceList
					if(imp.getM_PriceList_ID() > 0)
						req.setM_PriceList_ID(imp.getM_PriceList_ID());
					else
						req.setM_PriceList_ID();
					//	Set Reference Data
					if(imp.getC_Project_ID() > 0)
						req.set_ValueOfColumn("C_Project_ID", imp.getC_Project_ID());
					if(imp.getC_Campaign_ID() > 0)
						req.set_ValueOfColumn("C_Campaign_ID", imp.getC_Campaign_ID());
					if(imp.getC_Activity_ID() > 0)
						req.set_ValueOfColumn("C_Activity_ID", imp.getC_Activity_ID());
					if(imp.getUser1_ID() > 0)
						req.set_ValueOfColumn("User1_ID", imp.getUser1_ID());
					//	Save
					req.saveEx(get_TrxName());
					noInsert++;
					lineNo = 10;
				}
				//	Update Import Requisition ID
				imp.setM_Requisition_ID(req.getM_Requisition_ID());
				//	New Requisition Line
				MRequisitionLine line = new MRequisitionLine(req);
				line.setLine(lineNo);
				lineNo += 10;
				//	Validate BPartner
				if(imp.getC_BPartner_ID() > 0)
					line.setC_BPartner_ID(imp.getC_BPartner_ID());
				//	Validate Product
				if(imp.getM_Product_ID()> 0)
				{
					line.setM_Product_ID(imp.getM_Product_ID());
					if(imp.getC_UOM_ID() > 0)
						line.setC_UOM_ID(imp.getC_UOM_ID());
					else
						line.setC_UOM_ID(imp.getM_Product().getC_UOM_ID());
				}
				//	Validate Charge
				if(imp.getC_Charge_ID() > 0)
					line.setC_Charge_ID(imp.getC_Charge_ID());
				
				line.setQty(imp.getQty());
				line.setPriceActual(imp.getPriceActual());
				line.setDescription(imp.getLineDescription());
				//	Set Reference Data
				if(imp.getC_Project_ID() > 0)
					line.set_ValueOfColumn("C_Project_ID", imp.getC_Project_ID());
				if(imp.getC_Campaign_ID() > 0)
					line.set_ValueOfColumn("C_Campaign_ID", imp.getC_Campaign_ID());
				if(imp.getC_Activity_ID() > 0)
					line.set_ValueOfColumn("C_Activity_ID", imp.getC_Activity_ID());
				if(imp.getUser1_ID() > 0)
					line.set_ValueOfColumn("User1_ID", imp.getUser1_ID());
				//	Save Line
				line.saveEx(get_TrxName());
				//	Update Import Requisition Line ID
				imp.setM_RequisitionLine_ID(line.getM_RequisitionLine_ID());
				//	Update Import Row
				imp.setI_IsImported(true);
				imp.setProcessed(true);
				//	Save
				if(imp.save())
					noInsertLine++;
			}

			if (req != null)
			{
				if (m_docAction != null && m_docAction.length() > 0)
				{
					req.setDocAction(m_docAction);
					if(!req.processIt (m_docAction)) {
						log.warning("Order Process Failed: " + req + " - " + req.getProcessMsg());
						throw new IllegalStateException("Order Process Failed: " + req + " - " + req.getProcessMsg());	
					}
				}
				req.saveEx();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Order - " + sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Set Error to indicator to not imported
		sql = new StringBuilder ("UPDATE I_Requisition ")
			.append("SET I_IsImported='N', Updated=SysDate ")
			.append("WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog (0, null, new BigDecimal (no), "@Errors@");
		//
		addLog (0, null, new BigDecimal (noInsert), "@M_Requisition_ID@: @Inserted@");
		addLog (0, null, new BigDecimal (noInsertLine), "@M_Requisition_ID@: @Inserted@");
		StringBuilder msgreturn = new StringBuilder("#").append(noInsert).append("/").append(noInsertLine);
		return msgreturn.toString();
	}
}
