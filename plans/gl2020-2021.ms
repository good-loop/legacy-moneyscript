start: Jan 2020
end: Sep 2022

//samples: 20

// Import actuals from a g-drive spreadsheet
// Manually copied over by Dan into the `actuals` sheet
// NB: G-drive is a bit slow to respond, so by default we cache the g-drive response for a minute. Use "import fresh" to switch off caching.
import: https://docs.google.com/spreadsheets/d/14DOm3ex5FiG9-k11_9JWuhwS-AeTv-eLrXBZjl9nups/gviz/tq?tqx=out:csv&sheet=actuals {url:https://docs.google.com/spreadsheets/d/14DOm3ex5FiG9-k11_9JWuhwS-AeTv-eLrXBZjl9nups, name:"actuals", rows:"all"}

UK Staff:
    London Staff:
    	Sales Staff:	// salaries are base - commission is done separately
    		Amy: £46k per year
    		Charley: £29k per year
    		Julia: £70k per year
    		Stuart: £55k per year				
    		Nick from Mar 2020: £68k per year	// NB: Amy's sheet counts Brand-Direct staff separately at 1/2 a sales person for new leads. But Nick's been crushing it, so he's counted here as 1.
    		Claire from Mar 2020: £68k per year		
    		Junior Sales Publicis from Jan 2021: £35k per year
    		ExSalesStaff:
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

Contractors:
	Amanda: £450 per month		
	Pete: £150 per month
	Raimah: £1k per month
	Governance & Advisors:
		Chairman from Apr 2021: £1k per month
		Donia Baddou: £1040 per month // Jules Mentor
		Alexandra Jorge: £250 per month // Ryan Mentor
		Kate Ho from Nov 2020: £70 // Kati mentor
		Tech Advisor from Jan 2021: £400 per month

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


// Pay Rises
// October / December pay rises
Amy from Oct 2020: Ryan	
Daniel Winterstein from Dec 2020: Ryan
Charley from Dec 2020: + £2k per year
Stuart from Dec 2020: + £3k per year
Roscoe from Dec 2020: + £3k per year
Daniel Appel from Dec 2020: + £1k per year
// TODO in-year joiners like Nick, Ben, Raimah get less of an end-of-year raise
UK Staff except(Amy, Dan, Roscoe, Stuart, Charley, DanA) from Dec 2020: * 101.5%

// Students graduate
Kai from Jun 2021: £24k per year
Ben from Jun 2021: £26k per year

// Extra pay rise for Stu
Stuart from Jun 2021: + £2k per year

// 2021 pay-rises - 4% in Dec 2021? (inflation is likely to go up)
UK Staff from Dec 2021: * 104%


UK Staff:
    New Peeps:
        New Sales Staff:
            Kickstart Sales Assistant A from Jan 2020: £300 per month // Initially hired on govt scheme (with some top-up)
            Kickstart Sales Assistant A from June 2021: £25k per year		
            Kickstart Sales Assistant B from Dec 2020 to June 2021: £300 per month
    	    Junior Agency Salesperson1 from Jan 2021: £29k per year
        New Marketing Staff:        
        	Transfer Charley from Jun 2021: £31k per year // So we'd hire a new salesperson (modelled by the Charley line) and Charley would move over
        New Account Management Staff:
            Admin Officer from Jan 2021: 100% * £30k per year // full-time so they're heart and soul into the job
            Kickstart Social Media Community Manager from Dec 2020:  £300 per month // Initially hired on govt scheme (with some top-up)
            Kickstart Social Media Community Manager from June 2021: £25k per year
            CFO from June 2021: 20% * £70k per year // start part-time
        New Contractors:
            Raimah from Mar 2021: 0 // Sadly we expect to part ways with Raimah
            Motion Designer from Jan 2021: £1k per month // This will be off and on
        New Tech Staff:		
//            AdTech Dev from Apr 2021: £50k per year
            AdExchange Partnership Manager from Jan 2021: £50k per year // grant fund for a bit from SE??

Costs:
	HR Costs:
		Pay: UK Staff + US Staff + Contractors  // Does not include Sales Commission (in COS) - but this IS included for NI & pension costs
		Employers NI: 12% * (UK Staff + Sales Commission + Profit Share)
		Pension: 3% * (UK Staff + Sales Commission + Profit Share)
		Wellness budget: £100 * count(UK Staff) * 75% 				// let's assume a decent but 100% take-up
		Training: £20 * HeadCount + £100 * count(US Staff) 		// plan for US staff doing a UK visit
		Recruitment from Jan 2021: £7500 * max(0, Staff Count - previous(Staff Count)) // assumes all staff are recruited
		Childcare Vouchers: // in the past
		Freelancer Bonus: // a one-off
        Profit Share: 3% * (Sales_Revenue - COS) // ?? is this the right definition?	was prev Advertising Inventory - Business Donations - Brand_Uplift_Research
        Xmas Bonus at Dec 2020: £500 * Staff Count
        Xmas Bonus at Dec 2021: £500 * Staff Count
        Xmas Bonus at Dec 2022: £500 * Staff Count
        
	Rent:
		Edinburgh: £450 per month
		Edinburgh from March 2021: £750 per month
		London Club from Jan 2021: £100 * count(London Staff)
        Maintenance: // from the spreadsheet

	Marketing: 
    	Trudi: £3k per month
		Design Consultant Emilia: £1.4k per month
		Misc Marketing: £50 per month // design/editing/IAB/Awards
		Owned Events: £1150 per month
		Industry Events: £2500 per month
		Content Creation from Jan 2021: £1k per month
        Other Marketing: // a category in the spreadsheet
        Misc design editing IAB Awards: £250 // TODO average(actuals) an ad-hoc category in the actuals spreadsheet
        Paid Media: £1k per month

	Travel_Entertainment:
		Travel: £100
		Accommodation: £100
		Subsistence: £100
		Staff Entertainment: £100 * count(London Staff) + £20 * count(Tech Staff) // London are no office + more events
		Client Entertainment: £200 per month
	Travel_Entertainment from Feb 2021: * 500% // lockdown lifts?

	Professional Fees: // Not including e.g. Pete, Amanda, Donia
	    Accountancy:
		    UK Accountancy: £575			
		    US Accountancy from 2021: £750 per month
		Legal:
		    UK Legal: £500 per month
		    UK Legal from August 2021: £1k per month
		    US Legal from 2021: £2k per month // a patent = $20k alone
		Consultants: £80 per month // 2020 average to date - but who is included?

	Other Expenses:
		Insurance: £3k per year
		Telephones Internet: £2.5k per year
		Bank Charges: £6,400 per year
		Exchange Rate: £100
		Misc Overheads: £300 per month // subs/donations/postage
    
	IT:
		Software: £25 * Staff Count
		Servers: £1200
Costs: {.bg-red}


// Sales
Sales_Revenue: {.bg-green}
Sales_Revenue: (Marketing ROI + (Campaign_Size * UK_Campaigns)) * Seasonality
Revenue Details: na
    // TODO let's split into bigger deals which get a brand uplift study and smaller deals
    // Maybe - model sales staff by person, using their SF track record
    // + SF pipeline for next quarter
    // + some overrides for e.g. Claire
    // TODO Do put optimistic scaled growth (repeat sales get bigger, sales-people win more) in for e.g. Series A!
    // could also be always-on deals, saas, data, tabs for good
	RampedUpStaff: count(Sales Staff from 5 months ago) // This doesn't model churn - e.g. if a new peep comes in and Charley moves over to Marketing
	CampaignsPerSalesperson: 1+-1 per month // It was about 0.7 in 2019 and 0.9 in 2020 (source: email from Jules re SF data, noting that head-count his blurry). Allowing for covid, it could be higher. This is new and repeat.
	UK_Campaigns: CampaignsPerSalesperson * RampedUpStaff
	Campaign_Size: N(23k, 451m) // Checking the data: new and existing clients have the same profile
	// Adjustments
	Seasonality: 70%,	75%,	90%,	115%,	125%,	115%,	90%,	80%,	100%,	100%,	115%,	125%
	Seasonality: Seasonality at 12 months ago // annual loop
	Marketing ROI: 20% * (Marketing at 3 months ago + Trudi at 3 months ago) // This is titchy, but because we've really included marketing in sales already
Revenue Details: {.bg-green}


COS:
	Advertising Inventory: 25% * Sales_Revenue // ie we'll get 25% profit margin TODO split self-serve vs managed
	// SHould we model the 6% AppNexus fees?
	Business Donations: 50% * Sales_Revenue
	Brand_Uplift_Research: £4k * UK_Campaigns * 20% // Is it really only 20% who have a brand uplift study??
	Sales Commission: 4.75% * Sales_Revenue // Calculated from commission calc sheet
	Agent Commission: 5% * Sales_Revenue * 10% // estimates for average commission (5%), use of agents (10%)
COS: {.bg-red}


Financing:
	Investment: 0
	RnD Tax Credits: £15k per year
	Grant Income:
		SE_Early_Stage at Oct 2020: £300k
		SE_RnD_Grant from Nov 2020 to Jun 2021: £50k per year 
		Innovate UK Grant from Nov 2020 to Jun 2021: £10k per month
		SE_Future_Grants from Jan 2021: £10k per year
	Other Revenue: £500 // Is this from Winterwell?
	BounceBackLoan_Interest from Aug 2021: -£54 per month
	BounceBackLoan_Capital from Aug 2021: -£833 per month
	SISV Fee: -£313 per month
	SELoan_Interest from Nov 2021: -8% * £235k per year // Pete mentioned £40k a year, but I think that's an overestimate
Financing: {.bg-green}

Income: Sales_Revenue - COS + Financing

Burn Rate: Income - Costs

Balance: previous Balance + Burn Rate
//Balance at Jan 2020: £1,196,202 
Balance: {fontWeight: bold}

Closing CashAtBank at Oct 2020: £1m // TODO get an actual number
Closing CashAtBank: previous(Closing CashAtBank) + Burn Rate + Sales Receipts Delta - DelayedCreditorsDelta // Use deltas to swap which months are counted in the burn rate
Cashflow Details: na
    Sales Receipts Delta: (Sales_Revenue at 2 months ago) - Sales_Revenue // we get paid 2 months after the cost-of-sale
    Delayed Creditors: Sales Commission + Agent Commission + Business Donations
    DelayedCreditorsDelta: (Delayed Creditors at 3 months ago) - Delayed Creditors // we pay donations and commission after we get paid 
    // TODO VAT     
