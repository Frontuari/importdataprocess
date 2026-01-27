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
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.X_I_Order;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.compiere.util.Env;

import net.frontuari.base.CustomProcess;
import net.frontuari.custom.model.FTUMOrderLine;
import net.frontuari.custom.model.X_FTU_OLD;


/**
 *	Import Order from I_Order
 *  @author Oscar Gomez
 * 			<li>BF [ 2936629 ] Error when creating bpartner in the importation order
 * 			<li>https://sourceforge.net/tracker/?func=detail&aid=2936629&group_id=176962&atid=879332
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ImportOrder.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
@Process
public class ImportOrder extends CustomProcess
{
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Organization to be imported to		*/
	private int				m_AD_Org_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Document Action					*/
	private String			m_docAction = null;
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
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else if (name.equals("DocAction"))
				m_docAction = (String)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (m_DateValue == null)
			m_DateValue = new Timestamp (System.currentTimeMillis());
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message
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
			sql = new StringBuilder ("DELETE FROM I_Order ")
				  .append("WHERE I_IsImported='Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}
			
		//added by david castillo 11/03/2021 support to org from importer
		sql = new StringBuilder ("UPDATE I_Order o ")	//	org
				  .append("SET AD_OrgTrx_ID=(SELECT AD_Org_ID FROM AD_Org d WHERE d.Value=o.OrgValue")
				  .append(" AND o.AD_Client_ID=d.AD_Client_ID) ")
				  .append("WHERE AD_OrgTrx_ID IS NULL AND OrgValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		
		sql = new StringBuilder ("UPDATE I_Order o ")	//	org
				  .append("SET AD_Org_ID = o.AD_OrgTrx_ID")
				  .append(" WHERE AD_OrgTrx_ID IS NOT NULL AND OrgValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
				
		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET AD_Client_ID = COALESCE (AD_Client_ID,").append (m_AD_Client_ID).append ("),")
			  .append(" AD_Org_ID = COALESCE (AD_OrgTrx_ID,").append (m_AD_Org_ID).append ("),")
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
		
		sql = new StringBuilder ("UPDATE I_Order o ")
			.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '")
			.append("WHERE (AD_OrgTrx_ID IS NULL OR AD_OrgTrx_ID=0")
			.append(" OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_OrgTrx_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Org=" + no);

		//	Document Type - PO - SO
		sql = new StringBuilder ("UPDATE I_Order o ")	//	PO Document Type Name
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType='POO' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		sql = new StringBuilder ("UPDATE I_Order o ")	//	IPO Document Type Name
				  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
				  .append(" AND d.DocBaseType='IPO' AND o.AD_Client_ID=d.AD_Client_ID) ")
				  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PO DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")	//	SO Document Type Name
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType='SOO' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set SO DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType IN ('SOO','POO','IPO') AND o.AD_Client_ID=d.AD_Client_ID) ")
			//+ "WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Order ")	//	Error Invalid Doc Type Name
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid DocTypeName, ' ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid DocTypeName=" + no);
		//	DocType Default
		sql = new StringBuilder ("UPDATE I_Order o ")	//	Default PO
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType='POO' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PO Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")	//	Default SO
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType='SOO' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set SO Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType IN('SOO','POO') AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Order ")	// No DocType
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No DocType, ' ")
			  .append("WHERE C_DocType_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No DocType=" + no);

		//	Set IsSOTrx
		sql = new StringBuilder ("UPDATE I_Order o SET IsSOTrx='Y' ")
			  .append("WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='SOO' AND o.AD_Client_ID=d.AD_Client_ID)")
			  .append(" AND C_DocType_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set IsSOTrx=Y=" + no);
		sql = new StringBuilder ("UPDATE I_Order o SET IsSOTrx='N' ")
			  .append("WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='POO' AND o.AD_Client_ID=d.AD_Client_ID)")
			  .append(" AND C_DocType_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set IsSOTrx=N=" + no);
		//Added by David Castillo 10/03/2021 support to CurrencyIsoCode
		//	Set Currency
		sql = new StringBuilder ("UPDATE I_Order i ")
			.append("SET C_Currency_ID=(SELECT C_Currency_ID FROM C_Currency c")
			.append(" WHERE i.ISO_Code=c.ISO_Code AND c.AD_Client_ID IN (0,i.AD_Client_ID)) ")
			.append("WHERE C_Currency_ID IS NULL AND ISO_Code IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Set Currency=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order ")
			.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No Currency,' ")
			.append("WHERE C_Currency_ID IS NULL ")
			.append("AND I_IsImported<>'E' ")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No Currency=" + no);
		
		//	Price List
		//Added by david castillo 11/03/2021 support to pricelistname
		sql = new StringBuilder ("UPDATE I_Order o ")
					  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'")
					  .append("AND p.IsSOPriceList=o.IsSOTrx AND o.PriceListName = p.Name AND o.AD_Client_ID=p.AD_Client_ID) ")
					  .append("WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			
			//end david 	
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'")
			  .append(" AND p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default Currency PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'")
			  .append(" AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p ")
			  .append(" WHERE p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Currency PriceList=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p ")
			  .append(" WHERE p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PriceList=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No PriceList, ' ")
			  .append("WHERE M_PriceList_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No PriceList=" + no);

		// @Trifon - Import Order Source
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_OrderSource_ID=(SELECT C_OrderSource_ID FROM C_OrderSource p")
			  .append(" WHERE o.C_OrderSourceValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_OrderSource_ID IS NULL AND C_OrderSourceValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Order Source=" + no);
		// Set proper error message
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found Order Source, ' ")
			  .append("WHERE C_OrderSource_ID IS NULL AND C_OrderSourceValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No OrderSource=" + no);
		
		//	Payment Term
		sql = new StringBuilder("UPDATE I_Order o ")
				.append("SET C_PaymentTerm_ID = (SELECT BP.C_PaymentTerm_ID FROM C_BPartner BP WHERE BP.TaxID = o.BPTaxID) ")
				.append("WHERE C_PaymentTerm_ID IS NULL AND o.PaymentTermValue IS NULL AND o.I_IsImported <> 'Y'")
				.append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BPa	rtner PaymentTerm=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p")
			  .append(" WHERE o.PaymentTermValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_PaymentTerm_ID IS NULL AND PaymentTermValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PaymentTerm=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_PaymentTerm_ID=(SELECT MAX(C_PaymentTerm_ID) FROM C_PaymentTerm p")
			  .append(" WHERE p.IsDefault='Y' AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE C_PaymentTerm_ID IS NULL AND o.PaymentTermValue IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default PaymentTerm=" + no);
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No PaymentTerm, ' ")
			  .append("WHERE C_PaymentTerm_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No PaymentTerm=" + no);

		//	Warehouse
		//Added by David Castillo 10/03/2021 support to warehouse's value
		sql = new StringBuilder ("UPDATE I_Order o ")
				  .append("SET M_Warehouse_ID=(SELECT M_Warehouse_ID FROM M_Warehouse w")
				  .append(" WHERE o.WarehouseValue=w.value AND o.AD_Org_ID=w.AD_Org_ID) ")
				  .append("WHERE M_Warehouse_ID IS NULL AND WarehouseValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());	//	Warehouse for Org
		// end david castillo

		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_Warehouse_ID=(SELECT MAX(M_Warehouse_ID) FROM M_Warehouse w")
			  .append(" WHERE o.AD_Client_ID=w.AD_Client_ID AND o.AD_Org_ID=w.AD_Org_ID) ")
			  .append("WHERE M_Warehouse_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());	//	Warehouse for Org
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set Warehouse=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_Warehouse_ID=(SELECT M_Warehouse_ID FROM M_Warehouse w")
			  .append(" WHERE o.AD_Client_ID=w.AD_Client_ID) ")
			  .append("WHERE M_Warehouse_ID IS NULL")
			  .append(" AND EXISTS (SELECT AD_Client_ID FROM M_Warehouse w WHERE w.AD_Client_ID=o.AD_Client_ID GROUP BY AD_Client_ID HAVING COUNT(*)=1)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set Only Client Warehouse=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No Warehouse, ' ")
			  .append("WHERE M_Warehouse_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No Warehouse=" + no);

		//	BP from EMail
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u")
			  .append(" WHERE o.EMail=u.EMail AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) ")
			  .append("WHERE C_BPartner_ID IS NULL AND EMail IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from EMail=" + no);
		//	BP from ContactName
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u")
			  .append(" WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) ")
			  .append("WHERE C_BPartner_ID IS NULL AND ContactName IS NOT NULL")
			  .append(" AND EXISTS (SELECT Name FROM AD_User u WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL GROUP BY Name HAVING COUNT(*)=1)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from ContactName=" + no);
		//	BP from Value
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE o.BPartnerValue=bp.Value AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from Value=" + no);
		//	BP from TaxID
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE o.BPTaxID=bp.TaxID AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPTaxID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from TaxID=" + no);
		//	Default BP
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_BPartner_ID=(SELECT C_BPartnerCashTrx_ID FROM AD_ClientInfo c")
			  .append(" WHERE o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NULL AND Name IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default BP=" + no);

		//	Existing Location ? Exact Match
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET (BillTo_ID,C_BPartner_Location_ID)=(SELECT C_BPartner_Location_ID,C_BPartner_Location_ID")
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
		//	Set Bill Location from BPartner
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET BillTo_ID=(SELECT MAX(C_BPartner_Location_ID) FROM C_BPartner_Location l")
			  .append(" WHERE l.C_BPartner_ID=o.C_BPartner_ID AND o.AD_Client_ID=l.AD_Client_ID")
			  .append(" AND ((l.IsBillTo='Y' AND o.IsSOTrx='Y') OR (l.IsPayFrom='Y' AND o.IsSOTrx='N'))")
			  .append(") ")
			  .append("WHERE C_BPartner_ID IS NOT NULL AND BillTo_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP BillTo from BP=" + no);
		//	Set Location from BPartner
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_BPartner_Location_ID=(SELECT MAX(C_BPartner_Location_ID) FROM C_BPartner_Location l")
			  .append(" WHERE l.C_BPartner_ID=o.C_BPartner_ID AND o.AD_Client_ID=l.AD_Client_ID")
			  .append(" AND ((l.IsShipTo='Y' AND o.IsSOTrx='Y') OR o.IsSOTrx='N')")
			  .append(") ")
			  .append("WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP Location from BP=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No BP Location, ' ")
			  .append("WHERE C_BPartner_ID IS NOT NULL AND (BillTo_ID IS NULL OR C_BPartner_Location_ID IS NULL)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No BP Location=" + no);

		//	Set Country
		/**
		sql = new StringBuffer ("UPDATE I_Order o "
			  + "SET CountryCode=(SELECT MAX(CountryCode) FROM C_Country c WHERE c.IsDefault='Y'"
			  + " AND c.AD_Client_ID IN (0, o.AD_Client_ID)) "
			  + "WHERE C_BPartner_ID IS NULL AND CountryCode IS NULL AND C_Country_ID IS NULL"
			  + " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Country Default=" + no);
		**/
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_Country_ID=(SELECT C_Country_ID FROM C_Country c")
			  .append(" WHERE o.CountryCode=c.CountryCode AND c.AD_Client_ID IN (0, o.AD_Client_ID)) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL AND CountryCode IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Country=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Country, ' ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Country=" + no);

		//	Set Region
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("Set RegionName=(SELECT MAX(Name) FROM C_Region r")
			  .append(" WHERE r.IsDefault='Y' AND r.C_Country_ID=o.C_Country_ID")
			  .append(" AND r.AD_Client_ID IN (0, o.AD_Client_ID)) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Region Default=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("Set C_Region_ID=(SELECT C_Region_ID FROM C_Region r")
			  .append(" WHERE r.Name=o.RegionName AND r.C_Country_ID=o.C_Country_ID")
			  .append(" AND r.AD_Client_ID IN (0, o.AD_Client_ID)) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Region=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Region, ' ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL ")
			  .append(" AND EXISTS (SELECT * FROM C_Country c")
			  .append(" WHERE c.C_Country_ID=o.C_Country_ID AND c.HasRegion='Y')")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Region=" + no);

		//	Product
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.ProductValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from Value=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.UPC=p.UPC AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND UPC IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from UPC=" + no);
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.SKU=p.SKU AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND SKU IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product fom SKU=" + no);
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' ")
			  .append("WHERE M_Product_ID IS NULL AND (ProductValue IS NOT NULL OR UPC IS NOT NULL OR SKU IS NOT NULL)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product=" + no);

		sql = new StringBuilder ("UPDATE I_Order o ")
				.append("SET UserLine1_ID=(SELECT ev.c_elementvalue_id FROM AD_Tree tree ")
				.append("join C_Element e on tree.AD_Tree_ID = e.AD_Tree_ID ")
				.append("join C_ElementValue ev on ev.c_element_id = e.c_element_id ")
				.append(" WHERE o.UserLine1Value=ev.value AND o.AD_Client_ID=tree.AD_Client_ID AND tree.TreeType = 'U1' AND e.ElementType = 'U') ")
				.append("WHERE UserLine1_ID IS NULL AND UserLine1Value IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set UserLine1_ID from UserLine1Value=" + no);
		
		sql = new StringBuilder ("UPDATE I_Order o ")
				.append("SET Account_ID=(SELECT ev.c_elementvalue_id FROM AD_Tree tree ")
				.append("join C_Element e on tree.AD_Tree_ID = e.AD_Tree_ID ")
				.append("join C_ElementValue ev on ev.c_element_id = e.c_element_id ")
				.append(" WHERE o.AccountValue=ev.value AND o.AD_Client_ID=tree.AD_Client_ID AND tree.TreeType = 'EV' AND e.ElementType = 'A') ")
				.append("WHERE Account_ID IS NULL AND AccountValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Account_ID from AccountValue=" + no);
		
		//		Set Activity
			sql = new StringBuilder ("UPDATE I_Order o ")	//	Activity
				  .append("SET C_Activity_ID=(SELECT MAX(C_Activity_ID) FROM C_Activity a WHERE a.Value=o.ActivityValue OR a.Name=o.ActivityName")
				  .append(" AND o.AD_Client_ID=a.AD_Client_ID) ")
				  .append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL OR ActivityName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Activity=" + no);
			sql = new StringBuilder ("UPDATE I_Order ")	//	Error Invalid Activity
				  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, ' ")
				  .append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				log.warning ("Invalid Activity=" + no);
		
			
			//		Set ActivityDistributionLine
			sql = new StringBuilder ("UPDATE I_Order o ")	//	ActivityDistributionLineValue
					.append("SET C_ActivityDistributionLine_ID=(SELECT MAX(C_Activity_ID) FROM C_Activity a WHERE a.Value=o.ActivityDistributionLineValue")
					.append(" AND o.AD_Client_ID=a.AD_Client_ID) ")
					.append("WHERE C_ActivityDistributionLine_ID IS NULL AND ActivityDistributionLineValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set Activity=" + no);
			sql = new StringBuilder ("UPDATE I_Order ")	//	Error Invalid Activity
					.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity Distribution Line, ' ")
					.append("WHERE C_ActivityDistributionLine_ID IS NULL AND ActivityDistributionLineValue IS NOT NULL")
					.append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				log.warning ("Invalid Activity Distribution Line=" + no);
		
			
		//	Charge
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_Charge_ID=(SELECT C_Charge_ID FROM C_Charge c")
			  .append(" WHERE o.ChargeName=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE C_Charge_ID IS NULL AND ChargeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Charge=" + no);
		sql = new StringBuilder ("UPDATE I_Order ")
				  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Charge, ' ")
				  .append("WHERE C_Charge_ID IS NULL AND (ChargeName IS NOT NULL)")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Charge=" + no);
		//
		
		sql = new StringBuilder ("UPDATE I_Order ")
				  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Product and Charge, ' ")
				  .append("WHERE M_Product_ID IS NOT NULL AND C_Charge_ID IS NOT NULL ")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product and Charge exclusive=" + no);

		//	Tax
		//Added by David Castillo 10/03/2021 support to tax's name
		sql = new StringBuilder ("UPDATE I_Order o ")
				  .append("SET C_Tax_ID=(SELECT C_Tax_ID FROM C_Tax t")
				  .append(" WHERE o.TaxName = t.Name AND o.AD_Client_ID=t.AD_Client_ID) ")
				  .append("WHERE C_Tax_ID IS NULL AND TaxName IS NOT NULL")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		
		sql = new StringBuilder ("UPDATE I_Order o ")
			  .append("SET C_Tax_ID=(SELECT MAX(C_Tax_ID) FROM C_Tax t")
			  .append(" WHERE o.TaxIndicator=t.TaxIndicator AND o.AD_Client_ID=t.AD_Client_ID) ")
			  .append("WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Tax=" + no);
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Tax, ' ")
			  .append("WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Tax=" + no);
		
		//	David Castillo 17/10/2020 added saving of C_Activity, User1, UOM.
		//UoM
		sql = new StringBuilder ("UPDATE I_Order o ")
				  .append("SET C_UoM_ID=(SELECT C_UoM_ID FROM C_UoM c")
				  .append(" WHERE o.UoMName=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
				  .append("WHERE C_UoM_ID IS NULL AND UoMName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set UoM=" + no);
		// Set proper error message
		sql = new StringBuilder ("UPDATE I_ORDER i ")
				.append("SET C_UOM_ID = (SELECT MAX(C_UOM_ID) FROM C_UOM u WHERE u.X12DE355=i.X12DE355 AND u.AD_Client_ID IN (0,i.AD_Client_ID))")
				.append("WHERE C_UOM_ID IS NULL AND X12DE355 IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.INFO)) log.info("Set UOM=" + no);
			//
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found UOM, ' ")
			  .append("WHERE C_UoM_ID IS NULL AND (UoMName IS NOT NULL OR X12DE355 IS NOT NULL) AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No UoM=" + no);
		
		// It is filtered using TaxID as the search criteria, ensuring that the SalesRep_Value of I_Order matches the TaxID of C_BPartner.
		//Added by Joaquin Mora 25/03/2025
		sql = new StringBuilder("UPDATE I_Order o ")
			      .append("SET SalesRep_ID=(SELECT MAX(u.AD_User_ID) FROM AD_User u ")
			      .append(" JOIN C_BPartner cb ON u.C_BPartner_ID = cb.C_BPartner_ID ")
			      .append(" WHERE o.SalesRep_Value = cb.TaxID ")
			      .append(" AND o.AD_Client_ID = cb.AD_Client_ID ")
			      .append(" AND cb.IsSalesRep = 'Y' AND cb.IsActive = 'Y')")
			      .append(" WHERE SalesRep_ID IS NULL AND SalesRep_Value IS NOT NULL")
			      .append(" AND I_IsImported<>'Y'").append(clientCheck);

			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set SalesRep from Value=" + no);
		

		//	instancia atribute
		//Added by Jose Vasquez 27/05/2024
		sql = new StringBuilder ("UPDATE I_Order o ")
				  .append("SET M_AttributesetInstance_ID=(SELECT M_AttributesetInstance_ID FROM M_AttributesetInstance m")
				  .append(" WHERE o.AttributesetInstance = m.M_AttributesetInstance_ID) ")
				  .append("WHERE M_AttributesetInstance_ID IS NULL AND AttributesetInstance IS NOT NULL")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Attributeset Instance=" + no);
		
		//	instancia atribute
		//Added by Jose Vasquez 27/05/2024
		sql = new StringBuilder ("UPDATE I_Order o ")
				.append("SET M_AttributesetInstance_ID=(SELECT M_AttributesetInstance_ID FROM M_AttributesetInstance m")
				.append(" WHERE o.AttributesetInstance = m.M_AttributesetInstance_ID) ")
				.append("WHERE M_AttributesetInstance_ID IS NULL AND AttributesetInstance IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Attributeset Instance=" + no);
		
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Attributeset Instance, ' ")
			  .append("WHERE M_AttributesetInstance_ID IS NULL AND AttributesetInstance IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Attributeset Instance =" + no);

		commitEx();
		
		if (p_IsValidateOnly)
		{
			return "Validated";
		}
		
		//	-- New BPartner ---------------------------------------------------
		//	Go through Order Records w/o C_BPartner_ID
		sql = new StringBuilder ("SELECT * FROM I_Order ")
			  .append("WHERE I_IsImported='N' AND C_BPartner_ID IS NULL").append (clientCheck);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				X_I_Order imp = new X_I_Order (getCtx (), rs, get_TrxName());
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
					bpl.setC_Location_ID (loc.getC_Location_ID ());
					if (!bpl.save ())
						continue;
				}
				imp.setC_Location_ID (bpl.getC_Location_ID ());
				imp.setBillTo_ID (bpl.getC_BPartner_Location_ID ());
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
			log.log(Level.SEVERE, "BP - " + sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		sql = new StringBuilder ("UPDATE I_Order ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No BPartner, ' ")
			  .append("WHERE C_BPartner_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No BPartner=" + no);

		commitEx();
		
		//	-- New Orders -----------------------------------------------------


		int noInsert = 0;
		int noInsertLine = 0;
		Integer maxLinesByDocument = MSysConfig.getIntValue("MAXLINESBYORDER", 0, getAD_Client_ID());

		if (maxLinesByDocument <= 0) {
		    maxLinesByDocument = Integer.MAX_VALUE;
		}

		sql = new StringBuilder("SELECT * FROM I_Order ")
		    .append("WHERE I_IsImported='N'").append(clientCheck)
		    .append(" ORDER BY AD_Org_ID,C_BPartner_ID,C_DocType_ID,DocumentNo,C_Currency_ID, BillTo_ID, C_BPartner_Location_ID, POReference, I_Order_ID");

		try {
		    pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
		    rs = pstmt.executeQuery();

		    int oldC_BPartner_ID = 0;
		    int oldBillTo_ID = 0;
		    int oldC_BPartner_Location_ID = 0;
		    int oldCurrency_ID = 0;
		    int oldSalesrep_ID = 0;
		    String oldDocumentNo = "";
		    String oldPOReference = "";

		    MOrder order = null;
		    int lineNo = 0;
		    int currentLineCount = 0;
		    
		    Map<String, FTUMOrderLine> lineMap = new HashMap<>();

		    while (rs.next()) {
		        X_I_Order imp = new X_I_Order(getCtx(), rs, get_TrxName());
				boolean forceNewOrder = (currentLineCount >= maxLinesByDocument);	
		        String cmpDocumentNo = imp.getDocumentNo();
		        if (cmpDocumentNo == null) cmpDocumentNo = "";

		        String cmpPOReference = imp.get_ValueAsString("POReference");
		        if (cmpPOReference == null) cmpPOReference = "";

		        if (order == null || oldC_BPartner_ID != imp.getC_BPartner_ID() 
		            || oldC_BPartner_Location_ID != imp.getC_BPartner_Location_ID()
		            || oldBillTo_ID != imp.getBillTo_ID()
		            || oldSalesrep_ID != imp.getSalesRep_ID()
		            || oldCurrency_ID != imp.getC_Currency_ID()
		            || !oldDocumentNo.equals(cmpDocumentNo) 
		            || !oldPOReference.equals(cmpPOReference)
		            || forceNewOrder) {

		            if (order != null) {
		                if (m_docAction != null && m_docAction.length() > 0) {
		                    order.setDocAction(m_docAction);
		                    if (!order.processIt(m_docAction)) {
		                        log.warning("Order Process Failed: " + order + " - " + order.getProcessMsg());
		                        throw new IllegalStateException("Order Process Failed: " + order + " - " + order.getProcessMsg());
		                    }
		                }
		                order.saveEx();
		            }

		            oldC_BPartner_ID = imp.getC_BPartner_ID();
		            oldC_BPartner_Location_ID = imp.getC_BPartner_Location_ID();
		            oldBillTo_ID = imp.getBillTo_ID();
		            oldCurrency_ID = imp.getC_Currency_ID();
		            oldSalesrep_ID = imp.getSalesRep_ID(); 
		            oldDocumentNo = imp.getDocumentNo();
		            oldPOReference = imp.get_ValueAsString("POReference");
		            if (oldDocumentNo == null) oldDocumentNo = "";
		            if (oldPOReference == null) oldPOReference = "";

					lineMap.clear();
		            order = new MOrder(getCtx(), 0, get_TrxName());
		            order.setClientOrg(imp.getAD_Client_ID(), imp.getAD_OrgTrx_ID());
		            order.setC_DocTypeTarget_ID(imp.getC_DocType_ID());
		            order.setIsSOTrx(imp.isSOTrx());
		            if (imp.getDeliveryRule() != null) {
		                order.setDeliveryRule(imp.getDeliveryRule());
		            }
		            order.setDocumentNo(imp.getDocumentNo());
		            order.setPOReference(imp.get_ValueAsString("POReference"));
		            order.setC_BPartner_ID(imp.getC_BPartner_ID());
		            order.setC_BPartner_Location_ID(imp.getC_BPartner_Location_ID());
		            if (imp.getAD_User_ID() != 0) order.setAD_User_ID(imp.getAD_User_ID());
		            order.setBill_BPartner_ID(imp.getC_BPartner_ID());
		            order.setBill_Location_ID(imp.getBillTo_ID());
		            order.setDescription(imp.getDescription());
		            order.setC_PaymentTerm_ID(imp.getC_PaymentTerm_ID());
		            order.setM_PriceList_ID(imp.getM_PriceList_ID());
		            order.setM_Warehouse_ID(imp.getM_Warehouse_ID());
		            if (imp.getM_Shipper_ID() != 0) order.setM_Shipper_ID(imp.getM_Shipper_ID());
		            if (imp.getSalesRep_ID() != 0) order.setSalesRep_ID(imp.getSalesRep_ID());
		            if (order.getSalesRep_ID() == 0) order.setSalesRep_ID(getAD_User_ID());
		            order.setDateOrdered(imp.getDateOrdered());
		            order.setDateAcct(imp.getDateAcct());
					int user1Id = imp.get_ValueAsInt("User1_ID"); 
					if (user1Id > 0) {
						order.setUser1_ID(user1Id);
					}
		            if (imp.get_Value("C_ConversionType_ID") != null) {
		                order.setC_ConversionType_ID(imp.get_ValueAsInt("C_ConversionType_ID"));
		            }
		            if (order.isSOTrx()) {
		                MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), imp.getC_BPartner_Location_ID(), get_TrxName());
		                if (bpl.get_Value("FTU_DeliveryRute_ID") != null) {
		                    int deliveryRouteID = bpl.get_ValueAsInt("FTU_DeliveryRute_ID");
		                    order.set_ValueOfColumn("FTU_DeliveryRute_ID", deliveryRouteID);
		                }
		            }
		            order.saveEx();

		            noInsert++;
		            lineNo = 10;
		            currentLineCount = 0;
		        }

		        imp.setC_Order_ID(order.getC_Order_ID());
		        
		        BigDecimal priceList = BigDecimal.ZERO;
		        if (imp.get_Value("PriceList") != null) {
		            priceList = new BigDecimal(imp.get_Value("PriceList").toString());
		        }

		        BigDecimal discountObj = BigDecimal.ZERO;
		        if (order.isSOTrx()) {
		            MBPartner bpartner = new MBPartner(getCtx(), imp.getC_BPartner_ID(), get_TrxName());
		            if (bpartner.get_Value("FlatDiscount") != null)
		                discountObj = new BigDecimal(bpartner.get_Value("FlatDiscount").toString());
		            imp.set_ValueOfColumn("Discount", discountObj);
		        }

		        BigDecimal addDiscount = BigDecimal.ZERO;
		        if (order.isSOTrx() && imp.get_Value("Add_Discount") != null) {
		            addDiscount = new BigDecimal(imp.get_Value("Add_Discount").toString());
		            imp.set_ValueOfColumn("Add_Discount", addDiscount);
		        }

		        // Calculamos el precio final aplicando descuentos
		        BigDecimal finalPrice = priceList
		                .subtract(priceList.multiply(discountObj).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP))
		                .subtract(priceList.multiply(addDiscount).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));

		        String lineKey = imp.getDocumentNo()+ "-" + imp.getDateOrdered() + "-" + imp.getC_BPartner_ID() + "-" + imp.getM_Product_ID() + imp.getC_Charge_ID() + "-" + imp.getDescription() + "-" + imp.getQtyOrdered()
		                + "-" + imp.getC_UOM_ID() + "-" + finalPrice;

		        FTUMOrderLine line;
		        if (!lineMap.containsKey(lineKey)) {
		            line = new FTUMOrderLine(order);
		            line.setLine(lineNo);
		            lineNo += 10;

		            if (imp.getM_Product_ID() != 0) {
		                line.setM_Product_ID(imp.getM_Product_ID());
		                line.setC_UOM_ID(imp.getC_UOM_ID());
		                BigDecimal pkgUnit = BigDecimal.ZERO;
		                MProduct product = new MProduct(getCtx(), line.getM_Product_ID(), get_TrxName());
			            Object pkgUnitObj = product.get_Value("PkgUnit");
			            if (pkgUnitObj != null) {
			                pkgUnit = new BigDecimal(pkgUnitObj.toString());
			                line.set_ValueOfColumn("PkgUnit", pkgUnit);
							imp.set_ValueOfColumn("PkgUnit", pkgUnit);
			            }
		            }

		            if (imp.getC_Charge_ID() != 0) {
		                line.setC_Charge_ID(imp.getC_Charge_ID());
		            }
					
					int lineUser1Id = imp.get_ValueAsInt("User1_ID");
					if (lineUser1Id > 0) {
						line.setUser1_ID(lineUser1Id);
					}
		            line.setQty(imp.getQtyOrdered());
		            line.setPriceList(priceList);
		            line.setPriceEntered(finalPrice);
		            line.setPriceActual(finalPrice);

		            line.set_ValueOfColumn("Discount", discountObj);
		            line.set_ValueOfColumn("IsManualDiscount", imp.get_Value("IsManualDiscount"));
		            line.set_ValueOfColumn("Add_Discount", addDiscount);

		            if (imp.getC_Tax_ID() != 0) {
		                line.setC_Tax_ID(imp.getC_Tax_ID());
		            } else {
		                line.setTax();
		                imp.setC_Tax_ID(line.getC_Tax_ID());
		            }

		            line.saveEx();
		            lineMap.put(lineKey, line);
		            noInsertLine++;
					currentLineCount++;
		        } else {
		            line = lineMap.get(lineKey);
		        }

		        Boolean isExpenseDistributiveFromImp = imp.get_ValueAsBoolean("IsExpenseDistributive");
		        
				// Crear X_FTU_OLD si aplica
				if (!imp.isSOTrx() && isExpenseDistributiveFromImp) {

					line.set_ValueOfColumn("IsExpenseDistributive", isExpenseDistributiveFromImp);
					line.set_ValueOfColumn("DistributionMethod", imp.get_Value("DistributionMethod"));
					
					line.saveEx();

					X_FTU_OLD rld = new X_FTU_OLD(getCtx(), 0, get_TrxName());
					rld.setC_OrderLine_ID(line.getC_OrderLine_ID());
					rld.setAD_Org_ID(imp.getAD_OrgTrx_ID());

					
					if (imp.get_ValueAsInt("UserLine1_ID") > 0)
						rld.setUserLine1_ID(imp.get_ValueAsInt("UserLine1_ID"));

					if (imp.get_Value("Account_ID") != null)
						rld.setAccount_ID(imp.get_ValueAsInt("Account_ID"));

					if (imp.get_Value("Amount") != null)
						rld.setAmount((BigDecimal) imp.get_Value("Amount"));

					if (imp.get_Value("Description_Line") != null)
						rld.setDescription_Line(imp.get_ValueAsString("Description_Line"));
					
					if (imp.get_Value("C_ActivityDistributionLine_ID") != null)
						rld.set_ValueOfColumn("C_Activity_ID", imp.get_ValueAsInt("C_ActivityDistributionLine_ID"));

					rld.saveEx();

				}

		        // Marcar imp como procesado
		        imp.setC_OrderLine_ID(line.getC_OrderLine_ID());
		        imp.setI_IsImported(true);
		        imp.setProcessed(true);
		        imp.setI_ErrorMsg(null);
		        imp.saveEx();
		    }

		    if (order != null) {
		        if (m_docAction != null && m_docAction.length() > 0) {
		            order.setDocAction(m_docAction);
		            if (!order.processIt(m_docAction)) {
		                log.warning("Order Process Failed: " + order + " - " + order.getProcessMsg());
		                throw new IllegalStateException("Order Process Failed: " + order + " - " + order.getProcessMsg());
		            }
		        }
		        order.saveEx();
		    }
		} finally {
		    DB.close(rs, pstmt);
		    rs = null;
		    pstmt = null;
		}

		sql = new StringBuilder("UPDATE I_Order ")
		    .append("SET I_IsImported='N', Updated=SysDate ")
		    .append("WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog(0, null, new BigDecimal(no), "@Errors@");
		addLog(0, null, new BigDecimal(noInsert), "@C_Order_ID@: @Inserted@");
		addLog(0, null, new BigDecimal(noInsertLine), "@C_OrderLine_ID@: @Inserted@");
		StringBuilder msgreturn = new StringBuilder("#").append(noInsert).append("/").append(noInsertLine);
		return msgreturn.toString();
 // doIt
	}
}	//	ImportOrder