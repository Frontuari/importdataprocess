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
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.I_I_Payment;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.X_I_Payment;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.compiere.util.Env;

import net.frontuari.base.FTUProcess;
import net.frontuari.model.FTUMPayment;

/**
 * 	Import Payments
 *	
 *  @author Jorg Janke
 *  @version $Id: ImportPayment.java,v 1.2 2006/07/30 00:51:01 jjanke Exp $
 *  
 *  Contributor(s):
 *    Carlos Ruiz - globalqss - FR [ 1992542 ] Import Payment doesn't have DocAction parameter
 */
public class ImportPayment extends FTUProcess
{
	/**	Organization to be imported to	*/
	private int				p_AD_Org_ID = 0;
	/** Default Bank Account			*/
	private int				p_C_BankAccount_ID = 0;
	/**	Delete old Imported				*/
	private boolean			p_deleteOldImported = false;
	/**	Document Action					*/
	private String			m_docAction = null;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;

	/** Properties						*/
	private Properties 		m_ctx;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			/*else if (name.equals("C_BankAccount_ID"))
				p_C_BankAccount_ID = ((BigDecimal)para[i].getParameter()).intValue();*/
			else if (name.equals("DeleteOldImported"))
				p_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("DocAction"))
				m_docAction = (String)para[i].getParameter();
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		m_ctx = Env.getCtx();
	}	//	prepare

	/**
	 * 	Process
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("C_BankAccount_ID" + p_C_BankAccount_ID);
		MBankAccount ba = MBankAccount.get(getCtx(), p_C_BankAccount_ID);
		
		/*if (p_C_BankAccount_ID == 0 || ba.get_ID() != p_C_BankAccount_ID)
			throw new AdempiereUserError("@NotFound@ @C_BankAccount_ID@ - " + p_C_BankAccount_ID);*/
		
		if (p_AD_Org_ID != ba.getAD_Org_ID() && ba.getAD_Org_ID() != 0)
			p_AD_Org_ID = ba.getAD_Org_ID();
		if (log.isLoggable(Level.INFO)) log.info("AD_Org_ID=" + p_AD_Org_ID);
		
		StringBuilder sql = null;
		int no = 0;
		StringBuilder clientCheck = new StringBuilder(" AND AD_Client_ID=").append(ba.getAD_Client_ID());

		//	****	Prepare	****

		//	Delete Old Imported
		if (p_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE I_Payment ")
				  .append("WHERE I_IsImported='Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}

		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_Payment ")
			  .append("SET AD_Client_ID = COALESCE (AD_Client_ID,").append (ba.getAD_Client_ID()).append ("),")
			  .append(" AD_Org_ID = COALESCE (AD_Org_ID,").append (p_AD_Org_ID).append ("),");
		sql.append(" IsActive = COALESCE (IsActive, 'Y'),")
			  .append(" Created = COALESCE (Created, SysDate),")
			  .append(" CreatedBy = COALESCE (CreatedBy, 0),")
			  .append(" Updated = COALESCE (Updated, SysDate),")
			  .append(" UpdatedBy = COALESCE (UpdatedBy, 0),")
			  .append(" I_ErrorMsg = ' ',")
			  .append(" I_IsImported = 'N' ")
			  .append("WHERE I_IsImported<>'Y' OR I_IsImported IS NULL OR AD_Client_ID IS NULL OR AD_Org_ID IS NULL OR AD_Client_ID=0 OR AD_Org_ID=0");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info ("Reset=" + no);

		sql = new StringBuilder ("UPDATE I_Payment o ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '")
			.append("WHERE (AD_Org_ID IS NULL OR AD_Org_ID=0")
			.append(" OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_Org_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Org=" + no);
		
		sql = new StringBuilder()
				.append("update I_Payment i set C_BankAccount_ID = (Select MAX(ba.C_BankAccount_ID) From C_BankAccount ba WHERE ba.AccountNo = i.AccountNo) WHERE i.C_BankAccount_ID IS NULL AND"
						+ "(I_isImported<>'Y' OR I_isImported IS NULL)").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Bank Account=" + no);

		sql = new StringBuilder()
				.append("update I_Payment i set C_BankAccount_ID = (Select MAX(ba.C_BankAccount_ID) From C_BankAccount ba WHERE ba.AccountNo = i.BankAccountNo) WHERE i.C_BankAccount_ID IS NULL AND"
						+ "(I_isImported<>'Y' OR I_isImported IS NULL)").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Bank Account=" + no);
		
		//	
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET I_isImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Bank Account, ' ")
			.append("WHERE C_BankAccount_ID IS NULL ")
			.append("AND (I_isImported<>'Y' ")
			.append("OR I_isImported IS NULL)").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("Invalid Bank Account=" + no);
		 
		//	Set Currency
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET C_Currency_ID=(SELECT C_Currency_ID FROM C_Currency c")
			.append(" WHERE i.ISO_Code=c.ISO_Code AND c.AD_Client_ID IN (0,i.AD_Client_ID)) ")
			.append("WHERE C_Currency_ID IS NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Set Currency=" + no);
		//
		sql = new StringBuilder("UPDATE I_Payment i ")
			.append("SET C_Currency_ID=(SELECT C_Currency_ID FROM C_BankAccount WHERE C_BankAccount_ID=i.C_BankAccount_ID)")
			.append(" WHERE i.C_Currency_ID IS NULL")
			.append(" AND i.C_BankAccount_ID IS NOT NULL")
			.append(" AND i.ISO_Code IS NULL").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Set Currency=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Payment ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Currency,' ")
			.append("WHERE C_Currency_ID IS NULL ")
			.append("AND I_IsImported<>'E' ")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No Currency=" + no);
		 
		//	Set Amount
		sql = new StringBuilder("UPDATE I_Payment ")
		 	.append("SET ChargeAmt=0 ")
			.append("WHERE ChargeAmt IS NULL ")
			.append("AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Charge Amount=" + no);
		//
		sql = new StringBuilder("UPDATE I_Payment ")
		 	.append("SET TaxAmt=0 ")
			.append("WHERE TaxAmt IS NULL ")
			.append("AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Tax Amount=" + no);
		//
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET WriteOffAmt=0 ")
			.append("WHERE WriteOffAmt IS NULL ")
			.append("AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("WriteOff Amount=" + no);
		//
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET DiscountAmt=0 ")
			.append("WHERE DiscountAmt IS NULL ")
			.append("AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Discount Amount=" + no);
		//
			
		//	Set Date
		sql = new StringBuilder("UPDATE I_Payment ")
		 	.append("SET DateTrx=Created ")
			.append("WHERE DateTrx IS NULL ")
			.append("AND I_isImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Trx Date=" + no);
		
		sql = new StringBuilder("UPDATE I_Payment ")
		 	.append("SET DateAcct=DateTrx ")
			.append("WHERE DateAcct IS NULL ")
			.append("AND I_isImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Acct Date=" + no);
		
		//	Invoice
		sql = new StringBuilder ("UPDATE I_Payment i ")
			  .append("SET C_Invoice_ID=(SELECT MAX(C_Invoice_ID) FROM C_Invoice ii")
			  .append(" WHERE i.InvoiceDocumentNo=ii.DocumentNo AND i.AD_Client_ID=ii.AD_Client_ID) ")
			  .append("WHERE C_Invoice_ID IS NULL AND InvoiceDocumentNo IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set Invoice from DocumentNo=" + no);
		
		//	BPartner
		sql = new StringBuilder ("UPDATE I_Payment i ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp")
			  .append(" WHERE i.BPartnerValue=bp.Value AND i.AD_Client_ID=bp.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set BP from Value=" + no);
		
		sql = new StringBuilder ("UPDATE I_Payment i ")
			  .append("SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_Invoice ii")
			  .append(" WHERE i.C_Invoice_ID=ii.C_Invoice_ID AND i.AD_Client_ID=ii.AD_Client_ID) ")
			  .append("WHERE C_BPartner_ID IS NULL AND C_Invoice_ID IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set BP from Invoice=" + no);
		
		sql = new StringBuilder ("UPDATE I_Payment ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No BPartner,' ")
			.append("WHERE C_BPartner_ID IS NULL ")
			.append("AND I_IsImported<>'E' ")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning("No BPartner=" + no);
		
		
		//	Check Payment<->Invoice combination
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Payment<->Invoice, ' ")
			.append("WHERE I_Payment_ID IN ")
				.append("(SELECT I_Payment_ID ")
				.append("FROM I_Payment i")
				.append(" INNER JOIN C_Payment p ON (i.C_Payment_ID=p.C_Payment_ID) ")
				.append("WHERE i.C_Invoice_ID IS NOT NULL ")
				.append(" AND p.C_Invoice_ID IS NOT NULL ")
				.append(" AND p.C_Invoice_ID<>i.C_Invoice_ID) ")
			.append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Payment<->Invoice Mismatch=" + no);
			
		//	Check Payment<->BPartner combination
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Payment<->BPartner, ' ")
			.append("WHERE I_Payment_ID IN ")
				.append("(SELECT I_Payment_ID ")
				.append("FROM I_Payment i")
				.append(" INNER JOIN C_Payment p ON (i.C_Payment_ID=p.C_Payment_ID) ")
				.append("WHERE i.C_BPartner_ID IS NOT NULL ")
				.append(" AND p.C_BPartner_ID IS NOT NULL ")
				.append(" AND p.C_BPartner_ID<>i.C_BPartner_ID) ")
			.append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Payment<->BPartner Mismatch=" + no);
			
		//	Check Invoice<->BPartner combination
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Invoice<->BPartner, ' ")
			.append("WHERE I_Payment_ID IN ")
				.append("(SELECT I_Payment_ID ")
				.append("FROM I_Payment i")
				.append(" INNER JOIN C_Invoice v ON (i.C_Invoice_ID=v.C_Invoice_ID) ")
				.append("WHERE i.C_BPartner_ID IS NOT NULL ")
				.append(" AND v.C_BPartner_ID IS NOT NULL ")
				.append(" AND v.C_BPartner_ID<>i.C_BPartner_ID) ")
			.append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Invoice<->BPartner Mismatch=" + no);
			
		//	Check Invoice.BPartner<->Payment.BPartner combination
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Invoice.BPartner<->Payment.BPartner, ' ")
			.append("WHERE I_Payment_ID IN ")
				.append("(SELECT I_Payment_ID ")
				.append("FROM I_Payment i")
				.append(" INNER JOIN C_Invoice v ON (i.C_Invoice_ID=v.C_Invoice_ID)")
				.append(" INNER JOIN C_Payment p ON (i.C_Payment_ID=p.C_Payment_ID) ")
				.append("WHERE p.C_Invoice_ID<>v.C_Invoice_ID")
				.append(" AND v.C_BPartner_ID<>p.C_BPartner_ID) ")
			.append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Invoice.BPartner<->Payment.BPartner Mismatch=" + no);
			
		//	TrxType
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET TrxType='S' ")	//	MPayment.TRXTYPE_Sales
			.append("WHERE TrxType IS NULL ")
			.append("AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("TrxType Default=" + no);
		
		//	TenderType
		sql = new StringBuilder("UPDATE I_Payment ")
			.append("SET TenderType='K' ")	//	MPayment.TENDERTYPE_Check
			.append("WHERE TenderType IS NULL ")
			.append("AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("TenderType Default=" + no);

		//	Document Type
		sql = new StringBuilder ("UPDATE I_Payment i ")
			  .append("SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=i.DocTypeName")
			  .append(" AND d.DocBaseType IN ('ARR','APP') AND i.AD_Client_ID=d.AD_Client_ID) ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set DocType=" + no);
		sql = new StringBuilder ("UPDATE I_Payment ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid DocTypeName, ' ")
			  .append("WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid DocTypeName=" + no);
		sql = new StringBuilder ("UPDATE I_Payment ")
			  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No DocType, ' ")
			  .append("WHERE C_DocType_ID IS NULL")
			  .append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("No DocType=" + no);
		
		
		//Add Change by Carlos Vargas
		
		//Set Charge
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET C_Charge_ID=(SELECT C_Charge_ID FROM C_Charge cc")
			.append(" WHERE i.ChargeName=cc.Name AND cc.AD_Client_ID IN (0,i.AD_Client_ID)) ")
			.append("WHERE C_Charge_ID IS NULL")
			.append(" AND ChargeName IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.INFO)) log.info("Set Charge=" + no);
		
		//Added by Adonis Castellanos 21/10/2020
			if (no != 0)
				log.warning ("No Charge=" + no);
			
		sql = new StringBuilder ("UPDATE I_Payment i ")
				.append("SET C_ConversionType_ID=(SELECT MAX(C_ConversionType_ID) FROM C_ConversionType cc")
				.append(" WHERE i.ConversionTypeValue=cc.value AND cc.AD_Client_ID IN (0,i.AD_Client_ID)) ")
				.append("WHERE C_ConversionType_ID IS NULL AND ConversionTypeValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				if (log.isLoggable(Level.INFO)) log.info("Set Charge=" + no);
			sql = new StringBuilder ("UPDATE I_Payment ")
				  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No ConversionType, ' ")
				  .append("WHERE C_ConversionType_ID IS NULL AND ConversionTypeValue IS NOT NULL")
				  .append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				log.warning ("No ConversionType=" + no);
		//End Adonis
			
		//David Castillo
		//	Set Activity
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET C_Activity_ID=(SELECT p.C_Activity_ID FROM C_Activity p")
			.append(" WHERE p.Value=i.ActivityValue AND p.IsSummary='N' AND i.AD_Client_ID=p.AD_Client_ID) ")
			.append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Activity from Value=" + no);
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, '")
			.append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Activity=" + no);
		
		// Set User1_ID David Castillo
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET User1_ID=(SELECT p.C_ElementValue_ID FROM c_elementvalue p")
			.append(" WHERE p.Value=i.User1Value AND p.IsSummary='N' AND i.AD_Client_ID=p.AD_Client_ID) ")
			.append("WHERE User1_ID IS NULL AND User1Value IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set User1 from Value=" + no);
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid User1, '")
			.append("WHERE User1_ID IS NULL AND User1Value IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid User1=" + no);
		
		//	Added by Jorge Colmenarez, 2021-05-17 11:19
		//	Set OrgTrx 
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET AD_OrgTrx_ID=(SELECT o.AD_Org_ID FROM AD_Org o ")
			.append(" WHERE o.Value=i.OrgValue AND o.IsSummary='N' AND i.AD_Client_ID=o.AD_Client_ID) ")
			.append("WHERE AD_OrgTrx_ID IS NULL AND OrgValue IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set OrgTrx from OrgValue=" + no);
		sql = new StringBuilder ("UPDATE I_Payment i ")
			.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid OrgTrx, '")
			.append("WHERE AD_OrgTrx_ID IS NULL AND OrgValue IS NOT NULL")
			.append(" AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid OrgTrx=" + no);
		//	End Jorge Colmenarez

		//David Castillo 23/08/2022 covencaucho new field CVC_CashFlowConcept_ID
		MPayment p = new MPayment(getCtx(), getRecord_ID(), get_TrxName());
		if (p.get_ColumnIndex("CVC_CashFlowConcept_ID") != -1) {
		sql = new StringBuilder ("UPDATE I_Payment i ")
				.append("SET CVC_CashFlowConcept_ID=(SELECT o.CVC_CashFlowConcept_ID FROM CVC_CashFlowConcept o ")
				.append(" WHERE o.Value=i.CVC_CashFlowConceptValue AND i.AD_Client_ID=o.AD_Client_ID) ")
				.append("WHERE CVC_CashFlowConcept_ID IS NULL AND CVC_CashFlowConceptValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Set CVC_CashFlowConcept_ID from CVC_CashFlowConceptValue=" + no);
			sql = new StringBuilder ("UPDATE I_Payment i ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid CVC_CashFlowConceptValue, '")
				.append("WHERE CVC_CashFlowConcept_ID IS NULL AND CVC_CashFlowConceptValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				log.warning ("Invalid CVC_CashFlowConceptValue=" + no);
		}
		
		//Added by David Castillo 05/10/2022 added support for new fields
		
		//SetSalesRep 
		sql = new StringBuilder ("UPDATE I_Payment o ")
		  .append("SET SalesRep_ID=(SELECT AD_User_ID FROM AD_User c")
		  .append(" WHERE o.SalesRep_Name=c.Name AND o.AD_Client_ID=c.AD_Client_ID) ")
		  .append("WHERE SalesRep_ID IS NULL AND SalesRep_Name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set User1=" + no);
		// Set proper error message
		sql = new StringBuilder ("UPDATE I_Payment ")
		  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found SalesRep_ID, ' ")
		  .append("WHERE SalesRep_ID IS NULL AND SalesRep_Name IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
			log.warning("No SalesRep_ID=" + no);
			
			//Added by David Castillo 05/10/2022 added support for new fields
			
			//SetSalesRegion
		sql = new StringBuilder ("UPDATE I_Payment o ")
			.append("SET C_SalesRegion_ID=(SELECT C_SalesRegion_ID FROM C_SalesRegion c")
			.append(" WHERE o.SalesRegionValue=c.Value AND o.AD_Client_ID=c.AD_Client_ID) ")
			.append("WHERE C_SalesRegion_ID IS NULL AND SalesRegionValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set C_SalesRegion_ID=" + no);
		// Set proper error message
		sql = new StringBuilder ("UPDATE I_Payment ")
		  .append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Not Found SalesRep_ID, ' ")
		  .append("WHERE C_SalesRegion_ID IS NULL AND SalesRegionValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
			log.warning("No C_SalesRegion_ID=" + no);
			
			
		commitEx();
		if (p_IsValidateOnly)
		{
			return "@Validated@";
		}
		
		//Import Bank Statement
		sql = new StringBuilder("SELECT * FROM I_Payment")
			.append(" WHERE I_IsImported='N'").append(clientCheck)
			.append(" ORDER BY C_BPartner_ID, DocumentNo, DateTrx ");
			//.append(" ORDER BY C_BankAccount_ID, CheckNo, DateTrx, R_AuthCode"); replaced order by 
			
		MBankAccount account = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int noInsert = 0;
		try
		{
			String oldDocumentNo = "";
			int oldC_BPartner_ID = 0;
			Timestamp oldDate = new Timestamp(System.currentTimeMillis());
			//payment, lineno
			FTUMPayment payment = null;
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
				
			while (rs.next())
			{ 
				X_I_Payment imp = new X_I_Payment(m_ctx, rs, get_TrxName());
				//	Get the bank account
				BigDecimal invoiceAmt = (BigDecimal) imp.get_Value("Amount");
				if (account == null || account.getC_BankAccount_ID() != imp.getC_BankAccount_ID())
				{
					account = MBankAccount.get (m_ctx, imp.getC_BankAccount_ID());
					if (log.isLoggable(Level.INFO)) log.info("New Account=" + account.getAccountNo());
				}
				
				if (oldC_BPartner_ID != imp.getC_BPartner_ID() 
						|| oldDate.compareTo(imp.getDateTrx()) != 0
						|| !oldDocumentNo.equals(imp.getDocumentNo())
						|| !imp.get_ValueAsBoolean("isMultipleInvoice"))
					{
					
					if (payment != null && m_docAction != null && m_docAction.length() > 0)
					{
						payment.setDocAction(m_docAction);
							if(!payment.processIt (m_docAction)) {
								log.warning("Payment Process Failed: " + payment + " - " + payment.getProcessMsg());
								DB.close(rs, pstmt);
								rs = null;
								pstmt = null;
								throw new IllegalStateException("Payment Process Failed: " + payment + " - " + payment.getProcessMsg());
							
								}
						payment.setIsAllocated(imp.get_ValueAsBoolean("IsAllocated"));
						payment.saveEx();
					}
//					New Payment
					payment = new FTUMPayment (m_ctx, 0, get_TrxName());
					payment.setAD_Org_ID(imp.getAD_Org_ID());
					payment.setDocumentNo(imp.get_ValueAsString("DocumentNo"));
					payment.setPONum(imp.getPONum());
					
					//added by Adonis Castellanos
					if(imp.get_ValueAsString("Description")!=null)
						payment.setDescription(imp.get_ValueAsString("Description"));
					
					payment.setTrxType(imp.getTrxType());
					payment.setTenderType(imp.getTenderType());
					
					payment.setC_BankAccount_ID(imp.getC_BankAccount_ID());
					payment.setRoutingNo(imp.getRoutingNo());
					payment.setAccountNo(imp.getAccountNo());
					payment.setIBAN(imp.getIBAN());
					payment.setSwiftCode(imp.getSwiftCode());
					payment.setCheckNo(imp.getCheckNo());
					payment.setMicr(imp.getMicr());					
					if (imp.getCreditCardType() != null)
						payment.setCreditCardType(imp.getCreditCardType());
					payment.setCreditCardNumber(imp.getCreditCardNumber());
					if (imp.getCreditCardExpMM() != 0)
						payment.setCreditCardExpMM(imp.getCreditCardExpMM());
					if (imp.getCreditCardExpYY() != 0)
						payment.setCreditCardExpYY(imp.getCreditCardExpYY());
					payment.setCreditCardVV(imp.getCreditCardVV());
					payment.setSwipe(imp.getSwipe());
					
					payment.setDateAcct(imp.getDateAcct());
					payment.setDateTrx(imp.getDateTrx());
				//	payment.setDescription(imp.getDescription());
					//
					
					if (!imp.get_ValueAsBoolean("isMultipleInvoice")) {
						payment.setC_Invoice_ID(imp.getC_Invoice_ID());
						payment.setC_Charge_ID(imp.getC_Charge_ID());
						payment.setChargeAmt(imp.getChargeAmt());
						
					}
					payment.setC_BPartner_ID(imp.getC_BPartner_ID());
					
					payment.setC_DocType_ID(imp.getC_DocType_ID());
					payment.setC_Currency_ID(imp.getC_Currency_ID());
			
			
					payment.setTaxAmt(imp.getTaxAmt());
					
					payment.setPayAmt(imp.getPayAmt());
					payment.setWriteOffAmt(imp.getWriteOffAmt());
					payment.setDiscountAmt(imp.getDiscountAmt());
					payment.setWriteOffAmt(imp.getWriteOffAmt());
					
					//	Copy statement line reference data
					payment.setA_City(imp.getA_City());
					payment.setA_Country(imp.getA_Country());
					payment.setA_EMail(imp.getA_EMail());
					payment.setA_Ident_DL(imp.getA_Ident_DL());
					payment.setA_Ident_SSN(imp.getA_Ident_SSN());
					payment.setA_Name(imp.getA_Name());
					payment.setA_State(imp.getA_State());
					payment.setA_Street(imp.getA_Street());
					payment.setA_Zip(imp.getA_Zip());
					payment.setR_AuthCode(imp.getR_AuthCode());
					payment.setR_Info(imp.getR_Info());
					payment.setR_PnRef(imp.getR_PnRef());
					payment.setR_RespMsg(imp.getR_RespMsg());
					payment.setR_Result(imp.getR_Result());
					payment.setOrig_TrxID(imp.getOrig_TrxID());
					payment.setVoiceAuthCode(imp.getVoiceAuthCode());
					
					//	Added by Jorge Colmenarez, 2020-01-12 14:46 
					//	Support for set IsAllocated and IsReconciled 
					payment.setIsAllocated(imp.get_ValueAsBoolean("IsAllocated"));
					payment.setIsReconciled(imp.get_ValueAsBoolean("IsReconciled"));
					payment.setDescription(imp.get_ValueAsString("Description"));
					//	End Jorge Colmenarez
					// Added by Adonis Castellanos 21/10/2020
					if(imp.get_ValueAsInt("C_ConversionType_ID")>0)
					payment.setC_ConversionType_ID(imp.get_ValueAsInt("C_ConversionType_ID"));
				
					// End Adonis
					//Added by David Castillo, 2021-02-23 
					if (imp.get_Value("User1_ID") != null) {
						payment.setUser1_ID(imp.get_ValueAsInt("User1_ID"));}
					if (imp.get_Value("C_Activity_ID") != null) {
						payment.set_ValueOfColumn("C_Activity_ID", imp.get_ValueAsInt("C_Activity_ID"));}
					//End David
					//	Added by Jorge Colmenarez, 2021-05-17 15:20
					if(imp.get_ValueAsInt("AD_OrgTrx_ID") > 0)
						payment.setAD_OrgTrx_ID(imp.get_ValueAsInt("AD_OrgTrx_ID"));
					//	End Jorge Colmenarez
					//David Castillo CVC_CashFlowConcept_ID Validation
					if (imp.get_ColumnIndex("CVC_CashFlowConcept_ID") != -1) {
						if (imp.get_ValueAsInt("CVC_CashFlowConcept_ID")>0)
							payment.set_ValueOfColumn("CVC_CashFlowConcept_ID", imp.get_ValueAsInt("CVC_CashFlowConcept_ID"));
					}
					
					//save new fields 05/10/2022
					if (imp.get_ValueAsInt("C_SalesRegion_ID")>0)
						payment.set_ValueOfColumn("C_SalesRegion_ID", imp.get_ValueAsInt("C_SalesRegion_ID"));
					
					if (imp.get_ValueAsInt("SalesRep_ID")>0)
						payment.set_ValueOfColumn("SalesRep_ID", imp.get_ValueAsInt("SalesRep_ID"));
					
					//	Save payment
					if (payment.save()) {

						
						if (imp.get_ValueAsBoolean("isMultipleInvoice")) {
							
							MPaymentAllocate alloc = new MPaymentAllocate(getCtx(), 0, get_TrxName());
							MInvoice inv = new MInvoice(getCtx(), imp.getC_Invoice_ID(), get_TrxName());
							
							alloc.setAD_Org_ID(payment.getAD_Org_ID());
							alloc.setC_Payment_ID(payment.getC_Payment_ID());
							alloc.setAmount(invoiceAmt);
							alloc.setC_Invoice_ID(imp.getC_Invoice_ID());
							alloc.setInvoiceAmt(inv.getGrandTotal());
							alloc.saveEx();
							
							imp.setC_Payment_ID(payment.getC_Payment_ID());
							imp.setI_IsImported(true);
							imp.setProcessed(true);
							imp.saveEx();
							noInsert++;	
						}else {

						imp.setC_Payment_ID(payment.getC_Payment_ID());
						imp.setI_IsImported(true);
						imp.setProcessed(true);
						imp.saveEx();
						noInsert++;	
					
						}
					}
					
						
						//	Group Change
					oldDocumentNo = payment.getDocumentNo();
					oldDate = payment.getDateTrx();
					oldC_BPartner_ID = payment.getC_BPartner_ID();
							
					}else {
			
					MPaymentAllocate alloc = new MPaymentAllocate(getCtx(), 0, get_TrxName());
					MInvoice inv = new MInvoice(getCtx(), imp.getC_Invoice_ID(), get_TrxName());
					
					alloc.setAD_Org_ID(payment.getAD_Org_ID());
					alloc.setC_Payment_ID(payment.getC_Payment_ID());
					alloc.setAmount(invoiceAmt);
					alloc.setC_Invoice_ID(imp.getC_Invoice_ID());
					alloc.setInvoiceAmt(inv.getGrandTotal());
					alloc.saveEx();
					
					imp.setC_Payment_ID(payment.getC_Payment_ID());
					imp.setI_IsImported(true);
					imp.setProcessed(true);
					imp.saveEx();
					noInsert++;
					
					}
				
				
			}
			if (payment != null && m_docAction != null && m_docAction.length() > 0)
			{
				payment.setDocAction(m_docAction);
					if(!payment.processIt (m_docAction)) {
						log.warning("Payment Process Failed: " + payment + " - " + payment.getProcessMsg());
						DB.close(rs, pstmt);
						rs = null;
						pstmt = null;
						throw new IllegalStateException("Payment Process Failed: " + payment + " - " + payment.getProcessMsg());
					
						}
						
				payment.saveEx();
			}
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		//	Set Error to indicator to not imported
		sql = new StringBuilder ("UPDATE I_Payment ")
			.append("SET I_IsImported='N', Updated=SysDate ")
			.append("WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog (0, null, new BigDecimal (no), "@Errors@");
		//
		addLog (0, null, new BigDecimal (noInsert), "@C_Payment_ID@: @Inserted@");
		return "";
	}	//	doIt
	
	private BigDecimal getAccumAmt(int Payment_ID) {
		BigDecimal amt = Env.ZERO;
		
		amt = DB.getSQLValueBD(get_TrxName(), "SELECT COALESCE(SUM(Amount),0) amt FROM C_PaymentAllocate WHERE C_Payment_ID = "  + Payment_ID);
		
		return amt;
	}
	
}	//	ImportPayment
