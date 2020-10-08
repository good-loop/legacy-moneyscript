start: Jan 2020
end: Sep 2022

samples: 20

// actuals
// https://docs.google.com/spreadsheets/d/e/2PACX-1vRvLd73E4kTwaoV3PRzQeDnJT7A1VZGzj6DjQty4sPckoikUEdqsuR0lkRCjVLFSWReywOfX5vtgif5/
import: https://docs.google.com/spreadsheets/d/e/2PACX-1vRvLd73E4kTwaoV3PRzQeDnJT7A1VZGzj6DjQty4sPckoikUEdqsuR0lkRCjVLFSWReywOfX5vtgif5/pub?output=csv

UK Staff:
	Sales Staff:	// salaries are base - commission is done separately
		Amy: £46k per year
		Charley: £29k per year
		Julia: £70k per year
		Stuart: £55k per year				
		Nick from Mar 2020: £68k per year	// NB: Amy's sheet counts Brand-Direct staff separately at 1/2 a sales person for new leads. But Nick's been crushing it, so he's counted here as 1.
		Claire from Mar 2020: £68k per year		
		Junior Sales Publicis from Jan 2021: £35k per year
		Junior Sales Assistant from Nov 2021: £21k per year // Initially hired on govt scheme so costs don't appear until later. Actually, see if we can hire 2, and one does I-in-C and SoGive work
	Account Management Staff:
		Ryan: £60k per year
		Tom: £40k per year		
	Tech Staff:		
		Dan: Amy
		Dan A: £36k per year
		Roscoe: £37k per year
		Eve from May 2020: £25k per year
		Kati from May 2020: £35k per year
		Ben from Jun 2020: £20k per year
		Kai from Oct 2020: £500 per month	// part-time student
UK Staff: {.bg-blue}

// 4 day week
Amy from Apr 2020 to Sep 2020: * 80%
Account Management Staff from Apr 2020 to Sep 2020: * 80%
Roscoe from Apr 2020 to Jun 2020: * 80%
Eve to Dec 2020: * 80%
Kati to Dec 2020: * 80%
Ben to Dec 2020: * 80%

// October pay rises
Amy from Oct 2020: Ryan	
Charley from Oct 2020: + £3k per year
Julia from Oct 2020: + £1.4k per year
Stuart from Oct 2020: + £3k per year
Roscoe from Oct 2020: + £3k per year

// general pay-rises - 5% in June 2021?
UK Staff from Jun 2021: * 105%

US Staff:
	Barbara from Oct 2020: £800 per month
//	US Remote Sales Agent from Jun 2021: £66k per year
//	Head of US Sales from Jun 2021: £120k per year
US Staff: {.bg-blue}

Contractors:
	Amanda: £450 per month		
	Pete: £150 per month
	Designer Emilia: £1.4k per month
	Raimah: £1k per month
	Business Admin from Dec 2020: £1k per month
	Trudi: £3k per month
	Advisors:
		Chairman from Apr 2021: £1k per month
		Donia Baddou: £1040 per month // Jules Mentor
		Alexandra Jorge: £250 per month // Ryan Mentor
		Kate Ho: £70 // Kati mentor
		Tech Advisor from Nov 2020: £400 per month

Staff Count: count(UK Staff) + count(US Staff)
HeadCount: Staff Count + count(Contractors)

Costs:
	HR Costs:
		Pay: UK Staff + US Staff + Contractors
		Employers NI: 12% * count(UK Staff)
		Pension: 3% * UK Staff
		Wellness budget: £100 * count(UK Staff) * 75% 				// let's assume a decent but 100% take-up
		Training: £20 * HeadCount + £100 * count(US Staff) 		// plan for US staff doing a UK visit
		Recruitment: £500 per month

	Rent:
		Edinburgh: £450 per month
		London Club from Jan 2021: £1k per month

	Marketing: 
		Misc Marketing: £50 per month // design/editing/IAB/Awards
		Owned Events: £1k per month
		Industry Events: £1k per month
		Content Creation from Jan 2021: £1k per month

	Travel_Entertainment:
		Travel: £100
		Accommodation: £100
		Subsistence: £100
		Staff Entertainment: £30 * Staff Count		
		Client Entertainment: £200 per month
	Travel_Entertainment from Feb 2021: * 200% // lockdown lifts?

	External Professional Fees: // Not including e.g. Pete, Amanda, Donia
		UK Accountancy: £575			
		US Accountancy from 2021: £750 per month
		UK Legal: £2k per month
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
	RampedUp Staff: count(Sales Staff from 5 months ago)
	Campaigns PerSalesperson: 1 per month // It was about 0.7 in 2019 and 0.9 in 2020 (source: email from Jules re SF data, noting that head-count his blurry). Allowing for covid, it could be higher. This is new and repeat.
	UK_Campaigns: Campaigns PerSalesperson * RampedUp Staff
	Campaign_Size: N(23k, 451m) // Checking the data: new and existing clients have the same profile
	// Adjustments
	Seasonality: 70%,	75%,	90%,	115%,	125%,	115%,	90%,	80%,	100%,	100%,	115%,	125%,	70%,	75%,	90%,	115%,	125%,	115%,	90%,	80%,	100%,	100%,	115%,	125%
	// seasonality + covid 70%,	75%,	18%,	23%,	25%,	58%,	45%,	45%,	70%,	90%,	80%,	90%,	70%,	75%,	18%,	23%,	25%,	58%,	45%,	45%,	50%,	80%,	80%,	90%,	70%,	75%,	90%,	115%,	125%,	115%,	90%,	80%,	100%,	100%,	115%,	125%, // inc Corona effect, copy-pasta from Amy's sheet
	Marketing ROI: 20% * (Marketing from 3 months ago + Trudi from 3 months ago) // this is over-egging as we haven't subtracted this from the historical average.
Revenue Details: {.bg-green}

COS:
	Advertising Inventory: 25% * Sales_Revenue
	Business Donations: 50% * Sales_Revenue
	Brand_Uplift_Research: £4k * UK_Campaigns * 20% // Is it really only 20% who have a brand uplift study??
	Sales Commission: 4.75% * Sales_Revenue // Calculated from commission calc sheet
	Profit Share: 3% * (Sales_Revenue - Advertising Inventory - Business Donations - Brand_Uplift_Research) // ?? is this the right definition?	
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

// Cashflow (TODO - currently the Balance actuals fold in aged debtors/creditors, so that's fairly close to Cashflow)
//Creditors: sum(Sales Commission from 4 months ago to now) + sum(Business Donations from 4 months ago to now)
//Debtors: sum(Sales_Revenue from 3 months ago to now)
//CashAtBank: Balance + Creditors - Debtors
