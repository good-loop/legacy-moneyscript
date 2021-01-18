start: Jan 2020
end: Sep 2022

// Comment the "samples" line below in/out to switch on stochastic modelling
// samples: 50

// Import actuals from a g-drive spreadsheet
// Manually copied from Pete's reports into the `actuals` sheet
import: https://docs.google.com/spreadsheets/d/14DOm3ex5FiG9-k11_9JWuhwS-AeTv-eLrXBZjl9nups/gviz/tq?tqx=out:csv&sheet=actuals {name:"actuals", rows:"overlap"}

//import: https://docs.google.com/spreadsheets/d/135rPC8O7K7Sqx0Ay2QC70BX-E_b87gEMvLNpqdQh1MU/  {name:"Marketing Budget Planning", rows:"overlap"}

// compare against previous spreadsheet plan
compare: https://docs.google.com/spreadsheets/d/19P7GrukxDJhpuQS0BzF62jJC9nSxR8GU4EvB0lCvXPU/gviz/tq?tqx=out:csv&sheet=P%26L {name:"copy Reasonable Estimates PL Petes version"}

Summary:
    Sales_2020: sum(Sales Revenue from Jan 2020 to Dec 2020)
    Sales_2021: sum(Sales Revenue from Jan 2021 to Dec 2021)
    Balance_Close_Jan_2022: Balance at Jan 2022
    CashAtBank_Close_Jan_2022: Closing CashAtBank at Jan 2022
    Balance_Close_Mar_2022: Balance at Mar 2022
    CashAtBank_Close_Mar_2022: Closing CashAtBank at Mar 2022

UK Staff:
    London Staff:
    	Sales Staff:	// salaries are base - commission is done separately
    		Amy: £46k per year
    		Charley: £29k per year
    		Julia: £70k per year
    		Stuart: £55k per year				
    		Nick from Mar 2020: £68k per year	// NB: Amy's sheet counts Brand-Direct staff separately at 1/2 a sales person for new leads. But Nick's been crushing it, so he's counted here as 1.
    		Claire from Mar 2020: £68k per year		
    		Natasha Publicis from Feb 2021: £35k per year
    		ExSalesStaff: // included as the actuals sheet pulls in some old spend 
                Martina Tumwebaze: 0 // ex
                Claudia: 0 // ex
    	Marketing Staff: // no-one yet
    	    Transfer Charley: 0
    	Account Management Staff:
    		Ryan: £60k per year
    		Tom: £40k per year	
	Tech Staff:		
		Daniel Winterstein: £46k per year
		Daniel Appel: £36k per year
		Roscoe: £37k per year
		Eve from May 2020: £25k per year
		Katalin from May 2020: £35k per year
		Ben from Jun 2020: £20k per year
		Kai from Oct 2020: £500 per month	// part-time student
		ExTechStaff:
    		Andris: 0 // ex
            Rachad: 0 // ex
UK Staff: {.bg-blue}


US Staff:
	Barbara from Oct 2020: £800 per month
//	US Remote Sales Agent from Jun 2021: £66k per year
//	Head of US Sales from Jun 2021: £120k per year
US Staff: {.bg-blue}

Contractors: // Trudi and Emilia are under Marketing
	Amanda: £450 per month		
	Pete: £900 per month
	Raimah: £1k per month
	Raimah from Feb 2021: 0
	Motion Designer from Jan 2021: £6k per year // 3x £2k projects?
	Governance & Advisors:
		Chairman from Apr 2021: £1k per month
		Donia Baddou: £1040 per month // Jules Mentor
		Alexandra Jorge: £250 per month // Ryan Mentor - being replaced?
		Kate Ho from Nov 2020: £70 // Kati mentor
		Tech Advisor from Feb 2021: £400 per month
        US Finance from May 2021: £1k per month

Staff Count: count(UK Staff) + count(US Staff)
HeadCount: Staff Count + count(Contractors)


// 4 day week
Amy from Apr 2020 to Sep 2020: * 80%
Daniel Winterstein from Apr 2020 to Sep 2020: * 80%
Ryan from Apr 2020 to Sep 2020: * 80%
Roscoe from Apr 2020 to Jun 2020: * 80%
Tom from Apr 2020 to Oct 2020: * 80%
Eve to Dec 2020: * 80%
Katalin to Dec 2020: * 80%
Ben to Dec 2020: * 80%


Pay Rises: na
    // October / December pay rises
    Amy from Oct 2020: Ryan	
    Daniel Winterstein from Dec 2020: Ryan
    Charley from Dec 2020: + £2k per year
    Stuart from Dec 2020: + £3k per year
    Roscoe from Dec 2020: + £3k per year
    Daniel Appel from Dec 2020: + £1k per year
    // Note: this is slightly over-estimating costs as in-year joiners like Nick, Ben should get less of an end-of-year raise
    UK Staff except(Amy, Dan, Roscoe, Stuart, Charley, DanA) from Dec 2020: * 101.5%
    
    // Students graduate
    Kai from Jun 2021: £24k per year
    Ben from Jun 2021: £26k per year
    
    // Extra pay rise for Stu
    Stuart from Jun 2021: + £2k per year
    Charley from Jun 2021: + £2k per year
    
    // 2021 pay-rises - 4% in Dec 2021? (planning against inflation going up)
    UK Staff from Dec 2021: * 104%  // TODO break out into inflation and performance based


UK Staff:
    New Peeps:
        New Sales Staff:
            Kickstart Sales Assistant A from Jan 2021: £300 per month // Initially hired on govt scheme (with some top-up)
            Kickstart Sales Assistant A from June 2021: £25k per year		
            Kickstart Sales Assistant B from Jan 2021 to June 2021: £300 per month
            // Junior Omnicom Sales Person from Jun 2021: £30k per year // Or is this the KickStart person?
        New Account Management Staff:
            Admin Officer from Jan 2021: £23k per year // full-time so they're heart and soul into the job
            Kickstart AdOps from Feb 2021: £300 per month
            Kickstart AdOps from July 2021: £25k per year
            Kickstart Social Media Community Manager from Jan 2021:  £300 per month // Initially hired on govt scheme (with some top-up)
            Kickstart Social Media Community Manager from June 2021: £0 //£25k per year
//            CFO from June 2021: 20% * £70k per year // start part-time
            // Pete from August 2021: 0
//        New Tech Staff:		
//            AdTech Dev from Apr 2021: £50k per year
            // AdExchange Partnership Manager from Jan 2021: £50k per year // grant fund for a bit from SE??

scenario Hiring_Delay:
    New Sales Staff from Jun 2021: 0
    Kickstart AdOps from Jun 2021: 0
    Kickstart Social Media Community Manager from Jun 2021: 0

Operating Expenses: // costs apart from COS
	People:
		Pay: UK Staff + US Staff + Contractors  // Does not include Sales Commission (in COS) - but this IS included for NI & pension costs
		Employers NI: 12% * (UK Staff + Sales Commission + Profit Share)
		Pension: 3% * (UK Staff + Sales Commission + Profit Share)
		Wellness budget: £100 * count(UK Staff) * 75% 				// let's assume a decent but 100% take-up
		Training: £20 * HeadCount + £100 * count(US Staff) 		// plan for US staff doing a UK visit
		Recruitment from Feb 2021: £0 //£7500 * max(0, Staff Count - previous(Staff Count)) // assumes all staff are recruited
		Childcare Vouchers: // in the past
		Freelancer Bonus: // a one-off
        Profit Share: 3% * (Sales Revenue - COS) // ?? is this the right definition? was prev Advertising Inventory - Business Donations - Brand_Uplift_Research
        Xmas Bonus at Dec 2020: £1k * Staff Count
        Xmas Bonus at Dec 2021: £500 * Staff Count
        Xmas Bonus at Dec 2022: £500 * Staff Count
        
	Property:
		Edinburgh: £450 per month
		Edinburgh from March 2021: £750 per month
		London Club from Jan 2021: £100 * count(London Staff)
        Maintenance: // from the spreadsheet

	Marketing: // £6,833 per month excluding Charley's salary See Amy's planning spreadsheet (which is imported)
        Design Consultant Emilia: £1.1k per month
        PR Trudi: £1.3k per month
        Industry Events: £37k per year
        Owned Events Content:
            Good Scoop: £300 
            Good Insights: £500
            Good News: £300
            Purpose Barometer: £2k per year
        Paid media: 0
        Outbound marketing: 0
        SEO: 0
        Misc Marketing: £50 // design/editing/IAB/Awards:
        Other Marketing: 0

	Travel_Entertainment:
		Travel US from May 2021: £1k per month
		Travel UK from Mar 2021: £500 per month // Edinburgh trips + misc
		Staff Entertainment: £100 * count(London Staff) + £20 * count(Tech Staff) // London are no office + more events
		Client Entertainment: £500 per month

	Professional Fees: // Not including e.g. Pete, Amanda, Donia
	    Accountancy:
		    UK Accountancy: £575			
		  //  US Accountancy from 2022: £500 per month
		Legal:
		    UK Legal: £500 per month
		    UK Legal from Oct 2021: £1k per month
		    US Legal from 2021: £2k per month // a patent = $20k alone
		Consultants: £80 per month // 2020 average to date - but who is included <- B Lab membership

	Other Expenses:
		Insurance: £3k per year
		Telephones Internet: £2.5k per year
		Bank Charges: £100 per month
		Exchange Rate: £100
		Misc Overheads: £300 per month // subs/donations/postage
    
	IT:
	    Hardware: max(0, Staff Count - previous(Staff Count))
		Software: £25 * Staff Count
		Servers: £1200
Operating Expenses: {.bg-red}


// Sales
Income:
    Sales Revenue: £2.6m per year // (Campaign_Size * Num_Campaigns) * Seasonality // This formula gets replaced by the scenarios below
    Revenue Details: na
    	RampedUpStaff: count(Sales Staff from 5 months ago) + count(New Sales Staff from 5 months ago) // This doesn't model churn - e.g. if a new peep comes in and Charley moves over to Marketing
    	CampaignsPerSalesperson: 1+-1 per month // It was about 0.7 in 2019 and 0.9 in 2020 (source: email from Jules re SF data, noting that head-count his blurry). Allowing for covid, it could be higher. This is new and repeat.
    	Num_Campaigns: CampaignsPerSalesperson * RampedUpStaff
    	// Adjustments
    	QuarterSalesFraction: 15%, 15%, 15%, 20%, 20%, 20%, 25%, 25%, 25%, 40%, 40%, 40% // What % of the annual sales are in this quarter? (laid out per month so we can use it in calculations)
    	QuarterSalesFraction: QuarterSalesFraction at 12 months ago // annual loop
    	Seasonality: QuarterSalesFraction / 25% // the % up/down adjustment
    	//Marketing ROI: 0% * (Marketing at 3 months ago) // This is titchy, but because we've really included marketing in sales already
	Grant Income: // Hm - counting grants as income, but perhaps it'd make more sence under financing/investment?
		SE_Early_Stage at Oct 2020: £300k
		SE_RnD_Grant from Nov 2020 to Jun 2021: £50k per year 
		Innovate UK Grant from Nov 2020 to Jun 2021: £10k per month
		SE_Future_Grants from Jan 2021: £10k per year // Assume we win some - but reducing the amount
	Other Revenue from Feb 2021: £0 // Was consultancy revenue from Winterwell

scenario Low_Sales:
    Sales Revenue: (£2.6m per year) * Seasonality // 30% growth on 2020 -- use this for OpEx
    
scenario Medium_Sales:
    Sales Revenue: £3.5m per year * Seasonality // 75% higher growth on 2020 (we've got a wide range on sales predictions as 2021 has market uncertainty)
    
scenario StretchGoal:
    Sales Revenue: £4m per year * Seasonality // 100% growth on 2020

// SF actuals data
// last I checked the campaign size was about N(25k, std-dev 25k). Note that from the data: existing clients don't have a different £ profile.
Campaign_Size to Feb 2021: import aggregate mean(Net Amount) from https://docs.google.com/spreadsheets/d/1FR8Y8LBgMF_aFxTXNPWl9Rc6WQZkdLBckwE61lMLJOw/gviz/tq?tqx=out:csv
Num_Campaigns to Nov 2020: import by month count(Amount) using {"End Date": month, name:"SalesForce closed-won", commentary:"Opportunity Name"} from https://docs.google.com/spreadsheets/d/1FR8Y8LBgMF_aFxTXNPWl9Rc6WQZkdLBckwE61lMLJOw/gviz/tq?tqx=out:csv

Campaign_Size from Mar 2021: N(£30k,400m) // about +10% from 2020
Campaign_Size from Mar 2022: N(£35k,400m)

Revenue Details: {.bg-green}
Campaign_Size: {.bg-green}
Income: {.bg-green}

COS:
	Advertising Inventory: 24% * Sales Revenue // ie we'll get 25% profit margin TODO split self-serve vs managed
	// SHould we model the 6% AppNexus fees?
	Business Donations: 50% * Sales Revenue
	Brand_Uplift_Research: £4k * Num_Campaigns * 20% // Is it really only 20% who have a brand uplift study??
	Sales Commission: 4.75% * Sales Revenue // 4.75% was calculated from the commission calc sheet
	Agent Commission: 5% * Sales Revenue * 10% // estimates for average commission (5%), use of agents (10%)
COS: {.bg-red}



Gross Profit: Income - COS

Net Profit: Gross Profit - Operating Expenses    

Financing:
	Investment: 0
	Investment at Jan 2021: £20k // Nigel
	RnD Tax Credits: £15k per year
	BounceBackLoan_Interest from Aug 2021: -£54 per month
	BounceBackLoan_Capital from Aug 2021: -£833 per month
	SISV Fee: -£313 per month
	SELoan_Interest from Nov 2021: -8% * £235k per year // Pete mentioned £40k a year, but I think that's an overestimate
	Corporation Tax: 0
Financing: {.bg-green}

// Balance
Balance: previous Balance + Net Profit + Financing
// Balance at Jan 2020: £1,196,202 
Balance: {fontWeight: bold}

//Closing CashAtBank at Oct 2020: £1m
Closing CashAtBank: previous(Closing CashAtBank) + Net Profit + Financing + Sales Receipts Delta - DelayedCreditorsDelta // Use deltas to swap which months are counted in the burn rate
Cashflow Details: na
    Sales Receipts Delta: (Sales Revenue at 2 months ago) - Sales Revenue // we get paid 2 months after the cost-of-sale
    Delayed Creditors: Sales Commission + Agent Commission + Business Donations
    DelayedCreditorsDelta: (Delayed Creditors at 3 months ago) - Delayed Creditors // we pay donations and commission after we get paid 
    // TODO VAT     
