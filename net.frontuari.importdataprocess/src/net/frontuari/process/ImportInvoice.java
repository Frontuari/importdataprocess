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
 *****************************************************************************/
package net.frontuari.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocation;
import org.compiere.model.MUser;
import org.compiere.model.X_I_Invoice;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.compiere.util.Env;

import net.frontuari.base.CustomProcess;
import net.frontuari.custom.model.FTUMInvoice;


/**
 *	Import Invoice from I_Invoice
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ImportInvoice.java,v 1.1 2007/09/05 09:27:31 cruiz Exp $
 */
@Process
public class ImportInvoice extends CustomProcess
{
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Organization to be imported to		*/
	private int				m_AD_Org_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Document Action					*/
	private String			m_docAction =  "" ;//MInvoice.DOCACTION_Prepare;


	/** Effective						*/
	private Timestamp		m_DateValue = null;
	
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Client_ID"))
				m_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("AD_Org_ID"))
				m_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("DocAction")) {
				String action = (String)para[i].getParameter();
				
			if (action != null) {
				m_docAction = action;
			}}
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
				
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
			
			
		}
		if (m_DateValue == null)
			m_DateValue = new Timestamp (System.currentTimeMillis());
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return clear Message
	 *  @throws Exception
	 */
	protected String doIt() throws java.lang.Exception
	{
		StringBuilder sql = null;
		int no = 0;
		StringBuilder clientCheck = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE FROM I_Invoice ")
				  .append("WHERE I_IsImported='Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}

		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET AD_Client_ID = COALESCE (AD_Client_ID,").append (m_AD_Client_ID).append ("),")
			 // .append(" AD_Org_ID = COALESCE (AD_Org_ID,").append (m_AD_Org_ID).append ("),")
			  .append(" IsActive = COALESCE (IsActive, 'Y'),")
			  .append(" Created = COALESCE (Created, SysDate),")
			  .append(" CreatedBy = COALESCE (CreatedBy, 0),")
			  .append(" Updated = COALESCE (Updated, SysDate),")
			  .append(" UpdatedBy = COALESCE (UpdatedBy, 0),")
			  .append(" I_ErrorMsg = ' ',")
			  .append(" I_IsImported = 'N' ")
			  .append("WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info ("Reset=" + no);
		
		//	Org
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET AD_Org_ID=(SELECT AD_Org_ID FROM AD_Org d WHERE d.Value=o.OrgValue")
			  .append(" AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE (AD_Org_ID IS NULL OR AD_Org_ID = 0) AND OrgValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		

		sql = new StringBuilder ("UPDATE I_Invoice o ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '")
			.append("WHERE (AD_Org_ID IS NULL OR AD_Org_ID=0")
			.append(") AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Org=" + no);
		

		//added by Adonis
		sql = new StringBuilder ("UPDATE I_Invoice o")
			.append(" SET C_Currency_ID=(SELECT C_Currency_ID FROM C_Currency c")
			.append(" WHERE o.ISO_Code=c.ISO_Code)")
			.append(" WHERE C_Currency_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("doIt- Set Currency=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Invoice ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Currency,' ")
			.append("WHERE C_Currency_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("Invalid Currency=" + no);

		//	Document Type - PO - SO
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType IN ('API','APC') AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set PO DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType IN ('ARI','ARC') AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set SO DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType IN ('API','ARI','APC','ARC') AND o.AD_Client_ID=d.AD_Client_ID) ")
			//+ "WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid DocTypeName, ' ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid DocTypeName=" + no);
		//	DocType Default
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType='API' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set PO Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType='ARI' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set SO Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType IN('ARI','API') AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No DocType, ' ")
			  .append("WHERE C_DocType_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No DocType=" + no);

		//	Set IsSOTrx
		sql = new StringBuilder ("UPDATE I_Invoice o SET IsSOTrx='Y' ")
			  .append("WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='ARI' AND o.AD_Client_ID=d.AD_Client_ID)")
			  .append(" AND C_DocType_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set IsSOTrx=Y=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o SET IsSOTrx='N' ")
			  .append("WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='API' AND o.AD_Client_ID=d.AD_Client_ID)")
			  .append(" AND C_DocType_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set IsSOTrx=N=" + no);

		//	Price List
		//PriceList By Name
		sql = new StringBuilder ("UPDATE I_Invoice o ")
		 .append("SET M_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList c")
		 .append(" WHERE o.PriceListName=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
		 .append("WHERE M_PriceList_ID IS NULL AND PriceListName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set User1=" + no);
		// Set proper error message
	/*	sql = new StringBuilder ("UPDATE I_Invoice ")
		 .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found M_PriceList_ID, ' ")
		 .append("WHERE M_PriceList_ID IS NULL AND PriceListName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
		log.warning("No M_PriceList_ID=" + no);*/
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'")
			  .append(" AND p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default Currency PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'")
			  .append(" AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p ")
			  .append(" WHERE p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Currency PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p ")
			  .append(" WHERE p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PriceList=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No PriceList, ' ")
			  .append("WHERE M_PriceList_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No PriceList=" + no);

		//	Payment Term
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p")
			  .append(" WHERE o.PaymentTermValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_PaymentTerm_ID IS NULL AND PaymentTermValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PaymentTerm=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_PaymentTerm_ID=(SELECT MAX(C_PaymentTerm_ID) FROM C_PaymentTerm p")
			  .append(" WHERE p.IsDefault='Y' AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_PaymentTerm_ID IS NULL AND o.PaymentTermValue IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default PaymentTerm=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No PaymentTerm, ' ")
			  .append("WHERE C_PaymentTerm_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No PaymentTerm=" + no);

		// globalqss - Add project and activity
		//	Project
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_Project_ID=(SELECT C_Project_ID FROM C_Project p")
			  .append(" WHERE o.ProjectValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_Project_ID IS NULL AND ProjectValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Project=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Project, ' ")
				  .append("WHERE C_Project_ID IS NULL AND (ProjectValue IS NOT NULL)")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Project=" + no);
		//	Activity
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_Activity_ID=(SELECT C_Activity_ID FROM C_Activity p")
			  .append(" WHERE o.ActivityValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Activity=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, ' ")
				  .append("WHERE C_Activity_ID IS NULL AND (ActivityValue IS NOT NULL)")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Activity=" + no);
		// globalqss - add charge
		//	Charge
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_Charge_ID=(SELECT C_Charge_ID FROM C_Charge p")
			  .append(" WHERE o.ChargeName=p.Name AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_Charge_ID IS NULL AND ChargeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Charge=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Charge, ' ")
				  .append("WHERE C_Charge_ID IS NULL AND (ChargeName IS NOT NULL)")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Charge=" + no);
		//
		
		//	BP from EMail
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u")
			  .append(" WHERE o.EMail=u.EMail AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) ")
			  .append("WHERE C_BPartner_ID IS NULL AND EMail IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from EMail=" + no);
		//	BP from ContactName
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u")
			  .append(" WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) ")
			  .append("WHERE C_BPartner_ID IS NULL AND ContactName IS NOT NULL")
			  .append(" AND EXISTS (SELECT Name FROM AD_User u WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL GROUP BY Name HAVING COUNT(*)=1)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from ContactName=" + no);
		//	BP from Value
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE o.BPartnerValue=bp.Value AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from Value=" + no);
		//	BP from TaxID
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE o.BPTaxID=bp.TaxID AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPTaxID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from TaxID=" + no);
		//	Default BP
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_BPartner_ID=(SELECT C_BPartnerCashTrx_ID FROM AD_ClientInfo c")
			  .append(" WHERE o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NULL AND Name IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default BP=" + no);
		//BP Location By Name
		sql = new StringBuilder ("UPDATE I_Invoice o ")
		  .append("SET C_BPartner_Location_ID=(SELECT C_BPartner_Location_ID FROM C_BPartner_Location c")
		  .append(" WHERE o.bp_location_name=c.Name AND o.AD_Client_ID=c.AD_Client_ID AND o.C_BPartner_ID = c.C_BPartner_ID) ")
		  .append("WHERE C_BPartner_Location_ID IS NULL AND bp_location_name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set C_BPartner_Location_ID=" + no);
		
//		Existing Location ? Exact Match
			sql = new StringBuilder ("UPDATE I_Invoice o ")
				  .append("SET C_BPartner_Location_ID=(SELECT C_BPartner_Location_ID")
				  .append(" FROM C_BPartner_Location bpl INNER JOIN C_Location l ON (bpl.C_Location_ID=l.C_Location_ID)")
				  .append(" WHERE o.C_BPartner_ID=bpl.C_BPartner_ID AND bpl.AD_Client_ID=o.AD_Client_ID")
				  .append(" AND ((o.Address1 IS NULL AND l.Address1 IS NULL) OR o.Address1=l.Address1)")
				  .append(" AND ((o.Address2 IS NULL AND l.Address2 IS NULL) OR o.Address2=l.Address2)")
				  .append(" AND ((o.City IS NULL AND l.City IS NULL) OR o.City=l.City)")
				  .append(" AND ((o.Postal IS NULL AND l.Postal IS NULL) OR o.Postal=l.Postal)")
				  .append(" AND COALESCE(o.C_Region_ID,0)=COALESCE(l.C_Region_ID,0)")
				  .append(" AND COALESCE(o.C_Country_ID,0)=COALESCE(l.C_Country_ID,0)) ")
				  .append("WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL")
				  .append(" AND I_IsImported='N'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Found Location=" + no);
			//	Set Location from BPartner
			sql = new StringBuilder ("UPDATE I_Invoice o ")
				  .append("SET C_BPartner_Location_ID=(SELECT MAX(C_BPartner_Location_ID) FROM C_BPartner_Location l")
				  .append(" WHERE l.C_BPartner_ID=o.C_BPartner_ID AND o.AD_Client_ID=l.AD_Client_ID")
				  .append(" AND ((l.IsBillTo='Y' AND o.IsSOTrx='Y') OR o.IsSOTrx='N')")
				  .append(") ")
				  .append("WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set BP Location from BP=" + no);
			//
			sql = new StringBuilder ("UPDATE I_Invoice ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No BP Location, ' ")
				  .append("WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				log.warning ("No BP Location=" + no);

		//	Set Country
		/**
		sql = new StringBuffer ("UPDATE I_Invoice o "
			  + "SET CountryCode=(SELECT MAX(CountryCode) FROM C_Country c WHERE c.IsDefault='Y'"
			  + " AND c.AD_Client_ID IN (0, o.AD_Client_ID)) "
			  + "WHERE C_BPartner_ID IS NULL AND CountryCode IS NULL AND C_Country_ID IS NULL"
			  + " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Country Default=" + no);
		**/
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_Country_ID=(SELECT C_Country_ID FROM C_Country c")
			  .append(" WHERE o.CountryCode=c.CountryCode AND c.AD_Client_ID IN (0, o.AD_Client_ID)) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL AND CountryCode IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Country=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Country, ' ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Country=" + no);

		//	Set Region
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("Set RegionName=(SELECT MAX(Name) FROM C_Region r")
			  .append(" WHERE r.IsDefault='Y' AND r.C_Country_ID=o.C_Country_ID")
			  .append(" AND r.AD_Client_ID IN (0, o.AD_Client_ID)) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Region Default=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("Set C_Region_ID=(SELECT C_Region_ID FROM C_Region r")
			  .append(" WHERE r.Name=o.RegionName AND r.C_Country_ID=o.C_Country_ID")
			  .append(" AND r.AD_Client_ID IN (0, o.AD_Client_ID)) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Region=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Region, ' ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL ")
			  .append(" AND EXISTS (SELECT * FROM C_Country c")
			  .append(" WHERE c.C_Country_ID=o.C_Country_ID AND c.HasRegion='Y')")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Region=" + no);

		//	Product
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.ProductValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from Value=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.UPC=p.UPC AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND UPC IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from UPC=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.SKU=p.SKU AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND SKU IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product fom SKU=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' ")
			  .append("WHERE M_Product_ID IS NULL AND (ProductValue IS NOT NULL OR UPC IS NOT NULL OR SKU IS NOT NULL)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product=" + no);

		// globalqss - charge and product are exclusive
		sql = new StringBuilder ("UPDATE I_Invoice ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Product and Charge, ' ")
				  .append("WHERE M_Product_ID IS NOT NULL AND C_Charge_ID IS NOT NULL ")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product and Charge exclusive=" + no);

			//	Tax
		
		
				//Tax By Name
		sql = new StringBuilder ("UPDATE I_Invoice o ")
				.append("SET C_Tax_ID=(SELECT C_Tax_ID FROM C_Tax c")
				.append(" WHERE o.TaxName=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
				.append("WHERE C_Tax_ID IS NULL AND TaxName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set TaxName=" + no);
				// Set proper error message
				/*sql = new StringBuilder ("UPDATE I_Invoice ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found C_Tax_ID, ' ")
				.append("WHERE C_Tax_ID IS NULL AND TaxName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
				no = DB.executeUpdate(sql.toString(), get_TrxName());
				if (no != 0)
				log.warning("No C_Tax_ID=" + no);*/

		sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET C_Tax_ID=(SELECT MAX(C_Tax_ID) FROM C_Tax t")
			  .append(" WHERE o.TaxIndicator=t.TaxIndicator AND o.AD_Client_ID=t.AD_Client_ID) ")
			  .append("WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Tax=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Tax, ' ")
			  .append("WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Tax=" + no);
		
		// Set 1099 Box
		sql = new StringBuilder ("UPDATE I_Invoice o ")
				.append("SET C_1099Box_ID=(SELECT C_1099Box_ID FROM C_1099Box a")
				.append(" WHERE o.C_1099Box_Value=a.Value AND a.AD_Client_ID = o.AD_Client_ID) ")
				.append(" WHERE C_1099Box_ID IS NULL and C_1099Box_Value IS NOT NULL")
				.append(" AND I_IsImported<>'Y' AND IsSOTrx='N'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set C_1099Box_ID=" + no);
		sql = new StringBuilder ("UPDATE I_Invoice ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid C_1099Box_Value, ' ")
				.append("WHERE C_1099Box_ID IS NULL AND (C_1099Box_Value IS NOT NULL)")
				.append(" AND I_IsImported<>'Y' AND IsSOTrx='N'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid C_1099Box_Value=" + no);
		
		// Set Conversion Type
				sql = new StringBuilder ("UPDATE I_Invoice o ")
						.append("SET C_ConversionType_ID=(SELECT C_ConversionType_ID FROM C_ConversionType a")
						.append(" WHERE o.ConversionTypeValue=a.Value AND (a.AD_Client_ID = o.AD_Client_ID OR a.AD_Client_ID = 0)) ")
						.append(" WHERE C_ConversionType_ID IS NULL and ConversionTypeValue IS NOT NULL")
						.append(" AND I_IsImported<>'Y'").append (clientCheck);
				no = DB.executeUpdate(sql.toString(), get_TrxName());
				log.fine("Set C_ConversionType_ID=" + no);
				sql = new StringBuilder ("UPDATE I_Invoice ")
						.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid ConversionTypeValue, ' ")
						.append("WHERE C_ConversionType_ID IS NULL AND (ConversionTypeValue IS NOT NULL)")
						.append(" AND I_IsImported<>'Y' ").append (clientCheck);
				no = DB.executeUpdate(sql.toString(), get_TrxName());
				if (no != 0)
					log.warning ("Invalid ConversionTypeValue=" + no);
				

				//User1
				sql = new StringBuilder ("UPDATE I_Invoice o ")
						  .append("SET User1_ID=(SELECT C_ElementValue_ID FROM C_ElementValue c")
						  .append(" WHERE o.User1Name=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
						  .append("WHERE User1_ID IS NULL AND User1Name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
					no = DB.executeUpdate(sql.toString(), get_TrxName());
					if (log.isLoggable(Level.FINE)) log.fine("Set User1=" + no);
					// Set proper error message
					sql = new StringBuilder ("UPDATE I_Invoice ")
						  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found User1_ID, ' ")
						  .append("WHERE User1_ID IS NULL AND User1Name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
					no = DB.executeUpdate(sql.toString(), get_TrxName());
					if (no != 0)
						log.warning("No User1Name=" + no);
			// David Castillo 04/10/2022
			
			//SetSalesRep 
			sql = new StringBuilder ("UPDATE I_Invoice o ")
			  .append("SET SalesRep_ID=(SELECT AD_User_ID FROM AD_User c")
			  .append(" WHERE o.SalesRep_Name=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE SalesRep_ID IS NULL AND SalesRep_Name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set User1=" + no);
			// Set proper error message
			sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found SalesRep_ID, ' ")
			  .append("WHERE SalesRep_ID IS NULL AND SalesRep_Name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
				if (no != 0)
				log.warning("No SalesRep_ID=" + no);
						
		// C_Order_ID
		sql = new StringBuilder ("UPDATE I_Invoice i ")
		  .append("SET C_Order_ID=(SELECT MAX(C_Order_ID) FROM C_Order o")
		  .append(" WHERE i.OrderDocumentNo=o.DocumentNo AND i.AD_Client_ID=o.AD_Client_ID AND i.AD_Org_ID = o.AD_Org_ID) ")
		  .append("WHERE C_Order_ID IS NULL AND OrderDocumentNo IS NOT NULL")
		  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Purchase/Sales Order=" + no);	
			
		sql = new StringBuilder ("UPDATE I_Invoice ")	// No DocType
		  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No Purchase/Sales Order, ' ")
		  .append("WHERE C_Order_ID IS NULL AND OrderDocumentNo IS NOT NULL")
		  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
			log.warning ("No Purchase/Sales Order=" + no);
			
	//	Order Line from Order and Product
		sql = new StringBuilder ("UPDATE I_Invoice i ")
		  .append("SET C_OrderLine_ID=(SELECT MAX(C_OrderLine_ID) FROM C_OrderLine ol")
		  .append(" WHERE i.C_Order_ID=ol.C_Order_ID AND i.AD_Client_ID=ol.AD_Client_ID  ")
		  .append(" AND i.M_Product_ID=ol.M_Product_ID) ")
		  .append("WHERE C_OrderLine_ID IS NULL AND C_Order_ID IS NOT NULL AND M_Product_ID IS NOT NULL")
		  .append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set OrderLine=" + no);
			
	// MInOut from Order
			
		sql = new StringBuilder ("UPDATE I_Invoice i ")
		  .append("SET M_InOut_ID=(SELECT MAX(M_InOut_ID) FROM M_InOut o")
		  .append(" WHERE i.InOutDocumentNo=o.DocumentNo AND i.AD_Client_ID=o.AD_Client_ID  AND i.AD_Org_ID=o.AD_Org_ID)")
		  .append("WHERE M_InOut_ID IS NULL AND InOutDocumentNo IS NOT NULL")
		  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set InOut=" + no);	
		
	//MInOutLine from header
		
	sql = new StringBuilder ("UPDATE I_Invoice i ")
	  .append("SET M_InOutLine_ID=(SELECT MAX(M_InOutLine_ID) FROM M_InOutLine ol")
	  .append(" WHERE i.M_InOut_ID=ol.M_InOut_ID AND i.AD_Client_ID=ol.AD_Client_ID  ")
	  .append(" AND i.M_Product_ID=ol.M_Product_ID) ")
	  .append("WHERE M_InOutLine_ID IS NULL AND M_InOut_ID IS NOT NULL AND M_Product_ID IS NOT NULL")
	  .append(" AND I_IsImported<>'Y'").append (clientCheck);
	   no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set M_InOutLine_ID=" + no);
		
		//InvoiceAffected
		sql = new StringBuilder ("UPDATE I_Invoice i ")
		  .append("SET LVE_invoiceAffected_ID=(SELECT MAX(C_Invoice_ID) FROM C_Invoice o")
		  .append(" WHERE i.InvoiceAffectedDocumentNo=o.DocumentNo AND i.AD_Client_ID=o.AD_Client_ID AND i.AD_Org_ID = o.AD_Org_ID) ")
		  .append("WHERE LVE_invoiceAffected_ID IS NULL AND InvoiceAffectedDocumentNo IS NOT NULL")
		  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Invoice Affected =" + no);	
			
		sql = new StringBuilder ("UPDATE I_Invoice ")	// No DocType
		  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No Invoice Affected, ' ")
		  .append("WHERE LVE_invoiceAffected_ID IS NULL AND InvoiceAffectedDocumentNo IS NOT NULL")
		  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
			log.warning ("No Invoice Affected=" + no);

		commitEx();
		
		if (p_IsValidateOnly)
		{
			return "Validated";
		}
		//	-- New BPartner ---------------------------------------------------

		//	Go through Invoice Records w/o C_BPartner_ID
		sql = new StringBuilder ("SELECT * FROM I_Invoice ")
			  .append("WHERE I_IsImported='N' AND C_BPartner_ID IS NULL").append (clientCheck)
			  .append(" ORDER BY DocumentNo,C_BPartner_ID,C_DocType_ID,DateInvoiced ASC ");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				X_I_Invoice imp = new X_I_Invoice (getCtx(), rs, get_TrxName());
				if (imp.getBPartnerValue () == null)
				{
					if (imp.getEMail () != null)
						imp.setBPartnerValue (imp.getEMail ());
					else if (imp.getName () != null)
						imp.setBPartnerValue (imp.getName ());
					else
						continue;
				}
				if (imp.getName () == null)
				{
					if (imp.getContactName () != null)
						imp.setName (imp.getContactName ());
					else
						imp.setName (imp.getBPartnerValue ());
				}
				//	BPartner
				MBPartner bp = MBPartner.get (getCtx(), imp.getBPartnerValue(), get_TrxName());
				if (bp == null)
				{
					bp = new MBPartner (getCtx (), -1, get_TrxName());
					bp.setClientOrg (imp.getAD_Client_ID (), imp.getAD_Org_ID ());
					bp.setValue (imp.getBPartnerValue ());
					bp.setName (imp.getName ());
					if (!bp.save ())
						continue;
				}
				imp.setC_BPartner_ID (bp.getC_BPartner_ID ());
				
				//	BP Location
				MBPartnerLocation bpl = null; 
				MBPartnerLocation[] bpls = bp.getLocations(true);
				for (int i = 0; bpl == null && i < bpls.length; i++)
				{
					if (imp.getC_BPartner_Location_ID() == bpls[i].getC_BPartner_Location_ID())
						bpl = bpls[i];
					//	Same Location ID
					else if (imp.getC_Location_ID() == bpls[i].getC_Location_ID())
						bpl = bpls[i];
					//	Same Location Info
					else if (imp.getC_Location_ID() == 0)
					{
						MLocation loc = bpls[i].getLocation(false);
						if (loc.equals(imp.getC_Country_ID(), imp.getC_Region_ID(), 
								imp.getPostal(), "", imp.getCity(), 
								imp.getAddress1(), imp.getAddress2()))
							bpl = bpls[i];
					}
				}
				if (bpl == null)
				{
					//	New Location
					MLocation loc = new MLocation (getCtx (), 0, get_TrxName());
					loc.setAddress1 (imp.getAddress1 ());
					loc.setAddress2 (imp.getAddress2 ());
					loc.setCity (imp.getCity ());
					loc.setPostal (imp.getPostal ());
					if (imp.getC_Region_ID () != 0)
						loc.setC_Region_ID (imp.getC_Region_ID ());
					loc.setC_Country_ID (imp.getC_Country_ID ());
					if (!loc.save ())
						continue;
					//
					bpl = new MBPartnerLocation (bp);
					bpl.setC_Location_ID (imp.getC_Location_ID() > 0 ? imp.getC_Location_ID() : loc.getC_Location_ID());
					if (!bpl.save ())
						continue;
				}
				imp.setC_Location_ID (bpl.getC_Location_ID ());
				imp.setC_BPartner_Location_ID (bpl.getC_BPartner_Location_ID ());
				
				//	User/Contact
				if (imp.getContactName () != null 
					|| imp.getEMail () != null 
					|| imp.getPhone () != null)
				{
					MUser[] users = bp.getContacts(true);
					MUser user = null;
					for (int i = 0; user == null && i < users.length;  i++)
					{
						String name = users[i].getName();
						if (name.equals(imp.getContactName()) 
							|| name.equals(imp.getName()))
						{
							user = users[i];
							imp.setAD_User_ID (user.getAD_User_ID ());
						}
					}
					if (user == null)
					{
						user = new MUser (bp);
						if (imp.getContactName () == null)
							user.setName (imp.getName ());
						else
							user.setName (imp.getContactName ());
						user.setEMail (imp.getEMail ());
						user.setPhone (imp.getPhone ());
						if (user.save ())
							imp.setAD_User_ID (user.getAD_User_ID ());
					}
				}
				imp.saveEx();
			}	//	for all new BPartners
			//
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "CreateBP", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		sql = new StringBuilder ("UPDATE I_Invoice ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No BPartner, ' ")
			  .append("WHERE C_BPartner_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No BPartner=" + no);
		
		commitEx();
		
		//	-- New Invoices -----------------------------------------------------

		int noInsert = 0;
		int noInsertLine = 0;

		//	Go through Invoice Records w/o
		sql = new StringBuilder ("SELECT * FROM I_Invoice ")
			  .append("WHERE I_IsImported='N'").append (clientCheck)
			.append(" ORDER BY DocumentNo, C_BPartner_ID, C_DocType_ID, DateInvoiced, C_BPartner_Location_ID, I_Invoice_ID");
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			rs = pstmt.executeQuery ();
			//	Group Change
			int oldC_BPartner_ID = 0;
			int oldC_BPartner_Location_ID = 0;
			//	Added by Jorge Colmenarez, 2023-09-29 14:06, Distinct by DocType
			int oldC_DocType_ID = 0;
			//	End Jorge Colmenarez
			String oldDocumentNo = "";
			//
			FTUMInvoice invoice = null;
			int lineNo = 0;
			while (rs.next ())
			{
				X_I_Invoice imp = new X_I_Invoice (getCtx (), rs, get_TrxName());
				String cmpDocumentNo = imp.getDocumentNo();
				if (cmpDocumentNo == null)
					cmpDocumentNo = "";
				//	New Invoice
				if (oldC_BPartner_ID != imp.getC_BPartner_ID() 
					|| oldC_BPartner_Location_ID != imp.getC_BPartner_Location_ID()
					|| !oldDocumentNo.equals(cmpDocumentNo)	
					//	Added by Jorge Colmenarez, 2023-09-29 14:06, Distinct by DocType
					|| oldC_DocType_ID != imp.getC_DocType_ID())
					//	End Jorge Colmenarez
				{
					if (invoice != null)
					{//dont process if m_docAction is empty
						if (!m_docAction.equals("")) {
						if (!invoice.processIt(m_docAction)) {
							log.warning("Invoice Process Failed: " + invoice + " - " + invoice.getProcessMsg());
							throw new IllegalStateException("Invoice Process Failed: " + invoice + " - " + invoice.getProcessMsg());
						}	
						}
						invoice.saveEx();
					}
					//	Group Change
					oldC_BPartner_ID = imp.getC_BPartner_ID();
					oldC_BPartner_Location_ID = imp.getC_BPartner_Location_ID();
					oldDocumentNo = imp.getDocumentNo();
					//	Added by Jorge Colmenarez, 2023-09-29 14:06, Distinct by DocType
					oldC_DocType_ID = imp.getC_DocType_ID();
					//	End Jorge Colmenarez
					if (oldDocumentNo == null)
						oldDocumentNo = "";
					//
					invoice = new FTUMInvoice (getCtx(), 0, get_TrxName());
					invoice.setClientOrg (imp.getAD_Client_ID(), imp.getAD_Org_ID());
					invoice.setC_DocTypeTarget_ID(imp.getC_DocType_ID());
					invoice.setIsSOTrx(imp.isSOTrx());
					//added by david castillo 21/04/2021 invoice can be imported prepared
					invoice.setDocStatus(MInvoice.DOCSTATUS_Drafted);
					//end david castillo
					if (imp.getDocumentNo() != null)
					{
						invoice.setDocumentNo(imp.getDocumentNo());
						//	Support for set LVE_POInvoiceNo
						invoice.set_ValueOfColumn("LVE_POInvoiceNo",imp.getDocumentNo());
					}
					//	Added by Jorge Colmenarez, 2020-10-19 15:13 
					if(imp.get_ValueAsString("LVE_controlNumber") != "")
					{
						invoice.set_ValueOfColumn("LVE_controlNumber", imp.get_ValueAsString("LVE_controlNumber"));
					}
					// added by david castillo , 13/10/2022
					if(imp.get_ValueAsInt("LVE_invoiceAffected_ID") > 0) {
						invoice.set_ValueOfColumn("LVE_invoiceAffected_ID", imp.get_ValueAsInt("LVE_invoiceAffected_ID"));
					}
					//
					invoice.setC_BPartner_ID(imp.getC_BPartner_ID());
					invoice.setC_BPartner_Location_ID(imp.getC_BPartner_Location_ID());
					if (imp.getAD_User_ID() != 0)
						invoice.setAD_User_ID(imp.getAD_User_ID());
					//
					if (imp.getDescription() != null)
						invoice.setDescription(imp.getDescription());
					invoice.setC_PaymentTerm_ID(imp.getC_PaymentTerm_ID());
					invoice.setM_PriceList_ID(imp.getM_PriceList_ID());
					//	SalesRep from Import or the person running the import
					if (imp.getSalesRep_ID() != 0)
						invoice.setSalesRep_ID(imp.getSalesRep_ID());
					if (invoice.getSalesRep_ID() == 0)
						invoice.setSalesRep_ID(getAD_User_ID());
					//
					if (imp.getAD_OrgTrx_ID() != 0)
						invoice.setAD_OrgTrx_ID(imp.getAD_OrgTrx_ID());
					if (imp.getC_Activity_ID() != 0)
						invoice.setC_Activity_ID(imp.getC_Activity_ID());
					if (imp.getC_Campaign_ID() != 0)
						invoice.setC_Campaign_ID(imp.getC_Campaign_ID());
					if (imp.getC_Project_ID() != 0)
						invoice.setC_Project_ID(imp.getC_Project_ID());
					//
					if (imp.getDateInvoiced() != null)
						invoice.setDateInvoiced(imp.getDateInvoiced());
					if (imp.getDateAcct() != null)
						invoice.setDateAcct(imp.getDateAcct());


					if (imp.get_ValueAsInt("User1_ID") > 0)
						invoice.setUser1_ID(imp.get_ValueAsInt("User1_ID"));
					//Conversion Type
					int C_ConversionType_ID = imp.get_ValueAsInt("C_ConversionType_ID");
					if(C_ConversionType_ID>0)
						invoice.setC_ConversionType_ID(C_ConversionType_ID);
					//	Added by Jorge Colmenarez, 2023-09-28 09:23
					//	Support for Override Currency Rate
					invoice.setIsOverrideCurrencyRate(imp.get_ValueAsBoolean("IsOverrideCurrencyRate"));
					BigDecimal currencyRate = (BigDecimal)imp.get_Value("CurrencyRate");
					log.warning("Tasa="+currencyRate);
					if(currencyRate.compareTo(BigDecimal.ZERO)!=0)
						invoice.setCurrencyRate(currencyRate);
					else
						invoice.setCurrencyRate(new BigDecimal(0.00001));
					//	End Jorge Colmenarez
					
					//	Add Order to Invoice
					if(imp.get_ValueAsInt("M_InOut_ID") > 0 && imp.get_ValueAsInt("C_Order_ID") == 0)
					{
						MInOut io = new MInOut(getCtx(), imp.get_ValueAsInt("M_InOut_ID"), get_TrxName());
						if (io.getC_Order_ID() > 0) {
						imp.set_ValueOfColumn("C_Order_ID", io.getC_Order_ID());
						imp.saveEx();
						}
					}
					if(imp.get_ValueAsInt("C_Order_ID") > 0)
						invoice.setC_Order_ID(imp.get_ValueAsInt("C_Order_ID"));
					
					//
					invoice.saveEx();
					noInsert++;
					lineNo = 10;
				}
				imp.setC_Invoice_ID (invoice.getC_Invoice_ID());
				//	New InvoiceLine
				MInvoiceLine line = new MInvoiceLine (invoice);
				if (imp.getLineDescription() != null)
					line.setDescription(imp.getLineDescription());
				line.setLine(lineNo);
				lineNo += 10;
				if (imp.getM_Product_ID() != 0)
					line.setM_Product_ID(imp.getM_Product_ID(), true);
				// globalqss - import invoice with charges
				if (imp.getC_Charge_ID() != 0)
					line.setC_Charge_ID(imp.getC_Charge_ID());
				// globalqss - [2855673] - assign dimensions to lines also in case they're different 
				if (imp.getC_Activity_ID() != 0)
					line.setC_Activity_ID(imp.getC_Activity_ID());
				if (imp.getC_Campaign_ID() != 0)
					line.setC_Campaign_ID(imp.getC_Campaign_ID());
				if (imp.getC_Project_ID() != 0)
					line.setC_Project_ID(imp.getC_Project_ID());
				//
				line.setQty(imp.getQtyOrdered());
				line.setPrice();
				BigDecimal price = imp.getPriceActual();
				if (price != null && Env.ZERO.compareTo(price) != 0)
					line.setPrice(price);
				if (imp.getC_Tax_ID() != 0)
					line.setC_Tax_ID(imp.getC_Tax_ID());
				else
				{
					line.setTax();
					imp.setC_Tax_ID(line.getC_Tax_ID());
				}
				BigDecimal taxAmt = imp.getTaxAmt();
				if (taxAmt != null && Env.ZERO.compareTo(taxAmt) != 0)
					line.setTaxAmt(taxAmt);
				line.setC_1099Box_ID(imp.getC_1099Box_ID());
				//	Set Order and InOut
				if(imp.get_ValueAsInt("M_InOutLine_ID") > 0)
				{
					MInOutLine iol = new MInOutLine(getCtx(), imp.get_ValueAsInt("M_InOutLine_ID"), get_TrxName());
					imp.set_ValueOfColumn("C_OrderLine_ID", iol.getC_OrderLine_ID());
					imp.saveEx();
					line.setM_InOutLine_ID(iol.get_ID());
				}
				if(imp.get_ValueAsInt("C_OrderLine_ID") > 0)
					line.setC_OrderLine_ID(imp.get_ValueAsInt("C_OrderLine_ID"));
				
				line.saveEx();
				//
				imp.setC_InvoiceLine_ID(line.getC_InvoiceLine_ID());
				imp.setI_IsImported(true);
				imp.setProcessed(true);
				//
				if (imp.save())
					noInsertLine++;
			}
			if (invoice != null)
			{
				// Dont process if m_docAction Is Empty
				if (!m_docAction.equals("")) {
					if(!invoice.processIt (m_docAction)) {
						log.warning("Invoice Process Failed: " + invoice + " - " + invoice.getProcessMsg());
						throw new IllegalStateException("Invoice Process Failed: " + invoice + " - " + invoice.getProcessMsg());
						
					}
				}
				invoice.saveEx();
				commitEx();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "CreateInvoice", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Set Error to indicator to not imported
		sql = new StringBuilder ("UPDATE I_Invoice ")
			.append("SET I_IsImported='N', Updated=SysDate ")
			.append("WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog (0, null, new BigDecimal (no), "@Errors@");
		//
		addLog (0, null, new BigDecimal (noInsert), "@C_Invoice_ID@: @Inserted@");
		addLog (0, null, new BigDecimal (noInsertLine), "@C_InvoiceLine_ID@: @Inserted@");
		return "";
	}	//	doIt

}	//	ImportInvoice