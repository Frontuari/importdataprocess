package net.frontuari.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MBPBankAccount;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;
import org.compiere.util.Env;

import net.frontuari.base.CustomProcess;

@Process
public class ImportBPBankAccount extends CustomProcess{
	
	private boolean	p_deleteOldImported = false;
	
	private Properties 		m_ctx;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("DeleteOldImported"))
				p_deleteOldImported = "Y".equals(para[i].getParameter());
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		m_ctx = Env.getCtx();
		
	}

	@Override
	protected String doIt() throws Exception {
		
		int no = 0;
		StringBuilder sql = null;
		//Delete Old Imported
		
		if (p_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE FROM I_BPartnerBankAccount WHERE I_IsImported='Y'");
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}
		
		//Set BPartner
		sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET C_BPartner_ID=(SELECT bp.C_BPartner_ID as C_BPartner_ID FROM C_BPartner as bp WHERE bp.taxid=bpi.taxid) WHERE bpi.C_BPartner_ID IS NULL AND bpi.taxid IS NOT NULL AND bpi.I_IsImported<>'Y'");
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set BPartner=" + no);	
		
		//Set A_Name
		sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET A_Name=(SELECT bp.Name as Name FROM C_BPartner as bp WHERE bp.C_BPartner_ID=bpi.C_BPartner_ID) WHERE bpi.A_Name IS NULL AND bpi.I_IsImported<>'Y'");
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set A_Name=" + no);
		
		//Set Bank by Name
		sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET C_Bank_ID=(SELECT cb.C_Bank_ID as C_Bank_ID FROM C_Bank cb WHERE cb.name=bpi.BankName) WHERE bpi.C_Bank_ID IS NULL AND bpi.BankName IS NOT NULL AND bpi.I_IsImported<>'Y'");
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Bank=" + no);
		
		//Set Bank by Routing No
		sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET C_Bank_ID=(SELECT cb.C_Bank_ID as C_Bank_ID FROM C_Bank cb WHERE cb.name=bpi.RoutingNo) WHERE bpi.C_Bank_ID IS NULL AND bpi.RoutingNo IS NOT NULL AND bpi.I_IsImported<>'Y'");
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Bank=" + no);
		
		//Set SecuritySocial Number
		sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET A_Ident_SSN=(SELECT bp.taxid as value FROM C_BPartner bp WHERE bp.C_BPartner_ID=bpi.C_BPartner_ID) WHERE bpi.A_Ident_SSN IS NULL AND bpi.I_IsImported<>'Y'");
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Set Security Social Number=" + no);
		
		commitEx();
			
		int noInsert = 0;
		int noUpdate = 0;
		
		sql = new StringBuilder ("SELECT * FROM I_BPartnerBankAccount ")
				.append("WHERE I_IsImported='N'");
		
		sql.append(" ORDER BY I_BPartnerBankAccount_ID");
		PreparedStatement pstmt =  null;
		ResultSet rs = null;
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				
				StringBuilder vebp = new StringBuilder();
				vebp.append("SELECT C_BPartner_ID FROM C_BPartner where C_BPartner_ID=").append(rs.getInt("C_BPartner_ID"));
				
				int C_BPartner_ID =  DB.getSQLValueEx(get_TrxName(), vebp.toString());
				
				if(C_BPartner_ID != -1) {				
					StringBuilder vsql = new StringBuilder();
					
					vsql.append("SELECT C_BP_BankAccount_ID FROM C_BP_BankAccount where C_BPartner_ID=")
					.append(rs.getInt("C_BPartner_ID"))
					.append(" AND C_Bank_ID=")
					.append(rs.getInt("C_Bank_ID"))
					.append(" AND AccountNo='")
					.append(rs.getString("AccountNo")).append("'");
					
					int CBPartnerAccount_ID = DB.getSQLValueEx(get_TrxName(), vsql.toString());
					
					if(CBPartnerAccount_ID == -1) {
						MBPBankAccount bpba = new MBPBankAccount(m_ctx, 0, get_TrxName());
						bpba.setAD_Org_ID(rs.getInt("AD_Org_ID"));
						bpba.set_ValueOfColumn("AD_Client_ID", rs.getInt("AD_Client_ID"));
						bpba.setC_BPartner_ID(rs.getInt("C_BPartner_ID"));
						bpba.setIsACH(true);
						bpba.setC_Bank_ID(rs.getInt("C_Bank_ID"));
						bpba.setBPBankAcctUse(rs.getString("BPBankAcctUse"));
						bpba.setBankAccountType(rs.getString("BankAccountType"));
						bpba.setRoutingNo(rs.getString("RoutingNo"));
						bpba.setAccountNo(rs.getString("AccountNo"));
						bpba.setIBAN(rs.getString("IBAN"));
						bpba.setA_Name(rs.getString("A_Name"));
						bpba.setA_Ident_SSN(rs.getString("A_Ident_SSN"));
						bpba.saveEx();
						
						noInsert++;
						updateImported(rs.getInt("I_BPartnerBankAccount_ID"));
					}else {
						MBPBankAccount bpba = new MBPBankAccount(m_ctx, CBPartnerAccount_ID, get_TrxName());
						bpba.setC_Bank_ID(rs.getInt("C_Bank_ID"));
						bpba.setBPBankAcctUse(rs.getString("BPBankAcctUse"));
						bpba.setBankAccountType(rs.getString("BankAccountType"));
						bpba.setRoutingNo(rs.getString("RoutingNo"));
						bpba.setAccountNo(rs.getString("AccountNo"));
						bpba.setIBAN(rs.getString("IBAN"));	
						bpba.setA_Name(rs.getString("A_Name"));
						bpba.setA_Ident_SSN(rs.getString("A_Ident_SSN"));
						bpba.saveEx();
						
						noUpdate++;
						updateImported(rs.getInt("I_BPartnerBankAccount_ID"));
					}
				}else {
					updateErrorMessage(rs.getInt("I_BPartnerBankAccount_ID"));
				}

				commitEx();
			}
		}catch(SQLException e) {
			rollback();
			throw new DBException(e, sql.toString());
		}finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
			sql = new StringBuilder ("UPDATE I_BPartnerBankAccount ")
					.append("SET I_IsImported='N', Updated=SysDate ")
					.append("WHERE I_IsImported<>'Y'");
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			//addLog (0, null, new BigDecimal (no), "@Errors@");
			addLog (0, null, new BigDecimal (noInsert), "Cuentas Bancarias de Tercero: @Inserted@");
			addLog (0, null, new BigDecimal (noUpdate), "Cuentas Bancarias de Tercero: @Updated@");
		}
		
		return null;
	}
	
	public void updateImported(int I_BPartnerBankAccount_ID) {
		StringBuilder sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET I_IsImported='Y', Processed='Y' WHERE I_BPartnerBankAccount_ID =")
				.append(I_BPartnerBankAccount_ID);
		DB.executeUpdateEx(sql.toString(), get_TrxName());
	}
	
	public void updateErrorMessage(int I_BPartnerBankAccount_ID) {
		StringBuilder sql = new StringBuilder ("UPDATE I_BPartnerBankAccount as bpi SET I_ErrorMsg='Tercero no Existe' WHERE I_BPartnerBankAccount_ID =")
				.append(I_BPartnerBankAccount_ID);
		DB.executeUpdateEx(sql.toString(), get_TrxName());
	}

}
