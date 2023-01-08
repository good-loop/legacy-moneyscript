package com.goodloop.xero.data;

import java.util.List;
import java.util.Map;

import com.goodloop.data.KCurrency;

public class Invoice {
	
	@Override
	public String toString() {
		return "Invoice [Type=" + Type + ", Contact=" + Contact + ", DateString=" + DateString + ", DueDateString="
				+ DueDateString + ", Status=" + Status + ", LineItems=" + LineItems + ", Total=" + Total
				+ ", InvoiceID=" + InvoiceID + ", InvoiceNumber=" + InvoiceNumber + "]";
	}
	String Type;
	Map Contact;
    String DateString;
    String DueDateString;
    String Status;
    String     LineAmountTypes;
    List LineItems;
//	        {
//	          "ItemCode": "12",
//	          "Description": "Onsite project management ",
//	          "Quantity": "1.0000",
//	          "UnitAmount": "1800.00",
//	          "TaxType": "OUTPUT",
//	          "TaxAmount": "225.00",
//	          "LineAmount": "1800.00",
//	          "AccountCode": "200",
//	          "Item": {
//	                        "ItemID": "fed07c3f-ca77-4820-b4df-304048b3266f",
//	                        "Name": "Test item",
//	                        "Code": "12"
//	                    },
//	          "Tracking": [
//	            {
//	              "TrackingCategoryID": "e2f2f732-e92a-4f3a9c4d-ee4da0182a13",
//	              "Name": "Activity/Workstream",
//	              "Option": "Onsite consultancy"
//	            }
//	          ],
//	          "LineItemID": "52208ff9-528a-4985-a9ad-b2b1d4210e38"
//	        }
//	      ],
	      String SubTotal;
    String TotalTax;
    String Total;
	KCurrency CurrencyCode;
	String InvoiceID;
    String InvoiceNumber;
    List Payments;
//	        {
//	          "Date": "\/Date(1518685950940+0000)\/",
//	          "Amount": "1000.00",
//	          "PaymentID": "0d666415-cf77-43fa-80c7-56775591d426"
//	        }
    String AmountDue;
    String AmountPaid;
    String AmountCredited;	
	
}
