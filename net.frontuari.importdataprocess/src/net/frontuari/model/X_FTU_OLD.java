/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package net.frontuari.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for FTU_OLD
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="FTU_OLD")
public class X_FTU_OLD extends PO implements I_FTU_OLD, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250526L;

    /** Standard Constructor */
    public X_FTU_OLD (Properties ctx, int FTU_OLD_ID, String trxName)
    {
      super (ctx, FTU_OLD_ID, trxName);
      /** if (FTU_OLD_ID == 0)
        {
			setFTU_OLD_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_FTU_OLD (Properties ctx, int FTU_OLD_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, FTU_OLD_ID, trxName, virtualColumns);
      /** if (FTU_OLD_ID == 0)
        {
			setFTU_OLD_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_FTU_OLD (Properties ctx, String FTU_OLD_UU, String trxName)
    {
      super (ctx, FTU_OLD_UU, trxName);
      /** if (FTU_OLD_UU == null)
        {
			setFTU_OLD_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_FTU_OLD (Properties ctx, String FTU_OLD_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, FTU_OLD_UU, trxName, virtualColumns);
      /** if (FTU_OLD_UU == null)
        {
			setFTU_OLD_ID (0);
        } */
    }

    /** Load Constructor */
    public X_FTU_OLD (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_FTU_OLD[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_ElementValue getAccount() throws RuntimeException
	{
		return (org.compiere.model.I_C_ElementValue)MTable.get(getCtx(), org.compiere.model.I_C_ElementValue.Table_ID)
			.getPO(getAccount_ID(), get_TrxName());
	}

	/** Set Account.
		@param Account_ID Account used
	*/
	public void setAccount_ID (int Account_ID)
	{
		if (Account_ID < 1)
			set_ValueNoCheck (COLUMNNAME_Account_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Account_ID, Integer.valueOf(Account_ID));
	}

	/** Get Account.
		@return Account used
	  */
	public int getAccount_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Account_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Amount.
		@param Amount Amount in a defined currency
	*/
	public void setAmount (BigDecimal Amount)
	{
		set_ValueNoCheck (COLUMNNAME_Amount, Amount);
	}

	/** Get Amount.
		@return Amount in a defined currency
	  */
	public BigDecimal getAmount()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Amount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException
	{
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_ID)
			.getPO(getC_OrderLine_ID(), get_TrxName());
	}

	/** Set Sales Order Line.
		@param C_OrderLine_ID Sales Order Line
	*/
	public void setC_OrderLine_ID (int C_OrderLine_ID)
	{
		if (C_OrderLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_OrderLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_OrderLine_ID, Integer.valueOf(C_OrderLine_ID));
	}

	/** Get Sales Order Line.
		@return Sales Order Line
	  */
	public int getC_OrderLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Description_Line.
		@param Description_Line Description_Line
	*/
	public void setDescription_Line (String Description_Line)
	{
		set_ValueNoCheck (COLUMNNAME_Description_Line, Description_Line);
	}

	/** Get Description_Line.
		@return Description_Line	  */
	public String getDescription_Line()
	{
		return (String)get_Value(COLUMNNAME_Description_Line);
	}

	/** Set Order Line Distributive.
		@param FTU_OLD_ID Order Line Distributive
	*/
	public void setFTU_OLD_ID (int FTU_OLD_ID)
	{
		if (FTU_OLD_ID < 1)
			set_ValueNoCheck (COLUMNNAME_FTU_OLD_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_FTU_OLD_ID, Integer.valueOf(FTU_OLD_ID));
	}

	/** Get Order Line Distributive.
		@return Order Line Distributive	  */
	public int getFTU_OLD_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FTU_OLD_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set FTU_OLD_UU.
		@param FTU_OLD_UU FTU_OLD_UU
	*/
	public void setFTU_OLD_UU (String FTU_OLD_UU)
	{
		set_Value (COLUMNNAME_FTU_OLD_UU, FTU_OLD_UU);
	}

	/** Get FTU_OLD_UU.
		@return FTU_OLD_UU	  */
	public String getFTU_OLD_UU()
	{
		return (String)get_Value(COLUMNNAME_FTU_OLD_UU);
	}

	public org.compiere.model.I_C_ElementValue getUserLine1() throws RuntimeException
	{
		return (org.compiere.model.I_C_ElementValue)MTable.get(getCtx(), org.compiere.model.I_C_ElementValue.Table_ID)
			.getPO(getUserLine1_ID(), get_TrxName());
	}

	/** Set User Element List 1.
		@param UserLine1_ID User defined list element #1
	*/
	public void setUserLine1_ID (int UserLine1_ID)
	{
		if (UserLine1_ID < 1)
			set_Value (COLUMNNAME_UserLine1_ID, null);
		else
			set_Value (COLUMNNAME_UserLine1_ID, Integer.valueOf(UserLine1_ID));
	}

	/** Get User Element List 1.
		@return User defined list element #1
	  */
	public int getUserLine1_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_UserLine1_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}