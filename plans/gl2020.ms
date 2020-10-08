start: Jan 2020
end: Jan 2021

Costs:
	Rent:
		Edinburgh: £900 per month
		London: £3k per month
	Staff:
		Commercial Staff:
			Amy: £46k per year
			Ryan: £60k per year
			Charley: £29k per year
			Julia: £70k per year
			Stuart: £55k per year
			Tom: £38k per year
			Martina: £28k per year
			Brands Ambassador from month 2: £45k +- £5k per year
			Sales Person from month 6: £50k +- £10k per year
		Tech Staff:
			Dan: £46k per year
			Dan A: £36k per year
			Roscoe: £36k per year
			Andris: £22k per year
			Eve: £1k per month
			Rashad: 20% * £22k per year
			Junior Dev from month 4: £25k per year
			Data Scientist from month 2: £35k per year
		Other Staff:
			Devina: £150 per month
			Chairman: £1k per month
			Raimah: £1k per month
			Advisors: £2.5k per month
	Recruitment: £500 per month
	Contractors:
		Amanda: £1k per month		
		Claudia: £4.5k per month		
	Marketing:
		Emilia: £1.4k per month
		Trudi: £3k per month
		Owned events: £14k per year
		Industry events: £25k per year
		White papers at June 2020: £3k
	US Staff:
		Jamie from month 5: £2k per month
	US Legals from month 9: £2k per month
	Professional Fees: £30k per year
	Bank Fees: £6,400 per year
	Insurance: £2k per year
	Subscriptions: £2.5k per year // includes what?? probably came from xero
	Phone Net: £2.5k per year

	UK Salary Overheads: 20% * Staff	// tax, pension, etc
	US Salary Overheads: 25% * US Staff	// tax 7%, healthcare, pension, etc 
	
	Office Supplies: £2.5k per year
	IT: £2k per month // servers + office software subs??
	Travel: £2k per month
	Training: £200 per year * count(Staff) + £1k per year * count(US Staff)

Staff Count: count(Staff)

// grow with low churn
New Sales: 4 +- 2 per month
Old Customers: 30
Repeat Sales: 60% * (Old Customers / 12)

Sales Value: £30k +- £30k      // 2019 was £30k average -- Can we double it?
Num Sales: New Sales + Repeat Sales
Raw Revenue: Sales Value * Num Sales
Margin: 31% * 78%     // raw margin, less sales staff commission & general bonus

Income:
	Sales Revenue: Margin * Raw Revenue
	Grants: £10k +- £10k per month
	RnD: 14.5% * Tech Staff		

Cashflow: Income - Costs

Balance: previous Balance + Cashflow
Balance at month 1: £1200k
