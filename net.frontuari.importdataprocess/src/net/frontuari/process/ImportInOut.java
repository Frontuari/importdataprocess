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
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;

import net.frontuari.base.CustomProcess;

import net.frontuari.model.X_I_InOut;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;

/**
 *	Import Receipt/Delivery from I_InOut
 *  @author 	Jorge Colmenarez
 * 	@version 	$Id: ImportInOut.java,v 1.0 2021/05/04 18:26 jlctmaster Frontuari,C.A. $
 */
@Process
public class ImportInOut extends CustomProcess {
	
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;
	/**	Document Action		*/
	private String			p_DocAction = MInOut.ACTION_Prepare;

	public ImportInOut() {
	}

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Client_ID"))
				m_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("DocAction"))
				p_DocAction = para[i].getParameterAsString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}
	
	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	@Override
	protected String doIt() throws Exception {
		StringBuilder sql = null;
		int no = 0;
		StringBuilder clientCheck = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE FROM I_InOut ")
				  .append("WHERE I_IsImported='Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}
			
		//	Set AD_OrgTrx
		sql = new StringBuilder ("UPDATE I_InOut o ")	//	org
				  .append("SET AD_OrgTrx_ID=(SELECT AD_Org_ID FROM AD_Org d WHERE d.Value=o.OrgValue")
				  .append(" AND o.AD_Client_ID=d.AD_Client_ID) ")
				  .append("WHERE AD_OrgTrx_ID IS NULL AND OrgValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		//	Set AD_Org
		sql = new StringBuilder ("UPDATE I_InOut o ")	//	org
				  .append("SET AD_Org_ID = o.AD_OrgTrx_ID")
				  .append(" WHERE AD_OrgTrx_ID IS NOT NULL AND OrgValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
				
		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_InOut ")
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
				
		sql = new StringBuilder ("UPDATE I_InOut o ")
			.append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '")
			.append("WHERE (AD_OrgTrx_ID IS NULL OR AD_OrgTrx_ID=0")
			.append(" OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_OrgTrx_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Org=" + no);

		//	Document Type - PO - SO
		sql = new StringBuilder ("UPDATE I_InOut o ")	//	PO Document Type Name
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType='MMR' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PO DocType=" + no);
		sql = new StringBuilder ("UPDATE I_InOut o ")	//	SO Document Type Name
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType='MMS' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set SO DocType=" + no);
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName")
			  .append(" AND d.DocBaseType IN ('MMS','MMR') AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set DocType=" + no);
		sql = new StringBuilder ("UPDATE I_InOut ")	//	Error Invalid Doc Type Name
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid DocTypeName, ' ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid DocTypeName=" + no);
		//	DocType Default
		sql = new StringBuilder ("UPDATE I_InOut o ")	//	Default PO
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType='MMR' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set PO Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_InOut o ")	//	Default SO
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType='MMS' AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set SO Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'")
			  .append(" AND d.DocBaseType IN('MMS','MMR') AND o.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default DocType=" + no);
		sql = new StringBuilder ("UPDATE I_InOut ")	// No DocType
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No DocType, ' ")
			  .append("WHERE C_DocType_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No DocType=" + no);

		//	Set IsSOTrx
		sql = new StringBuilder ("UPDATE I_InOut o SET IsSOTrx='Y' ")
			  .append("WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='MMS' AND o.AD_Client_ID=d.AD_Client_ID)")
			  .append(" AND C_DocType_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set IsSOTrx=Y=" + no);
		sql = new StringBuilder ("UPDATE I_InOut o SET IsSOTrx='N' ")
			  .append("WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='MMR' AND o.AD_Client_ID=d.AD_Client_ID)")
			  .append(" AND C_DocType_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set IsSOTrx=N=" + no);
		
		//	Warehouse
		sql = new StringBuilder ("UPDATE I_InOut o ")
				  .append("SET M_Warehouse_ID=(SELECT M_Warehouse_ID FROM M_Warehouse w")
				  .append(" WHERE o.WarehouseValue=w.value AND COALESCE(o.AD_OrgTrx_ID,o.AD_Org_ID)=w.AD_Org_ID) ")
				  .append("WHERE M_Warehouse_ID IS NULL AND WarehouseValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());	//	Warehouse for Org
		//
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET M_Warehouse_ID=(SELECT MAX(M_Warehouse_ID) FROM M_Warehouse w")
			  .append(" WHERE o.AD_Client_ID=w.AD_Client_ID AND COALESCE(o.AD_OrgTrx_ID,o.AD_Org_ID)=w.AD_Org_ID) ")
			  .append("WHERE M_Warehouse_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());	//	Warehouse for Org
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set Warehouse=" + no);
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET M_Warehouse_ID=(SELECT M_Warehouse_ID FROM M_Warehouse w")
			  .append(" WHERE o.AD_Client_ID=w.AD_Client_ID) ")
			  .append("WHERE M_Warehouse_ID IS NULL")
			  .append(" AND EXISTS (SELECT AD_Client_ID FROM M_Warehouse w WHERE w.AD_Client_ID=o.AD_Client_ID GROUP BY AD_Client_ID HAVING COUNT(*)=1)")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set Only Client Warehouse=" + no);
		//
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No Warehouse, ' ")
			  .append("WHERE M_Warehouse_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No Warehouse=" + no);

		//	BP from Order
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_Order od")
			  .append(" WHERE o.C_Order_ID=od.C_Order_ID AND o.AD_Client_ID=od.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Order_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from Order=" + no);
		//	BP from Value
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE o.BPartnerValue=bp.Value AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from Value=" + no);
		//	BP from TaxID
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE o.BPTaxID=bp.TaxID AND o.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPTaxID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from TaxID=" + no);
		//	Default BP
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_BPartner_ID=(SELECT C_BPartnerCashTrx_ID FROM AD_ClientInfo c")
			  .append(" WHERE o.AD_Client_ID=c.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Default BP=" + no);

		//	Existing Location ? Exact Match
		//	BP from Order
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_BPartner_Location_ID=(SELECT MAX(C_BPartner_Location_ID) FROM C_Order od")
			  .append(" WHERE o.C_Order_ID=od.C_Order_ID AND o.AD_Client_ID=od.AD_Client_ID) ")
			  .append("WHERE C_BPartner_Location_ID IS NULL AND C_Order_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP from Order=" + no);
		//	Set Location from BPartner
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_BPartner_Location_ID=(SELECT MAX(C_BPartner_Location_ID) FROM C_BPartner_Location l")
			  .append(" WHERE l.C_BPartner_ID=o.C_BPartner_ID AND o.AD_Client_ID=l.AD_Client_ID")
			  .append(" AND ((l.IsShipTo='Y' AND o.IsSOTrx='Y') OR o.IsSOTrx='N')")
			  .append(") ")
			  .append("WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BP Location from BP=" + no);
		//
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No BP Location, ' ")
			  .append("WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No BP Location=" + no);
		
		//	Purchase/Sales Order
		sql = new StringBuilder ("UPDATE I_InOut i ")
			  .append("SET C_Order_ID=(SELECT MAX(C_Order_ID) FROM C_Order o")
			  .append(" WHERE i.OrderDocumentNo=o.DocumentNo AND i.AD_Client_ID=o.AD_Client_ID AND i.AD_Org_ID = o.AD_Org_ID ")
			  .append(" AND o.C_BPartner_ID = i.C_BPartner_ID) ")
			  .append("WHERE C_Order_ID IS NULL AND OrderDocumentNo IS NOT NULL AND C_BPartner_ID IS NOT NULL ")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Purchase/Sales Order=" + no);	
		
		sql = new StringBuilder ("UPDATE I_InOut ")	// No DocType
				  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=No Purchase/Sales Order, ' ")
				  .append("WHERE C_Order_ID IS NULL AND OrderDocumentNo IS NOT NULL AND C_BPartner_ID IS NOT NULL ")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No Purchase/Sales Order=" + no);

		//	Activity
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET C_Activity_ID=(SELECT MAX(C_Activity_ID) FROM C_Activity a")
			  .append(" WHERE o.ActivityName=a.Name AND o.AD_Client_ID=a.AD_Client_ID) ")
			  .append("WHERE C_Activity_ID IS NULL AND ActivityName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Activity from Name=" + no);
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, ' ")
			  .append("WHERE C_Activity_ID IS NULL AND ActivityName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Activity=" + no);
		
		//	User1
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET User1_ID=(SELECT MAX(C_ElementValue_ID) FROM C_ElementValue ev ")
			  .append(" JOIN C_Element e ON ev.C_Element_ID = e.C_Element_ID AND e.ElementType='U' ")
			  .append(" WHERE o.User1Name=ev.Name AND o.AD_Client_ID=ev.AD_Client_ID) ")
			  .append("WHERE User1_ID IS NULL AND User1Name IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set User1 from Name=" + no);
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid User1, ' ")
			  .append("WHERE User1_ID IS NULL AND User1Name IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid User1=" + no);
		
		//	Product
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p")
			  .append(" WHERE o.ProductValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) ")
			  .append("WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Product from Value=" + no);
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' ")
			  .append("WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Product=" + no);
		
		//UOM
		sql = new StringBuilder ("UPDATE I_InOut i ")
				.append("SET C_UOM_ID = (SELECT C_UOM_ID FROM C_UOM u WHERE u.X12DE355=i.X12DE355 AND u.AD_Client_ID IN (0,i.AD_Client_ID))")
				.append("WHERE C_UOM_ID IS NULL AND X12DE355 IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("Set UOM=" + no);
		//
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found UOM, ' ")
			  .append("WHERE C_UoM_ID IS NULL AND (X12DE355 IS NOT NULL) AND I_IsImported<>'Y'").append (clientCheck);
		//UOM from Product
		sql = new StringBuilder ("UPDATE I_InOut i ")
				.append("SET C_UOM_ID = (SELECT C_UOM_ID FROM M_Product p WHERE p.M_Product_ID=i.M_Product_ID AND p.AD_Client_ID IN (0,i.AD_Client_ID))")
				.append("WHERE C_UOM_ID IS NULL AND M_Product_ID IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("Set UOM from Product=" + no);		
		//
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found UOM from Product, ' ")
			  .append("WHERE C_UoM_ID IS NULL AND (M_Product_ID IS NOT NULL) AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No UoM=" + no);
		
		//	Order Line from Order and Product
		sql = new StringBuilder ("UPDATE I_InOut i ")
			  .append("SET C_OrderLine_ID=(SELECT MAX(C_OrderLine_ID) FROM C_OrderLine ol")
			  .append(" WHERE i.C_Order_ID=ol.C_Order_ID AND i.AD_Client_ID=ol.AD_Client_ID  ")
			  .append(" AND i.M_Product_ID=ol.M_Product_ID AND i.C_BPartner_ID = ol.C_BPartner_ID AND ol.QtyOrdered = ABS(i.QtyEntered)) ")
			  .append("WHERE C_OrderLine_ID IS NULL AND C_Order_ID IS NOT NULL AND M_Product_ID IS NOT NULL AND C_BPartner_ID IS NOT NULL AND QtyEntered IS NOT NULL ")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set OrderLine=" + no);
		
		//	Locator
		sql = new StringBuilder ("UPDATE I_InOut o ")
			  .append("SET M_Locator_ID=(SELECT MAX(M_Locator_ID) FROM M_Locator l")
			  .append(" WHERE o.LocatorValue=l.Value AND o.AD_Client_ID=l.AD_Client_ID AND o.M_Warehouse_ID = l.M_Warehouse_ID) ")
			  .append("WHERE M_Locator_ID IS NULL AND M_Warehouse_ID IS NOT NULL AND LocatorValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Locator from Value and Warehouse=" + no);
		sql = new StringBuilder ("UPDATE I_InOut ")
			  .append("SET I_IsImported='N', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Locator, ' ")
			  .append("WHERE M_Locator_ID IS NULL AND M_Warehouse_ID IS NOT NULL AND LocatorValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Locator=" + no);
		
		commitEx();
		if (p_IsValidateOnly)
		{
			return "Validated";
		}
		//	-- New Receipts/Deliverys -----------------------------------------------------

		int noInsert = 0;
		int noInsertLine = 0;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		//	Go through Order Records w/o
		sql = new StringBuilder ("SELECT * FROM I_InOut ")
			  .append("WHERE I_IsImported='N'").append (clientCheck)
			.append(" ORDER BY MovementDate,AD_Org_ID,C_Order_ID,C_DocType_ID,DocumentNo,I_InOut_ID");
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			rs = pstmt.executeQuery ();
			//
			String oldDocumentNo = "";
			//
			MOrder order = null;
			MInOut io = null;
			int lineNo = 0;
			while (rs.next ())
			{
				X_I_InOut imp = new X_I_InOut (getCtx (), rs, get_TrxName());
				String cmpDocumentNo = imp.getDocumentNo();
				if (cmpDocumentNo == null)
					cmpDocumentNo = "";
				//	New Order
				if (!oldDocumentNo.equals(cmpDocumentNo))
				{
					
					if(io!=null && p_DocAction.equals(MInOut.ACTION_Complete)) {
						if(!io.processIt(p_DocAction)) {
							log.saveError("Error", io.getProcessMsg());
						}
						io.saveEx();
						commitEx();
						io = null;
					}
					
					oldDocumentNo = imp.getDocumentNo();
					if (oldDocumentNo == null)
						oldDocumentNo = "";
					//
					io = new MInOut (getCtx(), 0, get_TrxName());
					io.setAD_Org_ID(imp.getAD_Org_ID());
					io.setC_DocType_ID(imp.getC_DocType_ID());
					io.setIsSOTrx(imp.isSOTrx());
					if (imp.getDocumentNo() != null)
						io.setDocumentNo(imp.getDocumentNo());
					//
					if (imp.getMovementDate() != null)
						io.setMovementDate(imp.getMovementDate());
					if (imp.getDateAcct() != null)
						io.setDateAcct(imp.getDateAcct());
					//	Ship Partner
					io.setC_BPartner_ID(imp.getC_BPartner_ID());
					io.setC_BPartner_Location_ID(imp.getC_BPartner_Location_ID());
					if (imp.getAD_User_ID() != 0)
						io.setAD_User_ID(imp.getAD_User_ID());
					//
					if (imp.getDescription() != null)
						io.setDescription(imp.getDescription());
					io.setM_Warehouse_ID(imp.getM_Warehouse_ID());
					//	Order
					if(imp.getC_Order_ID() != 0)
					{
						order = new MOrder(getCtx(), imp.getC_Order_ID(), get_TrxName());
						
						io.setC_Order_ID(order.get_ID());
						io.setDateOrdered(order.getDateOrdered());
						io.addDescription(order.getDescription());
						io.setDeliveryRule(order.getDeliveryRule());
						io.setDeliveryViaRule(order.getDeliveryViaRule());
						io.setFreightCostRule(order.getFreightCostRule());
						io.setPriorityRule(order.getPriorityRule());
					}
					
					//	SalesRep from Import or the person running the import
					if (imp.getSalesRep_ID() != 0)
						io.setSalesRep_ID(imp.getSalesRep_ID());
					if (io.getSalesRep_ID() == 0)
						io.setSalesRep_ID(getAD_User_ID());
					//
					if (imp.getC_Activity_ID() != 0)
						io.setC_Activity_ID(imp.getC_Activity_ID());
					if (imp.getC_Campaign_ID() != 0)
						io.setC_Campaign_ID(imp.getC_Campaign_ID());
					if (imp.getC_Project_ID() != 0)
						io.setC_Project_ID(imp.getC_Project_ID());
					if (imp.get_ValueAsInt("User1_ID") != 0)
						io.setUser1_ID(imp.get_ValueAsInt("User1_ID"));
					
					if(imp.getMovementType() != null)
						io.setMovementType(imp.getMovementType());
					
					io.saveEx();
					noInsert++;
					lineNo = 10;
				}
				imp.setM_InOut_ID(io.getM_InOut_ID());
				//	New InOutLine
				MInOutLine line = new MInOutLine (io);
				line.setLine(lineNo);
				lineNo += 10;
				if (imp.getM_Product_ID() != 0) {
					line.setM_Product_ID(imp.getM_Product_ID(), true);
					line.setC_UOM_ID(imp.getC_UOM_ID());
				}
				line.setM_Locator_ID(imp.getM_Locator_ID());
				line.setQty(imp.getQtyEntered());
				if (imp.getLineDescription() != null)
					line.setDescription(imp.getLineDescription());
				if (imp.getC_UOM_ID() != 0)
					line.setC_UOM_ID(imp.getC_UOM_ID());
				//	Reference
				if (imp.getC_Activity_ID() != 0)
					line.setC_Activity_ID(imp.getC_Activity_ID());
				if (imp.get_ValueAsInt("AD_User1") != 0)
					line.setUser1_ID(imp.get_ValueAsInt("AD_User1"));
				if (imp.getC_Campaign_ID() != 0)
					line.setC_Campaign_ID(imp.getC_Campaign_ID());
				if (imp.getC_Project_ID() != 0)
					line.setC_Project_ID(imp.getC_Project_ID());
				
				if(imp.getC_OrderLine_ID() != 0)
					line.setC_OrderLine_ID(imp.getC_OrderLine_ID());
				
				line.saveEx();
				imp.setM_InOutLine_ID(line.getM_InOutLine_ID());
				imp.setI_IsImported(true);
				imp.setProcessed(true);
				//
				if (imp.save())
					noInsertLine++;
			}
			if(io!=null && p_DocAction.equals(MInOut.ACTION_Complete)) {
				if(!io.processIt(p_DocAction)) {
					log.saveError("Error", io.getProcessMsg());
				}
				io.saveEx();
				commitEx();
				io = null;
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
		sql = new StringBuilder ("UPDATE I_InOut ")
			.append("SET I_IsImported='N', Updated=SysDate ")
			.append("WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog (0, null, new BigDecimal (no), "@Errors@");
		//
		addLog (0, null, new BigDecimal (noInsert), "@M_InOut_ID@: @Inserted@");
		addLog (0, null, new BigDecimal (noInsertLine), "@M_InOutLine_ID@: @Inserted@");
		StringBuilder msgreturn = new StringBuilder("#").append(noInsert).append("/").append(noInsertLine);
		return msgreturn.toString();
	}
}
