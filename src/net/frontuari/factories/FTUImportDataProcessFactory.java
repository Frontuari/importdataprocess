package net.frontuari.factories;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import net.frontuari.process.ImportBPBankAccount;
import net.frontuari.process.ImportBPartner;
import net.frontuari.process.ImportDiscountSchema;
import net.frontuari.process.ImportGLJournal;
import net.frontuari.process.ImportInventory;
import net.frontuari.process.ImportInvoice;
import net.frontuari.process.ImportOrder;
import net.frontuari.process.ImportPayment;
import net.frontuari.process.ImportPriceList;
import net.frontuari.process.ImportProduct;

public class FTUImportDataProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		
		if(className.equals("net.frontuari.process.ImportPayment"))
			return new ImportPayment();
		
		if(className.equals("net.frontuari.process.ImportProduct"))
			return new ImportProduct();
		
		if(className.equals("net.frontuari.process.ImportBPartner"))
			return new ImportBPartner();
		
		if(className.equals("net.frontuari.process.ImportBPBankAccount"))
			return new ImportBPBankAccount();
		
		if(className.equals("net.frontuari.process.ImportPriceList"))
			return new ImportPriceList();
		
		if(className.equals("net.frontuari.process.ImportInvoice"))
			return new ImportInvoice();

		if(className.equals("net.frontuari.process.ImportOrder"))
			return new ImportOrder();
		
		if(className.equals("net.frontuari.process.ImportInventory"))
			return new ImportInventory();
		
		if(className.equals("net.frontuari.process.ImportGLJournal"))
			return new ImportGLJournal();
		if(className.equals("net.frontuari.process.ImportDiscountSchema"))
			return new ImportDiscountSchema();
		
		return null;
	}

}
