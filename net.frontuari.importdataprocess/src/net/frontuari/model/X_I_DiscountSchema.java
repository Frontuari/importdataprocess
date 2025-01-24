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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;



/** Generated Model for I_DiscountSchema
 *  @author iDempiere (generated) 
 *  @version Release 7.1 - $Id$ */
@org.adempiere.base.Model(table = "I_DiscountSchema")
public class X_I_DiscountSchema extends PO implements I_I_DiscountSchema, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20201215L;

    /** Standard Constructor */
    public X_I_DiscountSchema (Properties ctx, int I_DiscountSchema_ID, String trxName)
    {
      super (ctx, I_DiscountSchema_ID, trxName);
      /** if (I_DiscountSchema_ID == 0)
        {
        } */
    }

    /** Load Constructor */
    public X_I_DiscountSchema (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_I_DiscountSchema[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Trx Organization.
		@param AD_OrgTrx_ID 
		Performing or initiating organization
	  */
	public void setAD_OrgTrx_ID (int AD_OrgTrx_ID)
	{
		if (AD_OrgTrx_ID < 1) 
			set_Value (COLUMNNAME_AD_OrgTrx_ID, null);
		else 
			set_Value (COLUMNNAME_AD_OrgTrx_ID, Integer.valueOf(AD_OrgTrx_ID));
	}

	/** Get Trx Organization.
		@return Performing or initiating organization
	  */
	public int getAD_OrgTrx_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_OrgTrx_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Business Partner Key.
		@param BPartner_Value 
		The Key of the Business Partner
	  */
	public void setBPartner_Value (String BPartner_Value)
	{
		set_Value (COLUMNNAME_BPartner_Value, BPartner_Value);
	}

	/** Get Business Partner Key.
		@return The Key of the Business Partner
	  */
	public String getBPartner_Value () 
	{
		return (String)get_Value(COLUMNNAME_BPartner_Value);
	}

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
    {
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_Name)
			.getPO(getC_BPartner_ID(), get_TrxName());	}

	/** Set Business Partner .
		@param C_BPartner_ID 
		Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner .
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_ConversionType getC_ConversionType() throws RuntimeException
    {
		return (org.compiere.model.I_C_ConversionType)MTable.get(getCtx(), org.compiere.model.I_C_ConversionType.Table_Name)
			.getPO(getC_ConversionType_ID(), get_TrxName());	}

	/** Set Currency Type.
		@param C_ConversionType_ID 
		Currency Conversion Rate Type
	  */
	public void setC_ConversionType_ID (int C_ConversionType_ID)
	{
		if (C_ConversionType_ID < 1) 
			set_Value (COLUMNNAME_C_ConversionType_ID, null);
		else 
			set_Value (COLUMNNAME_C_ConversionType_ID, Integer.valueOf(C_ConversionType_ID));
	}

	/** Get Currency Type.
		@return Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ConversionType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Classification.
		@param Classification 
		Classification for grouping
	  */
	public void setClassification (String Classification)
	{
		set_ValueNoCheck (COLUMNNAME_Classification, Classification);
	}

	/** Get Classification.
		@return Classification for grouping
	  */
	public String getClassification () 
	{
		return (String)get_Value(COLUMNNAME_Classification);
	}

	/** Set Conversion Date.
		@param ConversionDate 
		Date for selecting conversion rate
	  */
	public void setConversionDate (Timestamp ConversionDate)
	{
		set_Value (COLUMNNAME_ConversionDate, ConversionDate);
	}

	/** Get Conversion Date.
		@return Date for selecting conversion rate
	  */
	public Timestamp getConversionDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ConversionDate);
	}

	/** Set Currency Type Key.
		@param ConversionTypeValue 
		Key value for the Currency Conversion Rate Type
	  */
	public void setConversionTypeValue (String ConversionTypeValue)
	{
		set_Value (COLUMNNAME_ConversionTypeValue, ConversionTypeValue);
	}

	/** Get Currency Type Key.
		@return Key value for the Currency Conversion Rate Type
	  */
	public String getConversionTypeValue () 
	{
		return (String)get_Value(COLUMNNAME_ConversionTypeValue);
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** DiscountType AD_Reference_ID=247 */
	public static final int DISCOUNTTYPE_AD_Reference_ID=247;
	/** Flat Percent = F */
	public static final String DISCOUNTTYPE_FlatPercent = "F";
	/** Formula = S */
	public static final String DISCOUNTTYPE_Formula = "S";
	/** Breaks = B */
	public static final String DISCOUNTTYPE_Breaks = "B";
	/** Pricelist = P */
	public static final String DISCOUNTTYPE_Pricelist = "P";
	/** Set Discount Type.
		@param DiscountType 
		Type of trade discount calculation
	  */
	public void setDiscountType (String DiscountType)
	{

		set_Value (COLUMNNAME_DiscountType, DiscountType);
	}

	/** Get Discount Type.
		@return Type of trade discount calculation
	  */
	public String getDiscountType () 
	{
		return (String)get_Value(COLUMNNAME_DiscountType);
	}

	/*public org.frontuari.model.I_FTU_ProductClassifications getFTU_ProductClassifications2() throws RuntimeException
    {
		return (org.frontuari.model.I_FTU_ProductClassifications)MTable.get(getCtx(), org.frontuari.model.I_FTU_ProductClassifications.Table_Name)
			.getPO(getFTU_ProductClassifications2_ID(), get_TrxName());	}*/

	/** Set FTU_ProductClassifications2_ID.
		@param FTU_ProductClassifications2_ID FTU_ProductClassifications2_ID	  */
	/*public void setFTU_ProductClassifications2_ID (int FTU_ProductClassifications2_ID)
	{
		if (FTU_ProductClassifications2_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_FTU_ProductClassifications2_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_FTU_ProductClassifications2_ID, Integer.valueOf(FTU_ProductClassifications2_ID));
	}*/

	/** Get FTU_ProductClassifications2_ID.
		@return FTU_ProductClassifications2_ID	  */
/*	public int getFTU_ProductClassifications2_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FTU_ProductClassifications2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}*/

	/** Set ftu_productclassifications2_value.
		@param ftu_productclassifications2_va ftu_productclassifications2_value	  */
	/*public void setftu_productclassifications2_va (String ftu_productclassifications2_va)
	{
		set_ValueNoCheck (COLUMNNAME_ftu_productclassifications2_va, ftu_productclassifications2_va);
	}*/

	/** Get ftu_productclassifications2_value.
		@return ftu_productclassifications2_value	  */
	/*public String getftu_productclassifications2_va () 
	{
		return (String)get_Value(COLUMNNAME_ftu_productclassifications2_va);
	}*/

	/*public org.frontuari.model.I_FTU_ProductClassifications getFTU_ProductClassifications3() throws RuntimeException
    {
		return (org.frontuari.model.I_FTU_ProductClassifications)MTable.get(getCtx(), org.frontuari.model.I_FTU_ProductClassifications.Table_Name)
			.getPO(getFTU_ProductClassifications3_ID(), get_TrxName());	}*/

	/** Set FTU_ProductClassifications3_ID.
		@param FTU_ProductClassifications3_ID FTU_ProductClassifications3_ID	  */
	/*public void setFTU_ProductClassifications3_ID (int FTU_ProductClassifications3_ID)
	{
		if (FTU_ProductClassifications3_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_FTU_ProductClassifications3_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_FTU_ProductClassifications3_ID, Integer.valueOf(FTU_ProductClassifications3_ID));
	}*/

	/** Get FTU_ProductClassifications3_ID.
		@return FTU_ProductClassifications3_ID	  */
	/*public int getFTU_ProductClassifications3_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FTU_ProductClassifications3_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}*/

	/** Set ftu_productclassifications3_value.
		@param ftu_productclassifications3_va ftu_productclassifications3_value	  */
	/*public void setftu_productclassifications3_va (String ftu_productclassifications3_va)
	{
		set_ValueNoCheck (COLUMNNAME_ftu_productclassifications3_va, ftu_productclassifications3_va);
	}*/

	/** Get ftu_productclassifications3_value.
		@return ftu_productclassifications3_value	  */
	/*public String getftu_productclassifications3_va () 
	{
		return (String)get_Value(COLUMNNAME_ftu_productclassifications3_va);
	}*/

	/*public org.frontuari.model.I_FTU_ProductClassifications getFTU_ProductClassifications() throws RuntimeException
    {
		return (org.frontuari.model.I_FTU_ProductClassifications)MTable.get(getCtx(), org.frontuari.model.I_FTU_ProductClassifications.Table_Name)
			.getPO(getFTU_ProductClassifications_ID(), get_TrxName());	}*/

	/** Set Product Classifications.
		@param FTU_ProductClassifications_ID Product Classifications	  */
	/*public void setFTU_ProductClassifications_ID (int FTU_ProductClassifications_ID)
	{
		if (FTU_ProductClassifications_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_FTU_ProductClassifications_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_FTU_ProductClassifications_ID, Integer.valueOf(FTU_ProductClassifications_ID));
	}*/

	/** Get Product Classifications.
		@return Product Classifications	  */
	/*public int getFTU_ProductClassifications_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FTU_ProductClassifications_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}*/

	/** Set ftu_productclassifications_value.
		@param ftu_productclassifications_val ftu_productclassifications_value	  */
	/*public void setftu_productclassifications_val (String ftu_productclassifications_val)
	{
		set_ValueNoCheck (COLUMNNAME_ftu_productclassifications_val, ftu_productclassifications_val);
	}*/

	/** Get ftu_productclassifications_value.
		@return ftu_productclassifications_value	  */
	/*public String getftu_productclassifications_val () 
	{
		return (String)get_Value(COLUMNNAME_ftu_productclassifications_val);
	}*/

	/** Set Group1.
		@param Group1 Group1	  */
	public void setGroup1 (String Group1)
	{
		set_ValueNoCheck (COLUMNNAME_Group1, Group1);
	}

	/** Get Group1.
		@return Group1	  */
	public String getGroup1 () 
	{
		return (String)get_Value(COLUMNNAME_Group1);
	}

	/** Set Group2.
		@param Group2 Group2	  */
	public void setGroup2 (String Group2)
	{
		set_ValueNoCheck (COLUMNNAME_Group2, Group2);
	}

	/** Get Group2.
		@return Group2	  */
	public String getGroup2 () 
	{
		return (String)get_Value(COLUMNNAME_Group2);
	}

	/** Set I_DiscountSchema_ID.
		@param I_DiscountSchema_ID I_DiscountSchema_ID	  */
	public void setI_DiscountSchema_ID (int I_DiscountSchema_ID)
	{
		if (I_DiscountSchema_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_I_DiscountSchema_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_I_DiscountSchema_ID, Integer.valueOf(I_DiscountSchema_ID));
	}

	/** Get I_DiscountSchema_ID.
		@return I_DiscountSchema_ID	  */
	public int getI_DiscountSchema_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_I_DiscountSchema_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set I_DiscountSchema_UU.
		@param I_DiscountSchema_UU I_DiscountSchema_UU	  */
	public void setI_DiscountSchema_UU (String I_DiscountSchema_UU)
	{
		set_ValueNoCheck (COLUMNNAME_I_DiscountSchema_UU, I_DiscountSchema_UU);
	}

	/** Get I_DiscountSchema_UU.
		@return I_DiscountSchema_UU	  */
	public String getI_DiscountSchema_UU () 
	{
		return (String)get_Value(COLUMNNAME_I_DiscountSchema_UU);
	}

	/** Set Import Error Message.
		@param I_ErrorMsg 
		Messages generated from import process
	  */
	public void setI_ErrorMsg (String I_ErrorMsg)
	{
		set_Value (COLUMNNAME_I_ErrorMsg, I_ErrorMsg);
	}

	/** Get Import Error Message.
		@return Messages generated from import process
	  */
	public String getI_ErrorMsg () 
	{
		return (String)get_Value(COLUMNNAME_I_ErrorMsg);
	}

	/** Set Imported.
		@param I_IsImported 
		Has this import been processed
	  */
	public void setI_IsImported (boolean I_IsImported)
	{
		set_Value (COLUMNNAME_I_IsImported, Boolean.valueOf(I_IsImported));
	}

	/** Get Imported.
		@return Has this import been processed
	  */
	public boolean isI_IsImported () 
	{
		Object oo = get_Value(COLUMNNAME_I_IsImported);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Sales Price list.
		@param IsSOPriceList 
		This is a Sales Price List
	  */
	public void setIsSOPriceList (boolean IsSOPriceList)
	{
		set_Value (COLUMNNAME_IsSOPriceList, Boolean.valueOf(IsSOPriceList));
	}

	/** Get Sales Price list.
		@return This is a Sales Price List
	  */
	public boolean isSOPriceList () 
	{
		Object oo = get_Value(COLUMNNAME_IsSOPriceList);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Limit_Base AD_Reference_ID=194 */
	public static final int LIMIT_BASE_AD_Reference_ID=194;
	/** List Price = L */
	public static final String LIMIT_BASE_ListPrice = "L";
	/** Standard Price = S */
	public static final String LIMIT_BASE_StandardPrice = "S";
	/** Limit (PO) Price = X */
	public static final String LIMIT_BASE_LimitPOPrice = "X";
	/** Fixed Price = F */
	public static final String LIMIT_BASE_FixedPrice = "F";
	/** Product Cost = P */
	public static final String LIMIT_BASE_ProductCost = "P";
	/** Set Limit price Base.
		@param Limit_Base 
		Base price for calculation of the new price
	  */
	public void setLimit_Base (String Limit_Base)
	{

		set_Value (COLUMNNAME_Limit_Base, Limit_Base);
	}

	/** Get Limit price Base.
		@return Base price for calculation of the new price
	  */
	public String getLimit_Base () 
	{
		return (String)get_Value(COLUMNNAME_Limit_Base);
	}

	/** Set Limit price Discount %.
		@param Limit_Discount 
		Discount in percent to be subtracted from base, if negative it will be added to base price
	  */
	public void setLimit_Discount (BigDecimal Limit_Discount)
	{
		set_Value (COLUMNNAME_Limit_Discount, Limit_Discount);
	}

	/** Get Limit price Discount %.
		@return Discount in percent to be subtracted from base, if negative it will be added to base price
	  */
	public BigDecimal getLimit_Discount () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Limit_Discount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Limit_Rounding AD_Reference_ID=155 */
	public static final int LIMIT_ROUNDING_AD_Reference_ID=155;
	/** Whole Number .00 = 0 */
	public static final String LIMIT_ROUNDING_WholeNumber00 = "0";
	/** No Rounding = N */
	public static final String LIMIT_ROUNDING_NoRounding = "N";
	/** Quarter .25 .50 .75 = Q */
	public static final String LIMIT_ROUNDING_Quarter255075 = "Q";
	/** Dime .10, .20, .30, ... = D */
	public static final String LIMIT_ROUNDING_Dime102030 = "D";
	/** Nickel .05, .10, .15, ... = 5 */
	public static final String LIMIT_ROUNDING_Nickel051015 = "5";
	/** Ten 10.00, 20.00, .. = T */
	public static final String LIMIT_ROUNDING_Ten10002000 = "T";
	/** Currency Precision = C */
	public static final String LIMIT_ROUNDING_CurrencyPrecision = "C";
	/** Ending in 9/5 = 9 */
	public static final String LIMIT_ROUNDING_EndingIn95 = "9";
	/** Set Limit price Rounding.
		@param Limit_Rounding 
		Rounding of the final result
	  */
	public void setLimit_Rounding (String Limit_Rounding)
	{

		set_Value (COLUMNNAME_Limit_Rounding, Limit_Rounding);
	}

	/** Get Limit price Rounding.
		@return Rounding of the final result
	  */
	public String getLimit_Rounding () 
	{
		return (String)get_Value(COLUMNNAME_Limit_Rounding);
	}

	/** List_Base AD_Reference_ID=194 */
	public static final int LIST_BASE_AD_Reference_ID=194;
	/** List Price = L */
	public static final String LIST_BASE_ListPrice = "L";
	/** Standard Price = S */
	public static final String LIST_BASE_StandardPrice = "S";
	/** Limit (PO) Price = X */
	public static final String LIST_BASE_LimitPOPrice = "X";
	/** Fixed Price = F */
	public static final String LIST_BASE_FixedPrice = "F";
	/** Product Cost = P */
	public static final String LIST_BASE_ProductCost = "P";
	/** Set List price Base.
		@param List_Base 
		Price used as the basis for price list calculations
	  */
	public void setList_Base (String List_Base)
	{

		set_Value (COLUMNNAME_List_Base, List_Base);
	}

	/** Get List price Base.
		@return Price used as the basis for price list calculations
	  */
	public String getList_Base () 
	{
		return (String)get_Value(COLUMNNAME_List_Base);
	}

	/** Set List price Discount %.
		@param List_Discount 
		Discount from list price as a percentage
	  */
	public void setList_Discount (BigDecimal List_Discount)
	{
		set_ValueNoCheck (COLUMNNAME_List_Discount, List_Discount);
	}

	/** Get List price Discount %.
		@return Discount from list price as a percentage
	  */
	public BigDecimal getList_Discount () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_List_Discount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** List_Rounding AD_Reference_ID=155 */
	public static final int LIST_ROUNDING_AD_Reference_ID=155;
	/** Whole Number .00 = 0 */
	public static final String LIST_ROUNDING_WholeNumber00 = "0";
	/** No Rounding = N */
	public static final String LIST_ROUNDING_NoRounding = "N";
	/** Quarter .25 .50 .75 = Q */
	public static final String LIST_ROUNDING_Quarter255075 = "Q";
	/** Dime .10, .20, .30, ... = D */
	public static final String LIST_ROUNDING_Dime102030 = "D";
	/** Nickel .05, .10, .15, ... = 5 */
	public static final String LIST_ROUNDING_Nickel051015 = "5";
	/** Ten 10.00, 20.00, .. = T */
	public static final String LIST_ROUNDING_Ten10002000 = "T";
	/** Currency Precision = C */
	public static final String LIST_ROUNDING_CurrencyPrecision = "C";
	/** Ending in 9/5 = 9 */
	public static final String LIST_ROUNDING_EndingIn95 = "9";
	/** Set List price Rounding.
		@param List_Rounding 
		Rounding rule for final list price
	  */
	public void setList_Rounding (String List_Rounding)
	{

		set_Value (COLUMNNAME_List_Rounding, List_Rounding);
	}

	/** Get List price Rounding.
		@return Rounding rule for final list price
	  */
	public String getList_Rounding () 
	{
		return (String)get_Value(COLUMNNAME_List_Rounding);
	}

	public org.compiere.model.I_M_DiscountSchema getM_DiscountSchema() throws RuntimeException
    {
		return (org.compiere.model.I_M_DiscountSchema)MTable.get(getCtx(), org.compiere.model.I_M_DiscountSchema.Table_Name)
			.getPO(getM_DiscountSchema_ID(), get_TrxName());	}

	/** Set Discount Schema.
		@param M_DiscountSchema_ID 
		Schema to calculate the trade discount percentage
	  */
	public void setM_DiscountSchema_ID (int M_DiscountSchema_ID)
	{
		if (M_DiscountSchema_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_DiscountSchema_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_DiscountSchema_ID, Integer.valueOf(M_DiscountSchema_ID));
	}

	/** Get Discount Schema.
		@return Schema to calculate the trade discount percentage
	  */
	public int getM_DiscountSchema_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_DiscountSchema_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_DiscountSchemaLine getM_DiscountSchemaLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_DiscountSchemaLine)MTable.get(getCtx(), org.compiere.model.I_M_DiscountSchemaLine.Table_Name)
			.getPO(getM_DiscountSchemaLine_ID(), get_TrxName());	}

	/** Set Discount Pricelist.
		@param M_DiscountSchemaLine_ID 
		Line of the pricelist trade discount schema
	  */
	public void setM_DiscountSchemaLine_ID (int M_DiscountSchemaLine_ID)
	{
		if (M_DiscountSchemaLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_DiscountSchemaLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_DiscountSchemaLine_ID, Integer.valueOf(M_DiscountSchemaLine_ID));
	}

	/** Get Discount Pricelist.
		@return Line of the pricelist trade discount schema
	  */
	public int getM_DiscountSchemaLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_DiscountSchemaLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product_Category getM_Product_Category() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product_Category)MTable.get(getCtx(), org.compiere.model.I_M_Product_Category.Table_Name)
			.getPO(getM_Product_Category_ID(), get_TrxName());	}

	/** Set Product Category.
		@param M_Product_Category_ID 
		Category of a Product
	  */
	public void setM_Product_Category_ID (int M_Product_Category_ID)
	{
		if (M_Product_Category_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Product_Category_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Product_Category_ID, Integer.valueOf(M_Product_Category_ID));
	}

	/** Get Product Category.
		@return Category of a Product
	  */
	public int getM_Product_Category_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_Category_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_Value (COLUMNNAME_M_Product_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Organization Name.
		@param OrgName 
		Name of the Organization
	  */
	public void setOrgName (String OrgName)
	{
		set_ValueNoCheck (COLUMNNAME_OrgName, OrgName);
	}

	/** Get Organization Name.
		@return Name of the Organization
	  */
	public String getOrgName () 
	{
		return (String)get_Value(COLUMNNAME_OrgName);
	}

	/** Set Processed.
		@param Processed 
		The document has been processed
	  */
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Process Now.
		@param Processing Process Now	  */
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing () 
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Product Category Key.
		@param ProductCategory_Value Product Category Key	  */
	public void setProductCategory_Value (String ProductCategory_Value)
	{
		set_Value (COLUMNNAME_ProductCategory_Value, ProductCategory_Value);
	}

	/** Get Product Category Key.
		@return Product Category Key	  */
	public String getProductCategory_Value () 
	{
		return (String)get_Value(COLUMNNAME_ProductCategory_Value);
	}

	/** Set Product Name.
		@param ProductName 
		Name of the Product
	  */
	public void setProductName (String ProductName)
	{
		set_ValueNoCheck (COLUMNNAME_ProductName, ProductName);
	}

	/** Get Product Name.
		@return Name of the Product
	  */
	public String getProductName () 
	{
		return (String)get_Value(COLUMNNAME_ProductName);
	}

	/** Set SKU.
		@param SKU 
		Stock Keeping Unit
	  */
	public void setSKU (String SKU)
	{
		set_ValueNoCheck (COLUMNNAME_SKU, SKU);
	}

	/** Get SKU.
		@return Stock Keeping Unit
	  */
	public String getSKU () 
	{
		return (String)get_Value(COLUMNNAME_SKU);
	}

	/** Std_Base AD_Reference_ID=194 */
	public static final int STD_BASE_AD_Reference_ID=194;
	/** List Price = L */
	public static final String STD_BASE_ListPrice = "L";
	/** Standard Price = S */
	public static final String STD_BASE_StandardPrice = "S";
	/** Limit (PO) Price = X */
	public static final String STD_BASE_LimitPOPrice = "X";
	/** Fixed Price = F */
	public static final String STD_BASE_FixedPrice = "F";
	/** Product Cost = P */
	public static final String STD_BASE_ProductCost = "P";
	/** Set Standard price Base.
		@param Std_Base 
		Base price for calculating new standard price
	  */
	public void setStd_Base (String Std_Base)
	{

		set_Value (COLUMNNAME_Std_Base, Std_Base);
	}

	/** Get Standard price Base.
		@return Base price for calculating new standard price
	  */
	public String getStd_Base () 
	{
		return (String)get_Value(COLUMNNAME_Std_Base);
	}

	/** Set Standard price Discount %.
		@param Std_Discount 
		Discount percentage to subtract from base price
	  */
	public void setStd_Discount (BigDecimal Std_Discount)
	{
		set_Value (COLUMNNAME_Std_Discount, Std_Discount);
	}

	/** Get Standard price Discount %.
		@return Discount percentage to subtract from base price
	  */
	public BigDecimal getStd_Discount () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Std_Discount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Std_Rounding AD_Reference_ID=155 */
	public static final int STD_ROUNDING_AD_Reference_ID=155;
	/** Whole Number .00 = 0 */
	public static final String STD_ROUNDING_WholeNumber00 = "0";
	/** No Rounding = N */
	public static final String STD_ROUNDING_NoRounding = "N";
	/** Quarter .25 .50 .75 = Q */
	public static final String STD_ROUNDING_Quarter255075 = "Q";
	/** Dime .10, .20, .30, ... = D */
	public static final String STD_ROUNDING_Dime102030 = "D";
	/** Nickel .05, .10, .15, ... = 5 */
	public static final String STD_ROUNDING_Nickel051015 = "5";
	/** Ten 10.00, 20.00, .. = T */
	public static final String STD_ROUNDING_Ten10002000 = "T";
	/** Currency Precision = C */
	public static final String STD_ROUNDING_CurrencyPrecision = "C";
	/** Ending in 9/5 = 9 */
	public static final String STD_ROUNDING_EndingIn95 = "9";
	/** Set Standard price Rounding.
		@param Std_Rounding 
		Rounding rule for calculated price
	  */
	public void setStd_Rounding (String Std_Rounding)
	{

		set_Value (COLUMNNAME_Std_Rounding, Std_Rounding);
	}

	/** Get Standard price Rounding.
		@return Rounding rule for calculated price
	  */
	public String getStd_Rounding () 
	{
		return (String)get_Value(COLUMNNAME_Std_Rounding);
	}

	/** Set Valid from.
		@param ValidFrom 
		Valid from including this date (first day)
	  */
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_Value (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}


}