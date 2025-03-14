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
 * Contributor: Jorge Colmenarez - Frontuari, CA                              *
 *****************************************************************************/
package net.frontuari.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.DBException;
import org.adempiere.model.ImportValidator;
import org.adempiere.process.ImportProcess;
import org.compiere.model.ModelValidationEngine;

import net.frontuari.base.CustomProcess;
import net.frontuari.model.X_I_Employee;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.eevolution.model.X_HR_Employee;

/**
 *	Import Employee from I_Employee
 *
 * 	@author 	Jorge Colmenarez
 * 	@version 	$Id: ImportEmployee.java,v 1.0 2021/03/25 12:46 jlctmaster Exp $
 */
@Process
public class ImportEmployee extends CustomProcess implements ImportProcess {

	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;

	public ImportEmployee() {
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
			if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		m_AD_Client_ID = getAD_Client_ID();
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
		String clientCheck = getWhereClause();

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE I_Employee ")
					.append("WHERE I_IsImported='Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}

		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_Employee ")
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

		ModelValidationEngine.get().fireImportValidate(this, null, null, ImportValidator.TIMING_BEFORE_VALIDATE);
		
		//	Set BPartner
		sql = new StringBuilder ("UPDATE I_Employee i ")
				.append("SET C_BPartner_ID=(SELECT MAX(bp.C_BPartner_ID) FROM C_BPartner bp ")
				.append("WHERE bp.Value=i.BPValue")
				.append(" AND bp.AD_Client_ID=i.AD_Client_ID) ");
		sql.append("WHERE BPValue IS NOT NULL AND C_BPartner_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BPartner=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Employee ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid BPartner, ' ")
				.append("WHERE C_BPartner_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid BPartner=" + no);
		
		//	Set Department
		sql = new StringBuilder ("UPDATE I_Employee i ")
				.append("SET HR_Department_ID=(SELECT MAX(HR_Department_ID) FROM HR_Department d ")
				.append("WHERE d.Value=i.DepartmentValue")
				.append(" AND d.AD_Client_ID=i.AD_Client_ID) ");
		sql.append("WHERE DepartmentValue IS NOT NULL AND HR_Department_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Department=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Employee ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Department, ' ")
				.append("WHERE HR_Department_ID IS NULL AND DepartmentValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid Department=" + no);
		
		//	Set Job
		sql = new StringBuilder ("UPDATE I_Employee i ")
				.append("SET HR_Job_ID=(SELECT MAX(HR_Job_ID) FROM HR_Job j ")
				.append("WHERE j.Value=i.JobValue")
				.append(" AND j.AD_Client_ID=i.AD_Client_ID) ");
		sql.append("WHERE JobValue IS NOT NULL AND HR_Job_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Job=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Employee ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Job, ' ")
				.append("WHERE HR_Job_ID IS NULL AND JobValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid Department=" + no);
		
		//	Set Payroll
		sql = new StringBuilder ("UPDATE I_Employee i ")
				.append("SET HR_Payroll_ID=(SELECT MAX(HR_Payroll_ID) FROM HR_Payroll p ")
				.append("WHERE p.Value=i.PayrollValue")
				.append(" AND p.AD_Client_ID=i.AD_Client_ID) ");
		sql.append("WHERE PayrollValue IS NOT NULL AND HR_Payroll_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Payroll=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Employee ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Payroll, ' ")
				.append("WHERE HR_Payroll_ID IS NULL AND PayrollValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid Payroll=" + no);
		
		//	Set Activity
		sql = new StringBuilder ("UPDATE I_Employee i ")
				.append("SET C_Activity_ID=(SELECT MAX(C_Activity_ID) FROM C_Activity a ")
				.append("WHERE a.Value=i.ActivityValue")
				.append(" AND a.AD_Client_ID=i.AD_Client_ID) ");
		sql.append("WHERE ActivityValue IS NOT NULL AND C_Activity_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Activity=" + no);
		//
		sql = new StringBuilder ("UPDATE I_Employee ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, ' ")
				.append("WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid Activity=" + no);
		
		ModelValidationEngine.get().fireImportValidate(this, null, null, ImportValidator.TIMING_AFTER_VALIDATE);

		commitEx();
		if (p_IsValidateOnly)
		{
			return "Validated";
		}
		//	-------------------------------------------------------------------
		int noInsert = 0;
		int noUpdate = 0;

		//	Go through Records
		sql = new StringBuilder ("SELECT * FROM I_Employee ")
				.append("WHERE I_IsImported='N'").append(clientCheck);
		sql.append(" ORDER BY BPValue, Code, I_Employee_ID");
		PreparedStatement pstmt =  null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				X_I_Employee impE = new X_I_Employee(getCtx(), rs, get_TrxName());
				StringBuilder msglog = new StringBuilder("I_Employee_ID=") .append(impE.getI_Employee_ID())
						.append(", HR_Employee_ID=").append(impE.getHR_Employee_ID());
				if (log.isLoggable(Level.FINE)) log.fine(msglog.toString());
				
				//	Search Employee by BPartner and Code
				int EmployeeID = DB.getSQLValue(get_TrxName(), "SELECT HR_Employee_ID FROM HR_Employee WHERE C_BPartner_ID = ? AND NationalCode = ? AND AD_Client_ID = ?", 
						new Object[] {impE.getC_BPartner_ID(),impE.getNationalCode(),m_AD_Client_ID});
				
				if(EmployeeID<0)
					EmployeeID = 0;
				
				//	New/Update Employee
				X_HR_Employee e = new X_HR_Employee(getCtx(), EmployeeID, get_TrxName());
				e.setC_BPartner_ID(impE.getC_BPartner_ID());
				e.setName(impE.getC_BPartner().getName());
				e.setCode(impE.getCode());
				e.set_ValueOfColumn("Gender", impE.getGender());
				e.setNationalCode(impE.getNationalCode());
				e.setSSCode(impE.getSSCode());
				e.setStartDate(impE.getStartDate());
				e.setEndDate(impE.getEndDate());
				e.setHR_Department_ID(impE.getHR_Department_ID());
				e.setHR_Job_ID(impE.getHR_Job_ID());
				e.setHR_Payroll_ID(impE.getHR_Payroll_ID());
				e.setC_Activity_ID(impE.getC_Activity_ID());
				
				if(e.save())
				{
					impE.setHR_Employee_ID(e.getHR_Employee_ID());
					msglog = new StringBuilder("Insert Employee - ").append(e.getHR_Employee_ID());
					if (log.isLoggable(Level.FINEST)) log.finest(msglog.toString());
					if(EmployeeID > 0)
						noInsert++;
					else
						noUpdate++;
				}
				else
				{
					sql = new StringBuilder ("UPDATE I_Employee i ")
							.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||")
					.append("'Cannot Insert Employee, ' ")
					.append("WHERE I_Employee_ID=").append(impE.getI_Employee_ID());
					DB.executeUpdateEx(sql.toString(), get_TrxName());
					no++;
					continue;
				}
				//	Set Import Employee Processed
				impE.setI_IsImported(true);
				impE.setProcessed(true);
				impE.setProcessing(false);
				impE.saveEx();
				commitEx();
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
			sql = new StringBuilder ("UPDATE I_Employee ")
					.append("SET I_IsImported='N', Updated=SysDate ")
					.append("WHERE I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			addLog (0, null, new BigDecimal (no), "@Errors@");
			addLog (0, null, new BigDecimal (noInsert), "@HR_Employee_ID@: @Inserted@");
			addLog (0, null, new BigDecimal (noUpdate), "@HR_Employee_ID@: @Updated@");
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
		return X_I_Employee.Table_Name;
	}
	
}
